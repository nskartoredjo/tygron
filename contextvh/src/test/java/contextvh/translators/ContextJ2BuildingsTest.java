package contextvh.translators;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Translator;
import eis.iilang.Function;
import eis.iilang.Parameter;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.serializable.Category;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import tygronenv.translators.J2Category;
import tygronenv.translators.J2MultiPolygon;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by Stefan Breetveld on 23-5-2016. In package tygronenv.translators.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Building.class)
public class ContextJ2BuildingsTest {

	private Translator translator = Translator.getInstance();

	/**
	 * Test whether Java to Building({@link ContextJ2Building}) asks for the
	 * correct properties of the indicator.
	 *
	 * @throws TranslationException
	 *             thrown if the translate method fails.
	 */
	@Test
	public void j2BuildingTest() throws TranslationException {
		GeometryFactory gf = new GeometryFactory();
		MultiPolygon mp = PowerMockito.spy(gf.createMultiPolygon(new Polygon[0]));
		Collection<Category> categories = Arrays.asList(Category.EDUCATION, Category.BRIDGE);

		Building building = PowerMockito.mock(Building.class);

		PowerMockito.when(building.getMultiPolygon(any())).thenReturn(mp);
		PowerMockito.when(building.getCategories()).thenReturn(categories);

		J2Category j2c = PowerMockito.spy(new J2Category());
		J2MultiPolygon j2mp = PowerMockito.spy(new J2MultiPolygon());
		translator.registerJava2ParameterTranslator(new ContextJ2Building());
		translator.registerJava2ParameterTranslator(j2c);
		translator.registerJava2ParameterTranslator(j2mp);

		Parameter[] params = translator.translate2Parameter(building);
		Function func = (Function) params[0];

		assertEquals("building", func.getName());
		verify(building, times(1)).getID();
		verify(building, times(1)).getName();
		verify(building, times(1)).getOwnerID();
		verify(building, times(1)).getConstructionYear();
		verify(building, times(1)).getCategories();
		verify(building, times(1)).getFloors();
		verify(building, times(1)).getFunctionID();
		verify(building, times(1)).getMultiPolygon(any());
		verify(mp, times(1)).getArea();
		verify(j2c, times(2)).translate(any());
		verify(j2mp, times(1)).translate(any());
	}
}
