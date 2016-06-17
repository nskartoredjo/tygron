package contextvh.translators;

import com.vividsolutions.jts.geom.MultiPolygon;
import eis.eis2java.exception.TranslationException;
import nl.tytech.data.engine.item.Zone;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for the ContextJ2Stakeholder translator.
 *
 * @author Tom
 */
public class ContextJ2ZoneTest {
	/**
	 * Translator for the Zone class.
	 */
	private ContextJ2Zone translator;

	/**
	 * Zone instance to translate.
	 */
	private Zone z;

	/**
	 * Initialization method called before every test.
	 */
	@Before
	public void init() {
		final double areaSize = 5.0;
		translator = new ContextJ2Zone();

		z = mock(Zone.class);
		MultiPolygon mp = mock(MultiPolygon.class);

		when(z.getMultiPolygon()).thenReturn(mp);
		when(mp.getArea()).thenReturn(areaSize);
	}

	/**
	 * Test method which verifies that methods that are called. The other
	 * methods cannot be verified, since they are final methods (name && id).
	 * That does not work with mockito.
	 *
	 * @throws TranslationException
	 *             thrown if translating fails.
	 */
	@Test
	public void testTranslate() throws TranslationException {
		translator.translate(z);
		verify(z, atLeast(1)).getMaxAllowedFloors();
		verify(z, atLeast(1)).getMultiPolygon();
		verify(z, atLeast(1)).getAllowedCategories();
	}

}
