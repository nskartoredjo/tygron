package contextvh.actions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import contextvh.ContextEntity;
import contextvh.util.CoordinateUtils;
import contextvh.util.MapUtils;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import nl.tytech.util.JTSUtils;
import nl.tytech.util.logger.TLogger;

/**
 * TODO Make javadoc for class.
 * @author Max Groenenboom
 */
public class GetRelevantAreasBuild implements RelevantAreasAction {

	private static GetRelevantAreas parent;

	/**
	 * Create a new <code>GetRelevantAreasBuild</code> action.
	 * @param par The parent GetRelevantAreas of this action.
	 */
	public GetRelevantAreasBuild(final GetRelevantAreas par) {
		parent = par;
	}

	@Override
	public Percept call(final ContextEntity caller, final LinkedList<Parameter> parameters)
			throws TranslationException {
		// Redirect call to GetRelevantAreas to avoid code duplication.
		parameters.add(1, new Identifier(getInternalName()));
		return parent.call(caller, parameters);
	}

	@Override
	public String getName() {
		return "get_buildable_areas";
	}

	@Override
	public String getInternalName() {
		return "build";
	}

	@Override
	public void internalCall(final Percept createdPercept,
			final ContextEntity caller, final ParameterList parameters) {
		MultiPolygon constructableLand = getUsableArea(caller, parameters);

		final int minArea = 200, maxArea = 2000;
		final int maxPolys = 15;
		final int bufferUp = 5, bufferDown = -10;
		int numPolys = 0;
		final ParameterList results = new ParameterList();
		for (Polygon poly: JTSUtils.getPolygons(constructableLand)) {
			final List<Polygon> listPolygon = JTSUtils.getTriangles(poly, minArea);
			for (Geometry geom : listPolygon) {
				if (numPolys > maxPolys) {
					break;
				}
				geom = createNewPolygon(geom);
				geom = geom.intersection(constructableLand);
				geom = geom.buffer(bufferDown).buffer(bufferUp);
				if (geom.getArea() < minArea / 2 || geom.getArea() > maxArea) {
					continue;
				}
				MultiPolygon mp = JTSUtils.createMP(geom);
				constructableLand = JTSUtils.createMP(constructableLand.difference(mp));
				try {
					results.add(GetRelevantAreas.convertMPtoPL(mp));
				} catch (TranslationException e) {
					TLogger.exception(e);
					continue;
				}
				numPolys++;
			}
		}
		createdPercept.addParameter(results);
	}

	/**
	 * Returns all area that can be built on.
	 * @param caller The caller of the action.
	 * @param parameters The parameters provided to the action.
	 * @return The multiPolygon that can be built on.
	 */
	protected MultiPolygon getUsableArea(final ContextEntity caller, final ParameterList parameters) {
		// Get a MultiPolygon of all lands combined.
		Integer connectionID = caller.getSlotConnection().getConnectionID();

		final Integer stakeholderID = caller.getStakeholder().getID();
		MultiPolygon constructableLand = MapUtils.getStakeholderLands(connectionID, stakeholderID);

		// Remove all pieces of land that cannot be build on (water).
		constructableLand = MapUtils.removeWater(connectionID, constructableLand);

		// Remove all pieces of reserved land.
		constructableLand = MapUtils.removeReservedLand(connectionID, constructableLand);

		// Remove all pieces of occupied land.
		constructableLand = MapUtils.removeBuildings(connectionID, constructableLand);

		return constructableLand;
	}

	/**
	 * Create a new polygon from a triangle, by creating two new corner points.
	 * @param triangle The triangle to derive the square from.
	 * @return The new polygon.
	 */
	protected Geometry createNewPolygon(final Geometry triangle) {
		final int triangleAmountOfCoords = 4;
		final double distanceFromOppositeCorner = 1.25;
		if (triangle.getNumPoints() != triangleAmountOfCoords) {
			return triangle;
		} else {
			Coordinate[] coords = triangle.getCoordinates();

			// Create first new point.
			Coordinate newPoint1 = CoordinateUtils.plus(coords[0], coords[1]);
			newPoint1 = CoordinateUtils.divide(newPoint1, 2);
			newPoint1 = CoordinateUtils.minus(newPoint1, coords[2]);
			newPoint1 = CoordinateUtils.plus(coords[2], CoordinateUtils.times(newPoint1,
					distanceFromOppositeCorner));

			// Create second new point.
			Coordinate newPoint2 = CoordinateUtils.plus(coords[0], coords[2]);
			newPoint2 = CoordinateUtils.divide(newPoint2, 2);
			newPoint2 = CoordinateUtils.minus(newPoint2, coords[1]);
			newPoint2 = CoordinateUtils.plus(coords[1], CoordinateUtils.times(newPoint2,
					distanceFromOppositeCorner));

			// Create list with coordinates for the new Polygon.
			List<Coordinate> newCoords = Arrays.asList(new Coordinate[] {
					coords[0], newPoint1, coords[1], coords[2], newPoint2, coords[0]
			});

			return JTSUtils.createPolygon(newCoords);
		}
	}
}
