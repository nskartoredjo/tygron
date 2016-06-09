package contextvh.util;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * Utilty class for coordinates.
 * @author Max Groenenboom
 *
 */
public final class CoordinateUtils {

	/**
	 * Private constructor for checkstyle.
	 */
	private CoordinateUtils() { }

	/**
	 * Add two <code>Coordinate</code>s together.
	 * @param c1 First Coordinate.
	 * @param c2 Second Coordinate.
	 * @return A new Coordinate.
	 */
	public static Coordinate plus(final Coordinate c1, final Coordinate c2) {
		return new Coordinate(c1.x + c2.x, c1.y + c2.y, c1.z + c2.z);
	}

	/**
	 * Subtract one <code>Coordinate</code> from another.
	 * @param c1 First coordinate.
	 * @param c2 Second coordinate.
	 * @return A new Coordinate.
	 */
	public static Coordinate minus(final Coordinate c1, final Coordinate c2) {
		return new Coordinate(c1.x - c2.x, c1.y - c2.y, c1.z - c2.z);
	}

	/**
	 * Multiply a <code>Coordinate</code> with a number.
	 * @param c The coordinate.
	 * @param num The number.
	 * @return A new Coordinate.
	 */
	public static Coordinate times(final Coordinate c, final double num) {
		return new Coordinate(c.x * num, c.y * num, c.z * num);
	}

	/**
	 * Divide a <code>Coordinate</code> with a number.
	 * @param c The coordinate.
	 * @param num The number.
	 * @return A new Coordinate.
	 */
	public static Coordinate divide(final Coordinate c, final double num) {
		return new Coordinate(c.x / num, c.y / num, c.z / num);
	}

}
