package login;

/**
 * Thrown when something fails when creating or deleting projects.
 *
 * @author W.Pasman
 *
 */
@SuppressWarnings("serial")
public class ProjectException extends Exception {

	public ProjectException(String message) {
		super(message);
	}

}
