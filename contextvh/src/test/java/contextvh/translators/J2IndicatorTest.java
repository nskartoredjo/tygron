package contextvh.translators;

import nl.tytech.data.engine.item.Indicator;
import nl.tytech.data.engine.serializable.MapType;

import org.junit.Before;
import org.junit.Test;

import eis.eis2java.exception.TranslationException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the J2Indicator class.
 *
 * @author Stefan de Vringer
 */
public class J2IndicatorTest {
	/**
	 * translator the J2Indicator to test.
	 */
	private J2Indicator translator;
	/**
	 * the indicator to translate.
	 */
	private Indicator indicator;

	/**
	 * Initialise before every test.
	 */
	@Before
	public void init() {
		translator = new J2Indicator();
		indicator = mock(Indicator.class);
	}

	/**
	 * Test whether the translation method asks for the correct properties of
	 * the indicator. and whether the string is correctly handled.
	 *
	 * @throws TranslationException
	 *             thrown if the translate method fails.
	 */
	@Test
	public void translatorTest1() throws TranslationException {
		when(indicator.getExplanation()).thenReturn(
				"<p hidden>0multiN0\\t-0.21%\\n1\\t1\\n2\\t1\\n3\\t1\\n4\\t1\\n5\\t1\\n6"
				+ "\\t1\\n7\\t1\\n8\\t1\\n9\\t1\\n10\\t1\\n11\\t1\\n12\\t1\\n</p>");
		translator.translate(indicator);
		verify(indicator, times(1)).getExactNumberValue(MapType.MAQUETTE);
		verify(indicator, times(1)).getTarget();
		verify(indicator, times(2)).getExplanation();
	}
}
