package tygronenv.connection;

import javax.security.auth.login.LoginException;

import eis.exceptions.ManagementException;
import login.Login;
import login.ProjectException;
import login.ProjectFactory;
import nl.tytech.core.client.net.ServicesManager;
import nl.tytech.core.net.serializable.ProjectData;
import nl.tytech.core.net.serializable.User;
import nl.tytech.core.net.serializable.User.AccessLevel;
import tygronenv.configuration.Configuration;

/**
 * The top level connection with the server. Creates connection, checks
 * credentials, creates new project (if requested name is not yet available).
 * 
 * @author W.Pasman
 *
 */
public class ServerConnection {

	/**
	 * True if project is created by us. False if someone else created the
	 * project.
	 */
	private boolean createdProject = false;
	private ProjectData project;

	/**
	 * Session can be used by all team members.
	 */
	private Session session = null;
	private ProjectFactory factory = createFactory();

	/**
	 * Connect with the server, and create Session with the
	 * {@link Configuration}.
	 * 
	 * @param config
	 *            the {@link Configuration} to use
	 * @throws ManagementException
	 */
	public ServerConnection(Configuration config) throws ManagementException {
		User user;
		Login login;
		try {
			login = new Login();
			user = login.doLogin();
		} catch (LoginException e) {
			throw new ManagementException("login failed", e);
		}

		if (user.getMaxAccessLevel().ordinal() < AccessLevel.EDITOR.ordinal()) {
			throw new ManagementException("You need to have at least EDITOR access level");
		}

		project = factory.getProject(config.getProject());
		if (project == null) {
			try {
				project = factory.createProject(config.getProject());
			} catch (ProjectException e) {
				throw new ManagementException("failed to create project", e);
			}
			createdProject = true;
		}

		session = new Session(config, project);

	}

	/**
	 * Factory method for projectfactory.
	 * 
	 * @return {@link ProjectFactory}. Is created only 1 time and instantiation
	 *         is kept.
	 */
	public ProjectFactory createFactory() {
		return new ProjectFactory();
	}

	/**
	 * Disconnect from server. After calling this, this object can not be used
	 * anymore.
	 *
	 * @throws ManagementException
	 */
	public void disconnect() throws ManagementException {
		if (session != null) {
			session.close();
			session = null;
		}
		if (createdProject) {
			try {
				factory.deleteProject(project);
			} catch (ProjectException e) {
				throw new ManagementException("Failed to remove temp project", e);
			} finally {
				project = null;
				createdProject = false;
			}
		}

		ServicesManager.removeLoginCredentials();
	}

	/**
	 * @return the session that we are working on. All team members can use this
	 *         session
	 */
	public Session getSession() {
		return session;
	}
}
