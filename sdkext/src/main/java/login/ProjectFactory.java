package login;

import nl.tytech.core.client.net.ServicesManager;
import nl.tytech.core.client.net.SlotConnection;
import nl.tytech.core.client.net.TSlotConnection;
import nl.tytech.core.net.Network.AppType;
import nl.tytech.core.net.Network.SessionType;
import nl.tytech.core.net.event.IOServiceEventType;
import nl.tytech.core.net.serializable.JoinReply;
import nl.tytech.core.net.serializable.ProjectData;
import nl.tytech.core.util.SettingsManager;
import nl.tytech.data.editor.event.EditorEventType;
import nl.tytech.data.editor.event.EditorSettingsEventType;
import nl.tytech.data.editor.event.EditorStakeholderEventType;
import nl.tytech.data.engine.item.Stakeholder;
import nl.tytech.locale.TLanguage;
import nl.tytech.util.ThreadUtils;
import nl.tytech.util.logger.TLogger;

/**
 * Factory to fetch existing and create new projects
 *
 * @author W.Pasman
 *
 */
public class ProjectFactory {

	private final static String DOMAIN = "tudelft";

	/**
	 * Join existing project.
	 *
	 * @param name
	 *            the project name to join/make.
	 * @return project with given name, or null if no project with given name
	 *         exists.
	 * @throws ManagementException
	 */
	public ProjectData getProject(String name) {

		ProjectData[] projects = ServicesManager.fireServiceEvent(IOServiceEventType.GET_DOMAIN_STARTABLE_PROJECTS,
				DOMAIN);
		if (projects != null) {
			for (ProjectData existing : projects) {
				if (existing.getFileName().equals(name)) {
					return ServicesManager.fireServiceEvent(IOServiceEventType.GET_PROJECT_DATA, name);
				}
			}
		}

		return null;
	}

	/**
	 * Assumes that there is no existing project with given name (eg
	 * {@link #getProject(String)} returned null). Create one and initialize it.
	 * Bit hacky, it does not do much for the map, it probably stays empty.
	 *
	 * @throws ProjectException
	 *
	 * @throws ManagementException
	 */
	public ProjectData createProject(String name) throws ProjectException {
		ProjectData proj = ServicesManager.fireServiceEvent(IOServiceEventType.CREATE_NEW_PROJECT, name, TLanguage.EN);

		Integer slotID = ServicesManager.fireServiceEvent(IOServiceEventType.START_NEW_SESSION, SessionType.EDITOR,
				proj.getFileName(), TLanguage.EN);
		if (slotID == null || slotID < 0) {
			throw new ProjectException("Failed to create edit slot to create new project");
		}

		TSlotConnection editSlot = editProject(slotID);
		addCivilianMap(editSlot);

		String result = ServicesManager.fireServiceEvent(IOServiceEventType.SAVE_PROJECT_INIT, slotID);
		if (result != null) {
			throw new ProjectException("Failed to save new project " + proj.getFileName() + ": " + result);
		}

		editSlot.disconnect(false);

		if (!projectStartable(proj.getFileName())) {
			TLogger.warning("Failed to correctly close project: " + proj.getFileName());
		}

		return proj;
	}

	private boolean projectStartable(String projectFileName) {
		for (int i = 0; i < 10; i++) {
			ProjectData[] projects = ServicesManager.fireServiceEvent(IOServiceEventType.GET_MY_STARTABLE_PROJECTS);
			for (ProjectData projectData : projects) {
				if (projectFileName.equals(projectData.getFileName())) {
					return true;
				}

			}
			ThreadUtils.sleepInterruptible(1000);
		}
		return false;
	}

	/**
	 * open an editor slot
	 *
	 * @param proj
	 *            the project to open an edit slot for
	 * @return a {@link SlotConnection} that can be used for editing the
	 *         project.
	 * @throws ManagementException
	 */
	private TSlotConnection editProject(Integer slotID) throws ProjectException {

		JoinReply reply = ServicesManager.fireServiceEvent(IOServiceEventType.JOIN_SESSION, slotID, AppType.EDITOR);
		if (reply == null) {
			throw new ProjectException("failed to edit project:" + reply);
		}

		TSlotConnection slotConnection = TSlotConnection.createSlotConnection();
		slotConnection.initSettings(AppType.EDITOR, SettingsManager.getServerIP(), slotID, reply.serverToken,
				reply.client.getClientToken());
		if (!slotConnection.connect()) {
			throw new ProjectException("Failed to connect a slot for editing the new project");
		}

		return slotConnection;
	}

	/**
	 * Add map and civilian stakeholder
	 *
	 * @param slotConnection
	 *            the connection with the editor slot
	 * @throws ManagementException
	 */
	private void addCivilianMap(SlotConnection slotConnection) throws ProjectException {
		int mapSizeM = 500;
		slotConnection.fireServerEvent(true, EditorEventType.SET_INITIAL_MAP_SIZE, mapSizeM);
		slotConnection.fireServerEvent(true, EditorSettingsEventType.WIZARD_FINISHED);

		/**
		 * Add a civilian stakeholder
		 */
		slotConnection.fireServerEvent(true, EditorStakeholderEventType.ADD_WITH_TYPE_AND_PLAYABLE,
				Stakeholder.Type.CIVILIAN, true);
	}

	/**
	 * Deletes project on the server.
	 *
	 * @param project
	 * @throws ManagementException
	 */
	public void deleteProject(ProjectData project) throws ProjectException {
		if (!projectStartable(project.getFileName())) {
			TLogger.warning("Unable to delete project " + project.getFileName() + " on the server");
		}

		Boolean result = ServicesManager.fireServiceEvent(IOServiceEventType.DELETE_PROJECT, project.getFileName());
		if (!result) {
			TLogger.warning("failed to delete project " + project.getFileName() + " on the server");
		}

	}

}
