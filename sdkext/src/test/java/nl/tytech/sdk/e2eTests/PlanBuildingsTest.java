package nl.tytech.sdk.e2eTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

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
import nl.tytech.data.engine.item.Indicator;
import nl.tytech.data.engine.item.PersonalIndicator.PersonalIndicatorType;
import nl.tytech.data.engine.item.Stakeholder;
import nl.tytech.data.engine.serializable.Category;
import nl.tytech.data.engine.serializable.MapType;
import nl.tytech.locale.TLanguage;
import nl.tytech.util.JTSUtils;
import nl.tytech.util.ThreadUtils;
import nl.tytech.util.logger.TLogger;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PlanBuildingsTest {

	private static Integer slotID;

	private static JoinReply reply;

	private static ProjectData data;

	private static TSlotConnection slotConnection;

	private static ExampleEventHandler eventHandler;

	private static Login login;

	private static Integer stakeholderID = Item.NONE;

	private static Integer buildActionLogID = Item.NONE;
	private static Integer revertActionLogID = Item.NONE;

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
		actionMenuLoop: for (ActionMenu actionMenu : EventManager
				.<ActionMenu> getItemMap(slotConnection.getConnectionID(), MapLink.ACTION_MENUS)) {
			if (actionMenu.isBuildable(stakeholderID))
				for (Function buildableFunction : actionMenu.getFunctionTypeOptions()) {
					if (buildableFunction.getPlacementType() != PlacementType.WATER) {
						function = buildableFunction;
					}
					break actionMenuLoop;
				}
		}

		assertTrue("No applicable land function found!", function != null);

		List<Polygon> buildablePolygons = SDKTestUtil.getBuildableLand(slotConnection.getConnectionID(),
				MapType.MAQUETTE, stakeholderID, zoneID, function.getPlacementType());

		assertTrue("No buildable polygons found!", buildablePolygons.size() > 0);

		eventHandler.resetUpdate(MapLink.ACTION_LOGS);

		MultiPolygon selectedPlot = JTSUtils.createMP(buildablePolygons.get(0));
		TLogger.info("Size selected plot: " + selectedPlot.getArea());
		int floors = function.getDefaultFloors();
		buildActionLogID = slotConnection.fireServerEvent(true, ParticipantEventType.BUILDING_PLAN_CONSTRUCTION,
				stakeholderID, function.getID(), floors, selectedPlot);

		assertTrue("Action was not succesfull!", !Item.NONE.equals(buildActionLogID));

	}

	@Test
	public void test10GetActionLogAndIndicatorChange() throws Exception {
		boolean updated = false;
		for (int i = 0; i < 60; i++) {
			if (eventHandler.isUpdated(MapLink.ACTION_LOGS)) {
				updated = true;
				break;
			}
			ThreadUtils.sleepInterruptible(1000);
		}
		assertTrue(updated);

		ActionLog actionLog = EventManager.getItem(slotConnection.getConnectionID(), MapLink.ACTION_LOGS,
				buildActionLogID);
		assertTrue("Actionlog with ID " + buildActionLogID + " does not exist!", actionLog != null);

		for (Indicator indicator : EventManager.<Indicator> getItemMap(slotConnection.getConnectionID(),
				MapLink.INDICATORS)) {
			if (actionLog.containsAfterScore(indicator)) {
				Double increase = actionLog.getIncrease(indicator);
				TLogger.info("Indicator: " + indicator + " change: " + increase);
			}
		}

	}

	@Test
	public void test11RevertConstruction() throws Exception {

		ActionLog actionLog = EventManager.getItem(slotConnection.getConnectionID(), MapLink.ACTION_LOGS,
				buildActionLogID);
		assertTrue("Maplink of ActionLog " + actionLog + " was not BUILDINGS", !actionLog.getBuildingIDs().isEmpty());

		List<Building> buildings = EventManager
				.<Building> getItemMap(slotConnection.getConnectionID(), MapLink.BUILDINGS)
				.getItems(actionLog.getBuildingIDs());
		assertTrue("Building of ActionLog " + actionLog + " does not exist!", !buildings.isEmpty());

		eventHandler.resetUpdate(MapLink.ACTION_LOGS, MapLink.BUILDINGS);

		revertActionLogID = slotConnection.fireServerEvent(true, ParticipantEventType.BUILDING_REVERT_POLYGON,
				stakeholderID, actionLog.getMultiPolygon());
	}

	@Test
	public void test12RevertConstruction() throws Exception {
		boolean updated = false;
		for (int i = 0; i < 60; i++) {
			if (eventHandler.isUpdated(MapLink.ACTION_LOGS, MapLink.BUILDINGS)) {
				updated = true;
				break;
			}
			ThreadUtils.sleepInterruptible(1000);
		}
		assertTrue(updated);

		ActionLog buildActionLog = EventManager.getItem(slotConnection.getConnectionID(), MapLink.ACTION_LOGS,
				buildActionLogID);
		List<Building> buildings = EventManager
				.<Building> getItemMap(slotConnection.getConnectionID(), MapLink.BUILDINGS)
				.getItems(buildActionLog.getBuildingIDs());

		for (Building building : buildings) {
			assertTrue("Building " + building + " is not reverted!", !building.isInMap(MapType.MAQUETTE));
		}

		ActionLog revertActionLog = EventManager.getItem(slotConnection.getConnectionID(), MapLink.ACTION_LOGS,
				revertActionLogID);
		assertTrue("Revert ActionLog does not exist", revertActionLog != null);

		for (Indicator indicator : EventManager.<Indicator> getItemMap(slotConnection.getConnectionID(),
				MapLink.INDICATORS)) {
			if (revertActionLog.containsAfterScore(indicator)) {
				Double increase = revertActionLog.getIncrease(indicator);
				TLogger.info("Indicator: " + indicator + " change: " + increase);
			}
		}

	}

	@Test
	public void test13closeRegularSession() throws Exception {
		slotConnection.disconnect(false);
	}

	@Test
	public void test14deleteProject() throws Exception {
		assertTrue(ServicesManager.fireServiceEvent(IOServiceEventType.DELETE_PROJECT, data.getFileName()));
	}
}
