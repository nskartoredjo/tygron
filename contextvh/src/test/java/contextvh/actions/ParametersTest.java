package contextvh.actions;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;

import org.junit.Test;

import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;

/**
 * Tests the parameter data class.
 * @author Max Groenenboom
 */
public class ParametersTest {
	private static final String NUMERAL_KEY = "numPar";
	private static final int NUMERAL_VALUE = 0;
	private static final String IDENTIFIER_KEY = "identPar";
	private static final String IDENTIFIER_VALUE = "Ident";

	private static final Numeral NUMERAL = new Numeral(NUMERAL_VALUE);
	private static final Identifier IDENTIFIER = new Identifier(IDENTIFIER_VALUE);

	/**
	 * Tests if the Parameters constructor parses properly.
	 */
	@Test
	public void parametersConstructorTest() {
		final ParameterList rawParameters = new ParameterList(
				new Function(NUMERAL_KEY, NUMERAL),
				new Function(IDENTIFIER_KEY, IDENTIFIER));
		final Parameters parameters = new Parameters(rawParameters);

		final LinkedList<Parameter> expectedNumeral = new LinkedList<Parameter>();
		final LinkedList<Parameter> expectedIdentifier = new LinkedList<Parameter>();
		expectedNumeral.add(NUMERAL);
		expectedIdentifier.add(IDENTIFIER);

		assertEquals(parameters.get(NUMERAL_KEY), expectedNumeral);
		assertEquals(parameters.get(IDENTIFIER_KEY), expectedIdentifier);
	}

	/**
	 * Tests if an exception is thrown when the parameters are invalid.
	 * Suppresses unused warnings because it just tests for the exception.
	 */
	@SuppressWarnings("unused")
	@Test (expected = IllegalArgumentException.class)
	public void parametersConstructorInvalidParamTest() {
		final ParameterList rawParameters = new ParameterList(
				new Numeral(NUMERAL_VALUE));
		final Parameters parameters = new Parameters(rawParameters);
	}
}
