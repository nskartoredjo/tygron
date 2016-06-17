package contextvh.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import contextvh.ContextEnv;
import eis.exceptions.ManagementException;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import nl.tytech.util.JTSUtils;
import tygronenv.MyEnvListener;

/**
 * Test the GetRelevantAreasBuild class.
 * @author Rico Tubbing
 *
 */
public class GetRelevantAreasBuildTest {

	private static final String STAKEHOLDERS = "stakeholders";
	private static final String MUNICIPALITY = "MUNICIPALITY";
	private static final String PROJECT = "project";
	private static final Identifier PROJECTNAME = new Identifier("testutilsmap");

	private static final double AREA_MUNICIPALITY = (1000 - 0) * (1000 - 0);

	/**
	 * Test if createNewPolygon returns a bigger geometry.
	 */
	@Test
	public void testCreateNewPolygonGoodWeather() {
		GetRelevantAreasBuild action = new GetRelevantAreasBuild(null);
		Geometry triangle = createTriangle();
		Geometry result = action.createNewPolygon(triangle);
		final double area = 0.75;
		assertEquals(area, result.getArea(), 0);
	}

	/**
	 * Test if createNewPolygon returns the same if we give it no triangle.
	 */
	@Test
	public void testCreateNewPolygonBadWeather() {
		GetRelevantAreasBuild action = new GetRelevantAreasBuild(null);
		Geometry triangle = createSquare();
		Geometry result = action.createNewPolygon(triangle);
		assertEquals(1, result.getArea(), 0);
		assertTrue(result == triangle);
	}


	/**
	 * Test the getUsaubleLand function.
	 * @throws ManagementException {@link MangementExption}
	 * @throws InterruptedException {@link InterruptedException}
	 */
	@Test
	public void testGetUsableLand() throws ManagementException, InterruptedException {
		ContextEnv env = new ContextEnv();
		joinAsInhabitants(env);
		GetRelevantAreasBuild action = new GetRelevantAreasBuild(null);
		//9m2 + 4m2 building
		//1m2 water
		final double reservedArea = 9 + 4 + 1;
		double area = action.getUsableArea(env.getEntity(MUNICIPALITY), new Parameters()).getArea();
		assertEquals(AREA_MUNICIPALITY - reservedArea, area, 0);
	}

	/**
	 * Test the getUsaubleLand function with zones.
	 * @throws ManagementException {@link MangementExption}
	 * @throws InterruptedException {@link InterruptedException}
	 */
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void testGetUsableLandWithZones() throws ManagementException, InterruptedException {
		ContextEnv env = new ContextEnv();
		joinAsInhabitants(env);
		GetRelevantAreasBuild action = new GetRelevantAreasBuild(null);
		//9m2 + 4m2 building
		final double reservedArea = 9 + 4;
		LinkedList<Parameter> zones = new LinkedList<Parameter>();
		zones.add(new Numeral(2));
		final int zoneb = 3;
		zones.add(new Numeral(zoneb));
		Parameters params = new Parameters();
		params.put("zones", zones);
		double area = action.getUsableArea(env.getEntity(MUNICIPALITY), params).getArea();
		assertEquals(AREA_MUNICIPALITY / 2 - reservedArea, area, 0);
	}

	/**
	 * Init env and ask for inhabitant as stakeholder.
	 * @param env The environment
	 * @throws ManagementException {@link MangementExption}
	 * @throws InterruptedException {@link InterruptedException}
	 */
	private void joinAsInhabitants(final ContextEnv env) throws ManagementException, InterruptedException {
		MyEnvListener listener = new MyEnvListener();
		env.attachEnvironmentListener(listener);

		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		parameters.put(STAKEHOLDERS, new ParameterList(new Identifier(MUNICIPALITY)));

		// any slot so not specified.
		env.init(parameters);

		assertEquals(MUNICIPALITY, listener.waitForEntity());
	}

	/**
	 * Create a new triangle with an area of 0.5 square meters.
	 * @return A triangle.
	 */
	public static Geometry createTriangle() {
		Coordinate c1 = new Coordinate(0, 0);
		Coordinate c2 = new Coordinate(0, 1);
		final Coordinate c3 = new Coordinate(1, 1);
		List<Coordinate> coordinates = new LinkedList<Coordinate>();
		coordinates.add(c1);
		coordinates.add(c2);
		coordinates.add(c3);
		coordinates.add(c1);
		return JTSUtils.createPolygon(coordinates);
	}

	/**
	 * Create a square.
	 * @return A square.
	 */
	private Geometry createSquare() {
		Coordinate c1 = new Coordinate(0, 0);
		Coordinate c2 = new Coordinate(0, 1);
		Coordinate c3 = new Coordinate(1, 0);
		Coordinate c4 = new Coordinate(1, 1);
		List<Coordinate> coordinates = new LinkedList<Coordinate>();
		coordinates.add(c1);
		coordinates.add(c2);
		coordinates.add(c4);
		coordinates.add(c3);
		coordinates.add(c1);
		return JTSUtils.createPolygon(coordinates);
	}

}
