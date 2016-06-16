package contextvh.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.math.Vector2D;

import eis.eis2java.exception.TranslationException;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.item.Land;
import nl.tytech.data.engine.item.Setting;
import nl.tytech.data.engine.item.Terrain;
import nl.tytech.data.engine.item.Zone;
import nl.tytech.data.engine.serializable.Category;
import nl.tytech.util.JTSUtils;

public class MapUtilsExt extends MapUtils {

	/**
	 * protected constructor
	 */
	protected MapUtilsExt() {
		super();
	}

	/**
	 * Test if a filtered land is empty, if so, use the alternative land
	 * 
	 * @param land
	 *            the alternative land
	 * @param filteredLand
	 *            the filtered land which may be empty
	 * @return the filteredLand only if it is not empty, otherwise return land
	 */
	public static MultiPolygon getNonEmptyLand(MultiPolygon land, MultiPolygon filteredLand) {
		if (filteredLand.isEmpty())
			return land;
		else
			return filteredLand;
	}

	/**
	 * uses the land to create sub-polygons based on the specifications. A
	 * random located land will be returned.
	 * 
	 * @param land
	 *            The land which should be the base to create the smaller
	 *            specified sub-polygons.
	 * @param depth
	 *            The depth specification.
	 * @param width
	 *            The width specification.
	 * @param distanceToRoad
	 *            The DistanceToRoad specification.
	 * @return one multi-polygon randomly collected from a list of sub-polygons.
	 * @throws TranslationException
	 */
	public static MultiPolygon getSpecifiedLand(final MultiPolygon land, final double depth, final double width,
			final double distanceToRoad) throws TranslationException {
		List<Polygon> polygons = JTSUtils.getPolygons(land);
		List<MultiPolygon> multipolygons = new ArrayList<MultiPolygon>();

		for (Polygon polygon : polygons) {
			if ((depth + width) > 0) {
				List<LineString> lineSegments = MapUtilsExt.getLineSegments(polygon);
				for (LineString lineString : lineSegments) {
					List<MultiPolygon> specifiedMultiPolygon = MapUtilsExt.createSpecifiedPolygons(polygon, lineString,
							depth, width, distanceToRoad);
					multipolygons.addAll(specifiedMultiPolygon);
				}
			} else {
				multipolygons.add(JTSUtils.createMP(polygon));
			}
		}

		if (multipolygons.isEmpty())
			throw new TranslationException("getSpecifiedLand: no result");

		Random random = new Random();
		int randomPointer = random.nextInt(multipolygons.size() - 1);

		return multipolygons.get(randomPointer);
	}

	/**
	 * Gets land which is buyable, excluding land from the stakeholder and
	 * reserved land.
	 * 
	 * @param indicator
	 *            The indicator indicates if it should use the land from the
	 *            stakeholder or not, based on the action it got called from.
	 * @param stakeholderID
	 *            the stakeholder ID.
	 * @param zoneID
	 *            the zone ID.
	 * @return the buyable land.
	 */
	public static MultiPolygon getLand(final String indicator, final Integer stakeholderID, final Integer zoneID)
			throws TranslationException {

		Zone zone = EventManager.getItem(MapLink.ZONES, zoneID);
		MultiPolygon constructableLand = zone.getMultiPolygon();

		constructableLand = excludeReservedLand(constructableLand);

		List<Geometry> buyableLands = new ArrayList<>();
		for (Land land : EventManager.<Land> getItemMap(MapLink.LANDS)) {
			if (!land.getOwnerID().equals(stakeholderID) && indicator.equals("buy_land")) {
				MultiPolygon mp = JTSUtils.intersection(constructableLand, land.getMultiPolygon());
				if (JTSUtils.containsData(mp)) {
					buyableLands.add(mp);
				}
			} else if (land.getOwnerID().equals(stakeholderID) && indicator.equals("build_building")) {
				MultiPolygon mp = JTSUtils.intersection(constructableLand, land.getMultiPolygon());
				if (JTSUtils.containsData(mp)) {
					buyableLands.add(mp);
				}
			}
		}
		MultiPolygon myLandsMP = JTSUtils.createMP(buyableLands);
		return getNonEmptyLand(constructableLand, myLandsMP);
	}

	/**
	 * Excludes reserved land from the given land.
	 * 
	 * @param land
	 *            the land which should be excluded from reserved land.
	 * @return the difference from the given land and the reserved land.
	 */
	public static MultiPolygon excludeReservedLand(final MultiPolygon land) {
		MultiPolygon filteredLand = JTSUtils.EMPTY;

		Setting reservedLandSetting = EventManager.getItem(MapLink.SETTINGS, Setting.Type.RESERVED_LAND);
		MultiPolygon reservedLand = reservedLandSetting.getMultiPolygon();

		if (JTSUtils.containsData(reservedLand)) {
			filteredLand = JTSUtils.difference(land, reservedLand);
		}
		return getNonEmptyLand(land, filteredLand);
	}

	/**
	 * Excludes land which contains water from the given land.
	 * 
	 * @param land
	 *            the land which should be excluded form water.
	 * @return the difference of the given land and the water areas.
	 */
	public static MultiPolygon excludeWaterLand(final MultiPolygon land) {
		MultiPolygon filteredLand = JTSUtils.EMPTY;
		for (Terrain terrain : EventManager.<Terrain> getItemMap(MapLink.TERRAINS)) {
			if (terrain.getType().isWater())
				filteredLand = JTSUtils.difference(land, terrain.getMultiPolygon(DEFAULT_MAPTYPE));
		}
		return getNonEmptyLand(land, filteredLand);
	}

	/**
	 * excludes land with buildings on it from the given land.
	 * 
	 * @param land
	 *            the land which should be excluded from areas with buildings on
	 *            it.
	 * @return the difference of the given land and the areas containing
	 *         buildings.
	 */
	public static MultiPolygon excludeBuildingLand(final MultiPolygon land) {

		MultiPolygon filteredLand = JTSUtils.EMPTY;

		PreparedGeometry prepMyLand = PreparedGeometryFactory.prepare(land);
		for (Building building : EventManager.<Building> getItemMap(MapLink.BUILDINGS)) {
			if (prepMyLand.intersects(building.getMultiPolygon(DEFAULT_MAPTYPE))) {
				filteredLand = JTSUtils.difference(filteredLand, building.getMultiPolygon(DEFAULT_MAPTYPE));
			}
		}
		return getNonEmptyLand(land, filteredLand);
	}

	/**
	 * filters land so that it only contains areas where buildings are on it.
	 * 
	 * @param land
	 *            the land that should be filtered.
	 * @return land which only contains buildings.
	 */
	public static MultiPolygon confineBuildingLand(final MultiPolygon land) {

		MultiPolygon filteredLand = JTSUtils.EMPTY;

		PreparedGeometry prepMyLand = PreparedGeometryFactory.prepare(land);
		for (Building building : EventManager.<Building> getItemMap(MapLink.BUILDINGS)) {
			if (prepMyLand.intersects(building.getMultiPolygon(DEFAULT_MAPTYPE))) {
				filteredLand = JTSUtils.intersection(filteredLand, building.getMultiPolygon(DEFAULT_MAPTYPE));
			}
		}
		return getNonEmptyLand(land, filteredLand);
	}

	/**
	 * Creates line segments based on the given polygon.
	 * 
	 * @param polygon
	 *            the given polygon where the lines should be from.
	 * @return a list of line strings.
	 */
	public static List<LineString> getLineSegments(final Polygon polygon) throws TranslationException {
		List<LineString> lineSegments = new ArrayList<>();

		for (int i = 0; i < polygon.getExteriorRing().getNumGeometries(); i++) {
			Geometry geom = polygon.getExteriorRing().getGeometryN(i);
			if ((geom instanceof LinearRing)) {
				lineSegments.add((LineString) geom);
			}
		}
		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			LinearRing ring = (LinearRing) polygon.getInteriorRingN(i);
			for (int j = 0; j < ring.getNumGeometries(); j++) {
				Geometry geom = ring.getGeometryN(i);
				if ((geom instanceof LineString)) {
					lineSegments.add((LineString) geom);
				}
			}
		}

		if (lineSegments.isEmpty())
			throw new TranslationException("getLineSegments: no result");

		return lineSegments;
	}

	/**
	 * Creates new polygons on the edges from the given polygon, based on a
	 * given width on the lineString, and a depth into the polygon, resulting in
	 * a rectangle.
	 * 
	 * @param polygon
	 *            the polygon where the new polygon should be a subspace of.
	 * @param lineString
	 *            The line where the polygon should touch with.
	 * @param depth
	 *            the specified double depth If zero in addition to width, this
	 *            specification will be skipped.
	 * @param width
	 *            the specified double width. If zero in addition to depth, this
	 *            specification will be skipped.
	 * @param distanceToRoad
	 *            the distance the new area should be from a road. If zero, this
	 *            check will be skipped.
	 * @return
	 */
	public static List<MultiPolygon> createSpecifiedPolygons(final Polygon polygon, final LineString lineString,
			final double depth, final double width, final double distanceToRoad) throws TranslationException {

		List<MultiPolygon> specifiedPolygons = new ArrayList<MultiPolygon>();
		List<Building> buildings = new ArrayList<>(EventManager.<Building> getItemMap(MapLink.BUILDINGS).values());

		for (int c = 0; c < lineString.getCoordinates().length - 1; ++c) {

			Coordinate c1 = lineString.getCoordinates()[c];
			Coordinate c2 = lineString.getCoordinates()[c + 1];
			int sectionsOnLine = (int) Math.floor(c1.distance(c2) / width);

			if (sectionsOnLine <= 0) {
				continue;
			}

			Vector2D vec = new Vector2D(c1, c2);
			vec = vec.normalize();
			vec = vec.multiply(width);

			for (int s = 0; s < sectionsOnLine; ++s) {

				Coordinate nc1 = new Coordinate(s * vec.getX() + c1.x, s * vec.getY() + c1.y);
				Coordinate nc2 = new Coordinate((s + 1) * vec.getX() + c1.x, (s + 1) * vec.getY() + c1.y);

				LineString segment = JTSUtils.sourceFactory.createLineString(new Coordinate[] { nc1, nc2 });

				Geometry bufferedLine = JTSUtils.bufferSimple(segment, depth);

				if (distanceToRoad > 0) {

					Geometry roadQueryGeometry = JTSUtils.bufferSimple(segment, distanceToRoad);
					PreparedGeometry roadQueryPrepGeom = PreparedGeometryFactory.prepare(roadQueryGeometry);

					boolean roadsCloseby = false;
					for (Building building : buildings) {
						if (building.getCategories().contains(Category.ROAD)
								|| building.getCategories().contains(Category.INTERSECTION)) {

							if (JTSUtils.intersectsBorderIncluded(roadQueryPrepGeom,
									building.getMultiPolygon(DEFAULT_MAPTYPE))) {
								roadsCloseby = true;
								break;
							}
						}
					}
					if (!roadsCloseby) {
						continue;
					}
				}
				MultiPolygon mp = JTSUtils.intersection(polygon, bufferedLine);
				if (JTSUtils.containsData(mp)) {
					specifiedPolygons.add(mp);
				}
			}
		}

		if (specifiedPolygons.isEmpty())
			specifiedPolygons.add(JTSUtils.createMP(polygon));

		return specifiedPolygons;
	}

}
