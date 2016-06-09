package contextvh.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Tests usage of CoordinateUtils.
 * @author Rico Tubbing
 */
public class CoordinateUtilsTest {

	private final Coordinate coordinate1 = new Coordinate(5, 10, 0);
	private final Coordinate coordinate2 = new Coordinate(8, 2, 5);

	/**
	 * Tests the plus method.
	 */
	@Test
	public void testPlus() {
		final Coordinate coord = CoordinateUtils.plus(coordinate1, coordinate2);
		final Coordinate expected = new Coordinate(13, 12, 5);
		assertEquals(coord, expected);
	}

	/**
	 * Tests the minus method.
	 */
	@Test
	public void testMinus() {
		final Coordinate coord = CoordinateUtils.minus(coordinate1, coordinate2);
		final Coordinate expected = new Coordinate(-3, 8, -5);
		assertEquals(coord, expected);
	}

	/**
	 * Tests the times method.
	 */
	@Test
	public void testTimes() {
		final Coordinate coord = CoordinateUtils.times(coordinate1, 2);
		final Coordinate expected = new Coordinate(10, 20, 0);
		assertEquals(coord, expected);
	}

	/**
	 * Tests the divide method.
	 */
	@Test
	public void testDivide() {
		final Coordinate coord = CoordinateUtils.divide(coordinate1, 2);
		final Coordinate expected = new Coordinate(2.5, 5, 0);
		assertEquals(coord, expected);
	}
}
