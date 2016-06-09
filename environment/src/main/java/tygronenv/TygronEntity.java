package tygronenv;

import java.util.LinkedList;

import eis.eis2java.exception.TranslationException;
import eis.iilang.Action;
import eis.iilang.Percept;
import nl.tytech.data.engine.event.ParticipantEventType;
import nl.tytech.data.engine.item.Stakeholder;

/**
 * The 'participant' - a single stakeholder connection. Handles events coming in
 * for this stakeholder.
 * 
 * A TygronEntity becomes an EIS entity only when the initial percepts have come
 * in, and there is a stakeholder with the {@link #intendedStakeholderName}.
 * 
 * @author W.Pasman
 *
 */
public interface TygronEntity {

	/**
	 * Perform given action
	 *
	 * @param action
	 *            action of the form action(p1,...). Action must be a
	 *            {@link ParticipantEventType}. The arguments are those as
	 *            specified in {@link ParticipantEventType}, except that the
	 *            obligatory first argument "Stakeholder ID" is automatically
	 *            filled in so you can leave that out and start with parameter
	 *            2.
	 * @throws TranslationException
	 *             if a parameter can not be translated.
	 * @return {@link Percept}, or null if no percept is available.
	 */
	Percept performAction(Action action) throws TranslationException;

	/**
	 * @return the current percepts of this entity
	 */
	LinkedList<Percept> getPercepts();

	/**
	 * 
	 * @param action
	 * @return true iff the action is supported by this entity
	 */
	boolean isSupported(Action action);

	/**
	 * Close server connection.
	 */
	void close();

	/**
	 * 
	 * @return the Stakeholder, or null if we are not yet connected with a
	 *         stakeholder.
	 */
	Stakeholder getStakeholder();

}
