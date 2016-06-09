package contextvh.actions;

import java.util.HashMap;

import eis.iilang.Action;

/**
 * A container for CustomActions. All custom actions should be added in the constructor
 * of this object.
 * @author Max Groenenboom
 */
public class ActionContainer extends HashMap<String, CustomAction> {
	private static final long serialVersionUID = -6578689136054121705L;

	/**
	 * Creates an ActionContainer object filled with all existing CustomActions defined.
	 * Thus use the ActionContainer.put function to add custom actions to the list.
	 * They will automatically be used in TygronEntity.
	 */
	public ActionContainer() {
		super();

		// Add actions here:
		addAction(new GetRelevantAreas());
	}

	/**
	 * Add an action with as a key its name to the container.
	 * @param action The action to add to the container.
	 */
	public void addAction(final CustomAction action) {
		this.put(action.getName(), action);
	}

	/**
	 * Returns the CustomAction the given Action refers to.
	 * @param action The called action.
	 * @return The CustomAction the action refers to.
	 */
	public CustomAction get(final Action action) {
		return get(action.getName().toLowerCase());
	}
}
