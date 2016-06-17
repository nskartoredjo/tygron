package contextvh.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import contextvh.ContextEntity;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import nl.tytech.util.logger.TLogger;

/**
 * The filter_percepts(PerceptList) action can be called with as only parameter
 * a list of percepts that won't be send to the agent in the next cycle.
 * The connector will also send a response percept: filtered_percepts([PerceptList])
 * with a list of percepts that are currently being filtered by the connector.
 *
 * The filter_percepts(PerceptList) action requires a complete list of percepts to be filtered,
 * any percepts not included will not be filtered even if they were previously.
 * This allows you to re-enable all percepts by sending a empty list as parameter.
 * @author Dennis van Peer
 */
public class FilterPercepts implements CustomAction {


	private ArrayList<String> disabledPercepts;

	private static final String NAME = "filter_percepts";

	/**
	 * Creates a FilterPercepts actions.
	 */
	public FilterPercepts() {
		disabledPercepts = new ArrayList<String>();
	}

	@Override
	public Percept call(final ContextEntity caller, final LinkedList<Parameter> parameters)
			throws TranslationException {
	try {
		// Get and translate parameters.
		Iterator<Parameter> params = parameters.iterator();
		ParameterList filterParamList = new ParameterList();
		if (params.hasNext()) {
			Parameter filterParam = params.next();
			// If the filter parameter is not a ParameterList, it is invalid.
			if (filterParam instanceof ParameterList) {
				filterParamList = (ParameterList) filterParam;
				processFilter(filterParamList);
			}
		}


		return createPercept(caller, filterParamList);
	} catch (Exception e) {
		TLogger.exception(e);
		throw e;
	}
	}

	/**
	 * Replaces disabledPercepts with an arraylist containing the new percepts.
	 * @param filterParam	A ParameterList of parameters provided by the agent.
	 */
	private void processFilter(final ParameterList filterParam) {
		disabledPercepts = new ArrayList<String>();
		for (int i = 0; i < filterParam.size(); i++) {
			Parameter param = filterParam.get(i);
			if (param instanceof Identifier) {
				Identifier percept = (Identifier) param;
				String perceptname = percept.getValue();
				disabledPercepts.add(perceptname);
			}

		}

	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * This method provides the name of the action, which the agent
	 * will have to call.
	 * @return The name of the action.
	 */
	public static String staticGetName() {
		return NAME;
	}

	/**
	 * Create the response Percept, after the parameters have been parsed.
	 * @param caller		The ContextEntity that called the action.
	 * @param parameters	A ParameterList of parameters provided by the agent.
	 * @return The constructed Percept.
	 * @throws TranslationException  When an invalid internal action parameter is provided.
	 */
	private Percept createPercept(final ContextEntity caller, final ParameterList parameters)
			throws TranslationException {
		Percept result = new Percept("filtered_percepts");

		if (identifierCheck(parameters)) {
			result.addParameter(parameters);
		}

		return result;
	}

	/**
	 * Returns A boolean true iff parameters consists solely of identifiers.
	 * @param parameters	A ParameterList of parameters provided by the agent.
	 * @return A boolean true iff parameters consists solely of identifiers
	 */
	private boolean identifierCheck(final ParameterList parameters) {
		for (int i = 0; i < parameters.size(); i++) {
			Parameter param = parameters.get(i);
			if (!(param instanceof Identifier)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Filters out any percepts that are currently on the disabledPercept-list.
	 * @param percepts	A list of percepts
	 * @return The given perceptlist without the percepts currently in disabledPercepts
	 */
	public LinkedList<Percept> filterPercepts(final List<Percept> percepts) {
		LinkedList<Percept> result = new LinkedList<Percept>();

		for (Percept percept : percepts) {
			if (!disabledPercepts.contains(percept.getName())) {
				result.add(percept);
			}
		}

		return result;

	}



}
