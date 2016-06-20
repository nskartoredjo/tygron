package contextvh.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.MultiPolygon;

import contextvh.ContextEntity;
import contextvh.util.MapUtilsExt;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Action;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
public class BuySpecifiedLandTest {

	BuySpecifiedLand BSL;
	LinkedList<Parameter> parameters;
	ContextEntity ce;
	LinkedList<Parameter> landParams;


	
	/**
	 * @todo
	 */
	@Before
	public void init() {
		
		BSL = new BuySpecifiedLand();
		ce = mock(ContextEntity.class);
		parameters = new LinkedList<Parameter>();
		
		parameters.add(new Numeral(0));
		parameters.add(new Numeral(1));
		parameters.add(new Numeral(2));
		parameters.add(new Numeral(3));
		parameters.add(new Numeral(4));
		parameters.add(new Numeral(5));
		parameters.add(new Numeral(6));
		
		MultiPolygon multip = mock(MultiPolygon.class);
		when(MapUtilsExt.getLand("build_building", 0, 1)).thenReturn(multip);

		landParams = new LinkedList<Parameter>();
		
		
	}

	/**
	 * @throws TranslationException 
	 * @todo
	 */
	@Test
	public void testCall() throws TranslationException {
		BSL.call(ce, parameters);
		verify(ce, atLeast(1)).performAction(new Action("map_buy_land", landParams));
	}
	
	/**
	 * @todo
	 */
	@Test
	public void getName() {
		assertEquals(BSL.getName(),"buy_specified_land");
	}
	
	
	
}
