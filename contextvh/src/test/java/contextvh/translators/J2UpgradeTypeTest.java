package contextvh.translators;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

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
	private J2UpgradeType translator;
	/**
	 * The indicators to translate.
	 */
	private UpgradeType upgradeType;
	private UpgradePair upgradePair;
	private ArrayList<UpgradePair> a;

	/**
	 * Initialise before every test.
	 */
	@Before
	public void init() {
		translator = new J2UpgradeType();
		upgradeType = mock(UpgradeType.class);
		upgradePair = mock(UpgradePair.class);
		a = new ArrayList<UpgradePair>();
	}

	/**
	 * Tests if the translator returns the source and target of an upgradePair.
	 *
	 * @throws TranslationException
	 *             thrown if translation fails.
	 */
	@Test
	public void translatorTest1() throws TranslationException {
		a.add(upgradePair);
		when(upgradeType.getPairs()).thenReturn(a);
		translator.translate(upgradeType);
		verify(upgradePair, times(1)).getSourceFunctionID();
		verify(upgradePair, times(1)).getTargetFunctionID();
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
		translator.translate(upgradeType);
		verify(upgradeType, times(1)).getPairs();
	}

}
