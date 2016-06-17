package contextvh.translators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eis.eis2java.translation.Translator;
import eis.iilang.Parameter;
import org.junit.Before;
import org.junit.Test;

import eis.eis2java.exception.TranslationException;
import nl.tytech.data.engine.item.UpgradeType;
import nl.tytech.data.engine.serializable.UpgradePair;

/**
 * Test for the J2UpgradeType class.
 *
 * @author Marco Houtman
 *
 */
public class J2UpgradeTypeTest {
	/**
	 * Translator of the J2Indicator to test.
	 */
	private Translator translator = Translator.getInstance();
	/**
	 * The indicators to translate.
	 */
	private UpgradeType upgradeType;
	private List<UpgradePair> pairs;
	private J2UpgradePair pairTranslator;
	/**
	 * Initialise before every test.
	 * @throws TranslationException thrown when translation fails
	 */
	@Before
	public void init() throws TranslationException {
		upgradeType = mock(UpgradeType.class);
		pairTranslator = mock(J2UpgradePair.class);
		when(pairTranslator.translatesFrom()).thenCallRealMethod();
		when(pairTranslator.translate(any())).thenReturn(new Parameter[0]);
		pairs = new ArrayList<>();
		translator.registerJava2ParameterTranslator(new J2UpgradeType());
		translator.registerJava2ParameterTranslator(pairTranslator);
	}

	/**
	 * Tests if the translator returns the source and target of an upgradePair.
	 *
	 * @throws TranslationException
	 *             thrown if translation fails.
	 */
	@Test
	public void tranlatorTest1() throws TranslationException {
		pairs = Collections.singletonList(new UpgradePair());
		when(upgradeType.getPairs()).thenReturn(pairs);
		translator.translate2Parameter(upgradeType);
		verify(pairTranslator, times(1)).translate(any());
	}

	/**
	 * Tests if the translator will not try to look for a source and target of
	 * an upgradePair if the translator has an empty list of pairs.
	 *
	 * @throws TranslationException
	 *             if translation fails.
	 */
	@Test
	public void translatorTest2() throws TranslationException {
		translator.translate2Parameter(upgradeType);
		verify(upgradeType, times(1)).getPairs();
		verify(pairTranslator, times(0)).translate(any());
	}

	/**
	 * Tests if the translator returns the source and target of an upgradePair.
	 * @throws TranslationException thrown if translation fails.
	 */
	@Test
	public void tranlatorTest3() throws TranslationException {
		pairs = Arrays.asList(new UpgradePair(), new UpgradePair());
		when(upgradeType.getPairs()).thenReturn(pairs);
		translator.translate2Parameter(upgradeType);
		verify(pairTranslator, times(2)).translate(any());
	}

}
