package contextvh.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import com.vividsolutions.jts.geom.MultiPolygon;

import contextvh.ContextEntity;
import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Translator;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import nl.tytech.data.engine.serializable.MapType;

/**
 * Creates a list of areas that can be used with the corresponding actionType.
 * Possible actionTypes are "build", "demolish" and "sell". It is also possible
 * to specify a filter, with zone([id1],[id2]) or stakeholder([id1],[id2])
 * functions, filtering only on pieces of land that are owned by the specified
 * stakeholder, or are in the specified zone.
 *
 * @author Max Groenenboom
 */
public class GetRelevantAreas implements CustomAction {

	protected static final Translator TRANSLATOR = Translator.getInstance();
	protected static final MapType DEFAULT_MAPTYPE = MapType.MAQUETTE;

	private final HashMap<String, RelevantAreasAction> internalActions = new HashMap<String, RelevantAreasAction>();

	/**
	 * Creates a GetRelevantAreas action and adds its internal actions.
	 */
	public GetRelevantAreas() {
		addInternalAction(new GetRelevantAreasBuild(this));
	}

	/**
	 * Adds an internal action to the hashMap of internal actions.
	 *
	 * @param action
	 *            The RelevantAreasAction to add.
	 */
	public void addInternalAction(final RelevantAreasAction action) {
		internalActions.put(action.getInternalName(), action);
	}

	@Override
	public String getName() {
		return "get_relevant_areas";
	}

	@Override
	public Percept call(final ContextEntity caller, final LinkedList<Parameter> parameters)
			throws TranslationException {
		// Get and translate parameters.
		Iterator<Parameter> params = parameters.iterator();
		Number callID = ((Numeral) params.next()).getValue();
		String actionType = ((Identifier) params.next()).getValue();
		ParameterList filters = new ParameterList();
		if (params.hasNext()) {
			Parameter filterParam = params.next();
			// If the filter parameter is not a ParameterList, it is
			// invalid.
			if (filterParam instanceof ParameterList) {
				filters = (ParameterList) filterParam;
			}
		}

		return createPercept(caller, actionType, callID, filters);
	}

	/**
	 * Create the actual Percept, after the parameters have been parsed.
	 *
	 * @param caller
	 *            The ContextEntity that called the action.
	 * @param actionType
	 *            The type of the action.
	 * @param callID
	 *            The ID of the call.
	 * @param parameters
	 *            A ParameterList of parameters provided by the agent.
	 * @return The constructed Percept.
	 * @throws IllegalArgumentException
	 *             When an invalid internal action parameter is provided.
	 */
	private Percept createPercept(final ContextEntity caller, final String actionType, final Number callID,
			final ParameterList parameters) {
		final Percept result = new Percept("relevant_areas");
		final Parameters params = new Parameters(parameters);
		result.addParameter(new Numeral(callID));

		RelevantAreasAction action = internalActions.get(actionType);
		if (action == null) {
			throw new IllegalArgumentException("unknown action GetRelevantAreas(_, " + actionType + ", _)");
		} else {
			action.internalCall(result, caller, params);
		}

		return result;
	}

	/**
	 * Converts a MultiPolygon to a ParameterList.
	 *
	 * @param mp
	 *            The given MultiPolygon.
	 * @return The resulting ParameterList.
	 * @throws TranslationException
	 *             If an error occurred while translating.
	 */
	protected static ParameterList convertMPtoPL(final MultiPolygon mp) throws TranslationException {
		return new ParameterList(TRANSLATOR.translate2Parameter(mp)[0], new Numeral(mp.getArea()));
	}
}
