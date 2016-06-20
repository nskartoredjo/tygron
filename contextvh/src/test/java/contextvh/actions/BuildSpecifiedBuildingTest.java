package contextvh.actions;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import com.vividsolutions.jts.geom.MultiPolygon;

import contextvh.ContextEntity;
import contextvh.util.MapUtilsExt;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eis.eis2java.exception.TranslationException;
import eis.iilang.Action;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;

public class BuildSpecifiedBuildingTest {

	BuildSpecifiedBuilding BSB;
	
	LinkedList<Parameter> parameters;
	ContextEntity ce;
	LinkedList<Parameter> landParams;
	
	/**
	 * Setup for call.
	 * @throws TranslationException 
	 */
	@Before
	public void init() throws TranslationException {
		
		ce = mock(ContextEntity.class);
		BSB =  new BuildSpecifiedBuilding();
		parameters = new LinkedList<Parameter>();
		
		parameters.add(new Numeral(0));
		parameters.add(new Numeral(1));
		parameters.add(new Numeral(2));
		parameters.add(new Numeral(3));
		parameters.add(new Numeral(4));
		parameters.add(new Numeral(5));
		
		PowerMockito.mockStatic(MapUtilsExt.class);
		MultiPolygon multip = mock(MultiPolygon.class);
		MultiPolygon multip2 = mock(MultiPolygon.class);
		MultiPolygon multip3 = mock(MultiPolygon.class);
		
		Mockito.when(MapUtilsExt.getLand("build_building", 0, 1)).thenReturn(multip);
		
		
		when(MapUtilsExt.excludeBuildingLand(multip)).thenReturn(multip2);
		when(MapUtilsExt.getSpecifiedLand(multip2, 3.0, 4.0, 5.0)).thenReturn(multip3);
		when(multip3.toString()).thenReturn("mp3");
		
		landParams = new LinkedList<Parameter>();
		landParams.add(new Numeral(1));
		landParams.add(new Numeral(6));
		landParams.add(new Function("multipolygon", new Identifier(multip3.toString())));
	}
	
	/**
	 * test for call.
	 * @RunWith(PowerMockRunner.class)
	 * @PrepareForTest(MapUtilsExt.class)
	 * @throws TranslationException 
	 */
	@Test
	public void testCall() throws TranslationException {
		BSB.call(ce, parameters);
		verify(ce, atLeast(1)).performAction(new Action("building_plan_construction", landParams));
	}
	
	/**
	 * @RunWith(PowerMockRunner.class)
	 * @PrepareForTest(MapUtilsExt.class)
	 * test for getName.
	 */
	@Test
	public void getName() {
		assertEquals(BSB.getName(),"build_specified_building");
	}
	
}
