package contextvh.translators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import contextvh.translators.GroundLayerType2J;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Identifier;
import nl.tytech.data.engine.item.Building.GroundLayerType;

/**
 * This class containes unit tests for the GroundLaterType class.
 * @author Rico
 *
 */
public class GroundLayerType2JTest {

	private final GroundLayerType2J translator = new GroundLayerType2J();
	private final GroundLayerType surfaceType = GroundLayerType.SURFACE;
	private final GroundLayerType undergroundType = GroundLayerType.UNDERGROUND;


	/**
	 * Test if translating with a bad param throws a
	 * {@link TranslationException}.
	 * @throws TranslationException
	 * 		A translate exception.
	 */
	@Test(expected = TranslationException.class)
	public void testBadWeather() throws TranslationException {
		Identifier parameter = new Identifier("bad");
		translator.translate(parameter);
	}

	/**
	 * Test if translating with a good param (surface),
	 * in lower case gives us a surface.
	 * @throws TranslationException
	 * 		A translate exception.
	 */
	@Test
	public void testGoodWeatherSurface() throws TranslationException {
		Identifier parameter = new Identifier("surface");
		GroundLayerType type = translator.translate(parameter);
		assertEquals(type, surfaceType);
	}
	/**
	 * Test if translating with a good param (underground),
	 * in lower case gives us a underground.
	 * @throws TranslationException
	 * 		A translate exception.
	 */

	@Test
	public void testGoodWeatherUnderground() throws TranslationException {
		Identifier parameter = new Identifier("underground");
		GroundLayerType type = translator.translate(parameter);
		assertEquals(type, undergroundType);
	}

	/**
	 * Tests if the translatesTo function returns the proper class.
	 */
	@Test
	public void testTranslatesTo() {
		assertEquals(translator.translatesTo(), GroundLayerType.class);
	}

}
