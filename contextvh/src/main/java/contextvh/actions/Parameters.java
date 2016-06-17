package contextvh.actions;

import java.util.HashMap;
import java.util.LinkedList;

import eis.iilang.Function;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;

/**
 * A <code>HashMap&lt;String, LinkedList&lt;Parameter&gt;&gt;</code> saving the parameters
 * of a CustomAction call. Made as a separate object to account for possible code changes and
 * to make usage of the parameters easier.
 * @author Max Groenenboom
 */
public class Parameters extends HashMap<String, LinkedList<Parameter>> {
	private static final long serialVersionUID = 6054823572206388545L;

	/**
	 * Create a Empty Parameters object b.
	 */
	public Parameters() {
		super();
	}

	/**
	 * Create a Parameters object by parsing the ParameterList as a list of
	 * Function parameters, whose name-params pairs are used as key-value pairs.
	 * @param parameters
	 *            The ParameterList given to the action.
	 * @throws IllegalArgumentException
	 *             If one of the parameters wasn't a function.
	 */
	public Parameters(final ParameterList parameters) {
		super();
		for (Parameter param : parameters) {
			if (!(param instanceof Function)) {
				throw new IllegalArgumentException("Invalid parameter in parameterlist.");
			}
			Function paramAsFunction = (Function) param;
			put(paramAsFunction.getName(), paramAsFunction.getParameters());
		}
	}
}
