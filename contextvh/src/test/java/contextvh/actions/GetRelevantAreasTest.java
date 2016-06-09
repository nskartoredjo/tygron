package contextvh.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import contextvh.ContextEntity;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import nl.tytech.util.JTSUtils;
import tygronenv.translators.J2MultiPolygon;

/**
 * Test for the getRelevantAreas class.
 * @author Rico
 *
 */
public class GetRelevantAreasTest {

	private static final String SUB_ACTION_NAME = "test_action";

	private GetRelevantAreas action = new GetRelevantAreas();
	private RelevantAreasAction mockSubAction = mock(RelevantAreasAction.class);
	private ContextEntity mockEntity = mock(ContextEntity.class);

	/**
	 * Register the multipolygon translator.
	 */
	@Before
	public void setUp() {
		GetRelevantAreas.TRANSLATOR.registerJava2ParameterTranslator(new J2MultiPolygon());
		when(mockSubAction.getInternalName()).thenReturn(SUB_ACTION_NAME);
		action.addInternalAction(mockSubAction);
	}

	/**
	 * Test if convertMPtoPL function gives the correct area.
	 * @throws TranslationException A translate exception.
	 */
	@Test
	public void convertMPtoPLTest() throws TranslationException {
		Geometry triangle = GetRelevantAreasBuildTest.createTriangle();
		MultiPolygon triangleMP = JTSUtils.createMP(triangle);
		ParameterList list = GetRelevantAreas.convertMPtoPL(triangleMP);
		Numeral area = (Numeral) list.get(1);
		final double expected = 0.5;
		assertEquals(expected, area.getValue().doubleValue(), 0);
	}

	/**
	 * Tests if GetRelevantAreas.call calls the internal action registered with it.
	 * @throws TranslationException Unexpected exception.
	 */
	@Test
	public void callTest() throws TranslationException {
		LinkedList<Parameter> parameters = new LinkedList<Parameter>();
		parameters.add(new Numeral(0));
		parameters.add(new Identifier(SUB_ACTION_NAME));
		action.call(mockEntity, parameters);
		verify(mockSubAction).internalCall(any(), eq(mockEntity), any());
	}
}
