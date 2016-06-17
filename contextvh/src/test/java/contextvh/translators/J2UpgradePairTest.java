package contextvh.translators;

import eis.eis2java.exception.TranslationException;
import eis.iilang.Function;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import nl.tytech.data.engine.serializable.UpgradePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the J2UpgradePair class.
 *
 * @author Stefan Breetveld
 */
public class J2UpgradePairTest {

	private J2UpgradePair translator;
	private UpgradePair upPair;

	/**
	 *  * Initialize the test scenario.
	 */
	@Before
	public void init() {
		translator = new J2UpgradePair();
		upPair = Mockito.mock(UpgradePair.class);
	}

	/**
	 * test if a pair is translated correctly.
	 *
	 * @throws TranslationException thrown if the translation went wrong.
	 */
	@Test
	public void translatePair() throws TranslationException {
		translator.translate(upPair);
		verify(upPair, times(1)).getSourceFunctionID();
		verify(upPair, times(1)).getTargetFunctionID();
	}

	/**
	 * test if a pair is formatted correctly.
	 *
	 * @throws TranslationException thrown if the translation went wrong.
	 */
	@Test
	public void translatePairFormat() throws TranslationException {
		when(upPair.getSourceFunctionID()).thenReturn(0);
		when(upPair.getTargetFunctionID()).thenReturn(1);
		Parameter[] parameters = translator.translate(upPair);
		Function func = (Function) parameters[0];
		List<Parameter> arguments = func.getParameters();
		assertEquals("upgrade_pair", func.getName());
		assertEquals(new Numeral(0), arguments.get(0));
		assertEquals(new Numeral(1), arguments.get(1));
	}

}
