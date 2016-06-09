package nl.tytech.sdk.e2eTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import nl.tytech.data.editor.event.EditorEventType;
import nl.tytech.data.editor.event.EditorSettingsEventType;
import nl.tytech.data.editor.event.EditorStakeholderEventType;
import nl.tytech.data.engine.event.ParticipantEventType;
import nl.tytech.data.engine.item.Function;
import nl.tytech.data.engine.item.Stakeholder;
import nl.tytech.data.engine.serializable.Category;
import nl.tytech.locale.TLanguage;
import nl.tytech.util.JTSUtils;
import nl.tytech.util.ThreadUtils;
import nl.tytech.util.logger.TLogger;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExampleTest {

	private static Integer slotID;

	private static JoinReply reply;

	private static ProjectData data;

	private static TSlotConnection slotConnection;

	private static ExampleEventHandler eventHandler;

	private static Integer stakeholderID = 0;

	private static Login login;

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
	}

	@Test
	public void test08selectStakeholderToPlay() throws Exception {

		stakeholderID = 0;
		ItemMap<Stakeholder> stakeholders = EventManager.getItemMap(slotConnection.getConnectionID(),
				MapLink.STAKEHOLDERS);
		for (Stakeholder stakeholder : stakeholders) {
			stakeholderID = stakeholder.getID();
			TLogger.info("Selecting first stakeholder: " + stakeholder.getName() + " to play!");
			break;
		}
		slotConnection.fireServerEvent(true, ParticipantEventType.STAKEHOLDER_SELECT, stakeholderID,
				reply.client.getClientToken());
	}

	@Test
	public void test09planBuilding() throws Exception {

		/**
		 * Plan an new ROAD construction
		 */
		Integer functionID = 0;
		int floors = 1;
		ItemMap<Function> functions = EventManager.getItemMap(slotConnection.getConnectionID(), MapLink.FUNCTIONS);
		for (Function function : functions) {
			if (function.getCategories().contains(Category.ROAD)) {
				functionID = function.getID();
				TLogger.info("Selecting first road function: " + function.getName() + " to build!");
				break;
			}
		}

		/**
		 * Shape of my new road
		 */
		MultiPolygon roadMultiPolygon = JTSUtils.createSquare(10, 10, 200, 10);

		Integer newBuildingID = slotConnection.fireServerEvent(true, ParticipantEventType.BUILDING_PLAN_CONSTRUCTION,
				stakeholderID, functionID, floors, roadMultiPolygon);

		assertTrue(newBuildingID.intValue() >= 0);

	}

	@Test
	public void test10closeRegularSession() throws Exception {
		slotConnection.disconnect(false);
	}

	@Test
	public void test11deleteProject() throws Exception {
		assertTrue(ServicesManager.fireServiceEvent(IOServiceEventType.DELETE_PROJECT, data.getFileName()));
	}
}
