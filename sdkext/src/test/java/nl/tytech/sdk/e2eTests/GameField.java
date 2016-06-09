package nl.tytech.sdk.e2eTests;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

import login.Login;
import login.ProjectException;
import login.ProjectFactory;
import nl.tytech.core.client.net.ServicesManager;
import nl.tytech.core.client.net.TSlotConnection;
import nl.tytech.core.net.Network.AppType;
import nl.tytech.core.net.Network.SessionType;
import nl.tytech.core.net.event.IOServiceEventType;
import nl.tytech.core.net.serializable.JoinReply;
import nl.tytech.core.net.serializable.ProjectData;
import nl.tytech.core.util.SettingsManager;
import nl.tytech.data.core.item.Item;
import nl.tytech.data.engine.event.LogicEventType;
import nl.tytech.data.engine.item.Stakeholder;
import nl.tytech.locale.TLanguage;
import nl.tytech.util.logger.TLogger;

/**
 * Game field where general testing of interactions can be done for testing.
 *
 * @author W.Pasman
 *
 */
public class GameField {

	private ProjectData project;
	private Map<Stakeholder.Type, MyStakeholder> stakeholders = new HashMap<Stakeholder.Type, MyStakeholder>();
	private Integer slotID = Item.NONE;

	private TSlotConnection facilitatorConnection;

	public GameField() throws LoginException, ProjectException {
		try {
			Login login = new Login();
			login.doLogin();

			ProjectFactory factory = new ProjectFactory();
			String projectName = "test" + System.currentTimeMillis();
			project = factory.createProject(projectName);
			if (project == null) {
				throw new IllegalStateException("createProject returned null");
			}
			slotID = ServicesManager.fireServiceEvent(IOServiceEventType.START_NEW_SESSION, SessionType.MULTI,
					project.getFileName(), TLanguage.EN);
			if (Item.NONE.equals(slotID) || slotID == null) {
				throw new IllegalStateException(
						"failed to start multiplayer session for project: " + project.getFileName());
			}
			JoinReply reply = ServicesManager.fireServiceEvent(IOServiceEventType.JOIN_SESSION, slotID,
					AppType.FACILITATOR);

			facilitatorConnection = TSlotConnection.createSlotConnection();
			facilitatorConnection.initSettings(AppType.FACILITATOR, SettingsManager.getServerIP(), slotID,
					reply.serverToken, reply.client.getClientToken());
			boolean connected = facilitatorConnection.connect();
			if (!connected) {
				throw new IllegalStateException(
						"Failed to connect to session: " + project.getFileName() + " with token: " + reply.serverToken);
			}
			facilitatorConnection.fireServerEvent(true, LogicEventType.SETTINGS_ALLOW_INTERACTION, true);
		} catch (IllegalStateException e) {
			close();
			throw e;
		} catch (Exception ee) {
			TLogger.exception(ee);
			throw (ee);
		}

	}

	public TSlotConnection getSlotConnection() {
		return facilitatorConnection;
	}

	/**
	 * Call this once for each expected stakeholder. You may then use the
	 * returned stakeholder immediately
	 *
	 */
	public MyStakeholder addStakeholder(Stakeholder.Type type) {
		MyStakeholder stakeholder = new MyStakeholder(type, slotID);
		stakeholders.put(type, stakeholder);
		return stakeholder;
	}

	public void close() throws ProjectException {
		if (facilitatorConnection != null) {
			facilitatorConnection.disconnect(false);
			facilitatorConnection = null;
			slotID = Item.NONE;
		}

		if (project != null) {
			new ProjectFactory().deleteProject(project);
			project = null;
		}
	}

}
