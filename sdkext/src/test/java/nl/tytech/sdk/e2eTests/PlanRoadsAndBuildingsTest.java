package nl.tytech.sdk.e2eTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.vividsolutions.jts.geom.MultiPolygon;

import login.Login;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.client.net.ServicesManager;
import nl.tytech.core.client.net.TSlotConnection;
import nl.tytech.core.net.Network.AppType;
import nl.tytech.core.net.Network.SessionType;
import nl.tytech.core.net.event.IOServiceEventType;
import nl.tytech.core.net.serializable.JoinReply;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.net.serializable.ProjectData;
import nl.tytech.core.net.serializable.User;
import nl.tytech.core.net.serializable.User.AccessLevel;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.core.util.SettingsManager;
import nl.tytech.data.core.item.Item;
import nl.tytech.data.editor.event.EditorEventType;
import nl.tytech.data.editor.event.EditorIndicatorEventType;
import nl.tytech.data.editor.event.EditorSettingsEventType;
import nl.tytech.data.editor.event.EditorStakeholderEventType;
import nl.tytech.data.engine.event.ParticipantEventType;
import nl.tytech.data.engine.item.ActionLog;
import nl.tytech.data.engine.item.ActionMenu;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.item.Function;
import nl.tytech.data.engine.item.Function.PlacementType;
import nl.tytech.data.engine.item.PersonalIndicator.PersonalIndicatorType;
import nl.tytech.data.engine.item.Stakeholder;
import nl.tytech.data.engine.serializable.Category;
import nl.tytech.data.engine.serializable.MapType;
import nl.tytech.locale.TLanguage;
import nl.tytech.util.ThreadUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PlanRoadsAndBuildingsTest {

	private static Integer slotID;

	private static JoinReply reply;

	private static ProjectData data;

	private static TSlotConnection slotConnection;

	private static ExampleEventHandler eventHandler;

	private static Login login;

	private static Integer stakeholderID = Item.NONE;

	@Test
	public void test01Setup() throws Exception {
		login = new Login();
		login.doLogin();
	}

	@Test
	public void test02Connect() throws Exception {

		String result = ServicesManager.testServerAPIConnection();
		assertNull(result, result);

		User user = ServicesManager.getMyUserAccount();

		assertNotNull(user);
		assertEquals(user.getUserName(), login.getUserName());

		assertTrue("You need to be at least EDITOR to run these tests!",
				user.getMaxAccessLevel().ordinal() >= AccessLevel.EDITOR.ordinal());
	}

	@Test
	public void test03CreateNewProject() throws Exception {

		String projectName = "test" + System.currentTimeMillis();
		data = ServicesManager.fireServiceEvent(IOServiceEventType.CREATE_NEW_PROJECT, projectName, TLanguage.EN);
		assertNotNull(data);
	}

	@Test
	public void test04StartEditSessionAsEditor() throws Exception {

		slotID = ServicesManager.fireServiceEvent(IOServiceEventType.START_NEW_SESSION, SessionType.EDITOR,
				data.getFileName(), TLanguage.EN);
		assertTrue(slotID != null && slotID >= 0);

		reply = ServicesManager.fireServiceEvent(IOServiceEventType.JOIN_SESSION, slotID, AppType.EDITOR);
		assertNotNull(reply);

		slotConnection = TSlotConnection.createSlotConnection();
		slotConnection.initSettings(AppType.EDITOR, SettingsManager.getServerIP(), slotID, reply.serverToken,
				reply.client.getClientToken());

		assertTrue(slotConnection.connect());

		// add event handler to receive updates on
		eventHandler = new ExampleEventHandler(slotConnection);
	}

	@Test
	public void test05doEditSession() throws Exception {

		int mapSizeM = 500;
		slotConnection.fireServerEvent(true, EditorEventType.SET_INITIAL_MAP_SIZE, mapSizeM);
		slotConnection.fireServerEvent(true, EditorSettingsEventType.WIZARD_FINISHED);

		/**
		 * Add a civilian stakeholder
		 */
		slotConnection.fireServerEvent(true, EditorStakeholderEventType.ADD_WITH_TYPE_AND_PLAYABLE,
				Stakeholder.Type.CIVILIAN, true);

		// wait on first updates (seperate thread)
		boolean updated = false;
		for (int i = 0; i < 60; i++) {
			if (eventHandler.isMapUpdated() && eventHandler.isUpdated(MapLink.STAKEHOLDERS)) {
				updated = true;
				break;
			}
			ThreadUtils.sleepInterruptible(1000);
		}
		assertTrue(updated);

		ItemMap<Stakeholder> stakeholders = EventManager.getItemMap(slotConnection.getConnectionID(),
				MapLink.STAKEHOLDERS);
		for (Stakeholder stakeholder : stakeholders) {
			stakeholderID = stakeholder.getID();
			break;
		}

		// (Frank) Add indicators with targets
		Integer levelID = 0;
		double budget = 20000000d;
		Integer financeIndicatorID = slotConnection.fireServerEvent(true,
				EditorIndicatorEventType.ADD_PERSONAL_INDICATOR, stakeholderID, PersonalIndicatorType.FINANCE);
		slotConnection.fireServerEvent(true, EditorIndicatorEventType.SET_TARGET, levelID, financeIndicatorID, 0,
				budget);

		Integer housingIndicatorID = slotConnection.fireServerEvent(true,
				EditorIndicatorEventType.ADD_PERSONAL_INDICATOR, stakeholderID, PersonalIndicatorType.HOUSING);

		// (Frank) Targets for housing indicators are defined per category.
		double[] targets = new double[Category.values().length];
		for (int i = 0; i < targets.length; i++) {
			targets[i] = 250000;
		}

		slotConnection.fireServerEvent(true, EditorIndicatorEventType.SET_TARGETS, levelID, housingIndicatorID,
				targets);

		slotConnection.fireServerEvent(true, EditorStakeholderEventType.SET_START_BUDGET, stakeholderID, budget);

	}

	@Test
	public void test06closeEditSession() throws Exception {

		/**
		 * Save project in our slotID
		 */
		String result = ServicesManager.fireServiceEvent(IOServiceEventType.SAVE_PROJECT_INIT, slotID);
		assertNull(result, result);

		/**
		 * Disconnect from slot
		 */
		slotConnection.disconnect(false);
	}

	@Test
	public void test07startRegularSessionAsParticipant() throws Exception {
		// TODO: (Frank) Change to multi if you want to join
		slotID = ServicesManager.fireServiceEvent(IOServiceEventType.START_NEW_SESSION, SessionType.SINGLE,
				data.getFileName(), TLanguage.EN);
		assertTrue(slotID != null && slotID >= 0);

		reply = ServicesManager.fireServiceEvent(IOServiceEventType.JOIN_SESSION, slotID, AppType.PARTICIPANT);
		assertNotNull(reply);

		slotConnection = TSlotConnection.createSlotConnection();
		slotConnection.initSettings(AppType.PARTICIPANT, SettingsManager.getServerIP(), slotID, reply.serverToken,
				reply.client.getClientToken());

		assertTrue(slotConnection.connect());

		// add event handler to receive updates on
		eventHandler = new ExampleEventHandler(slotConnection);

		boolean updated = false;
		for (int i = 0; i < 60; i++) {
			if (eventHandler.isMapUpdated() && eventHandler.isUpdated(MapLink.STAKEHOLDERS, MapLink.LANDS)) {
				updated = true;
				break;
			}
			ThreadUtils.sleepInterruptible(1000);
		}
		assertTrue(updated);
	}

	@Test
	public void test08selectStakeholderToPlay() throws Exception {

		// TODO: (Frank) Enable this to be able to log into the session.
		// Prerequisite is that the session is started as SessionType.MULTI
		// slotConnection.fireServerEvent(false,
		// LogicEventType.SETTINGS_ALLOW_INTERACTION, true);

		ItemMap<Stakeholder> stakeholders = EventManager.getItemMap(slotConnection.getConnectionID(),
				MapLink.STAKEHOLDERS);
		for (Stakeholder stakeholder : stakeholders) {
			stakeholderID = stakeholder.getID();
			break;
		}
		assertTrue("Expected at least 2 stakeholders", !Item.NONE.equals(stakeholderID));
	}

	@Test
	public void test09GetConstructableLand() throws Exception {

		Integer zoneID = 0;

		Function function = null;
		Function roadFunction = null;
		actionMenuLoop: for (ActionMenu actionMenu : EventManager
				.<ActionMenu> getItemMap(slotConnection.getConnectionID(), MapLink.ACTION_MENUS)) {
			if (actionMenu.isBuildable(stakeholderID))
				for (Function buildableFunction : actionMenu.getFunctionTypeOptions()) {
					if (function == null && buildableFunction.getPlacementType() != PlacementType.WATER) {
						function = buildableFunction;
					}

					if (roadFunction == null && buildableFunction.getCategories().contains(Category.ROAD)) {
						roadFunction = buildableFunction;
					}

					if (function != null && roadFunction != null) {
						break actionMenuLoop;
					}
				}
		}

		assertTrue("No applicable land function found!", function != null);
		assertTrue("No applicable land function found!", roadFunction != null);
		Random random = new Random();
		// TODO: (Frank) Change max from 4 to something larger if you want to
		// see more buildings
		for (int i = 0; i < 4; ++i) {

			Function f = function;
			double width = 20;
			double depth = 10;
			double distanceToRoad = 10;
			if (i % 2 == 0) {
				f = roadFunction;
				width = 10;
				depth = 50;
				distanceToRoad = -1;
			}

			List<MultiPolygon> buildablePolygons = SDKTestUtil.createBlueprintMPs(slotConnection.getConnectionID(),
					MapType.MAQUETTE, stakeholderID, zoneID, f.getPlacementType(), width, depth, distanceToRoad);

			if (buildablePolygons.size() <= 0) {
				continue;
			}

			while (buildablePolygons.size() > 0) {
				eventHandler.resetUpdate(MapLink.BUILDINGS, MapLink.ACTION_LOGS);

				int index = random.nextInt(buildablePolygons.size());
				MultiPolygon selectedPlot = buildablePolygons.remove(index);
				int floors = f.getDefaultFloors();
				Integer buildActionLogID = slotConnection.fireServerEvent(true,
						ParticipantEventType.BUILDING_PLAN_CONSTRUCTION, stakeholderID, f.getID(), floors,
						selectedPlot);

				if (!Item.NONE.equals(buildActionLogID)) {
					boolean updated = false;
					for (int u = 0; u < 60; u++) {
						if (eventHandler.isMapUpdated()
								&& eventHandler.isUpdated(MapLink.BUILDINGS, MapLink.ACTION_LOGS)) {
							updated = true;
							break;
						}
						ThreadUtils.sleepInterruptible(1000);
					}
					ActionLog actionLog = EventManager.getItem(slotConnection.getConnectionID(), MapLink.ACTION_LOGS,
							buildActionLogID);
					if (actionLog != null) {
						ItemMap<Building> buildings = EventManager.getItemMap(slotConnection.getConnectionID(),
								MapLink.BUILDINGS);
						double areaSize = 0;
						for (Building building : buildings.getItems(actionLog.getBuildingIDs())) {
							areaSize += building.getMultiPolygon(MapType.MAQUETTE).getArea();
						}
						if (Math.abs(areaSize - selectedPlot.getArea()) > 1) {
							slotConnection.fireServerEvent(true, ParticipantEventType.BUILDING_REVERT_POLYGON,
									stakeholderID, selectedPlot);
						} else {
							break;
						}
					}

					assertTrue("Excepted Buildings-Update not received", updated);
				}
			}

		}

	}

	@Test
	public void test13closeRegularSession() throws Exception {
		ThreadUtils.sleepInterruptible(1000);
		slotConnection.disconnect(false);
	}

	@Test
	public void test14deleteProject() throws Exception {
		Boolean result = ServicesManager.fireServiceEvent(IOServiceEventType.DELETE_PROJECT, data.getFileName());

		assertTrue(Boolean.TRUE.equals(result));
	}
}
