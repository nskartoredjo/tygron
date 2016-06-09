package contextvh.actions;

import java.util.LinkedList;

import contextvh.ContextEntity;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Parameter;
import eis.iilang.Percept;

/**
 * Interface for custom actions.
 * @author Max Groenenboom
 */
public interface CustomAction {
	/**
	 * This method is called when the action was called by an agent.
	 * @param caller The ContextEntity that called the action.
	 * @param parameters The parameters provided by the agent.
	 * @return List of percepts resulting from the action. This list can be empty or null.
	 * @throws TranslationException When an action call was invalid.
	 */
	Percept call(ContextEntity caller, LinkedList<Parameter> parameters) throws TranslationException;

	/**
	 * This method provides the name of the action, which the agent
	 * will have to call.
	 * @return The name of the action.
	 */
	String getName();
}
