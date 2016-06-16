package contextvh.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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

import contextvh.ContextEntity;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Action;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.item.Land;
import nl.tytech.data.engine.item.Setting;
import nl.tytech.data.engine.item.Terrain;
import nl.tytech.data.engine.item.Zone;
import nl.tytech.data.engine.item.Function.PlacementType;
import nl.tytech.data.engine.serializable.Category;
import nl.tytech.data.engine.serializable.MapType;
import nl.tytech.util.JTSUtils;
import nl.tytech.util.logger.TLogger;

/**
 * Extends the internal buy_map_land action to allow the agent to specify
 * additional specifications like: width and depth for the area, the zone of the
 * area, the price per square meter, and the distance to a road. The action
 * takes care for the specified allocation of the area, delivering a
 * multipolygon for the buy_map_land action. It does return the percept from the
 * buy_map_land action.
 * 
 * Special Thanks to Frank, which wrote the code where this action is based on.
 * 
 * Land is a synonym for multi-polygon, where area is a synonym for polygon.
 * 
 * @author Nando Kartoredjo
 */
public class BuySpecifiedLand implements CustomAction {

	private static final MapType MAPTYPE = MapType.MAQUETTE;

	/**
	 * this method includes multiple procedures:
	 * 
	 * - It retrieves the values.
	 * 
	 * - within the parameter list. If there are more parameters, then it will
	 * return an exception.
	 * 
	 * - It will create a land which is buyable.
	 * 
	 * - Based on the parameter BuyOnBuilding, it allows to: exclude buildings,
	 * include buildings, or only buy areas on buildings.
	 * 
	 * - It will create a list of lands which are based on the specifications.
	 * 
	 * - It will pick randomly a land from the list of lands to call
	 * buy_map_land.
	 */
	@Override
	public Percept call(final ContextEntity caller, final LinkedList<Parameter> parameters)
			throws TranslationException {
		try {
			Iterator<Parameter> params = parameters.iterator();
			Number sID = ((Numeral) params.next()).getValue();
			Number zoneID = ((Numeral) params.next()).getValue();
			Number buyOnBuilding = ((Numeral) params.next()).getValue();
			Number price = ((Numeral) params.next()).getValue();
			Number depth = ((Numeral) params.next()).getValue();
			Number width = ((Numeral) params.next()).getValue();
			Number distanceToRoad = ((Numeral) params.next()).getValue();

			if (params.hasNext())
				throw new TranslationException("buy_specified_land: to many arguments");

			MultiPolygon buyableLand = getBuyableLand(sID.intValue(), zoneID.intValue());

			/**
			 * Checks the buyOnBuilding value if: 0: the land will be excluded
			 * form areas with buildings on it. 1: the land will contain areas
			 * where buildings are on it. 2: the land will include the areas
			 * where buildings are on it.
			 */
			if (buyOnBuilding.intValue() == 0)
				buyableLand = excludeBuildingLand(buyableLand);
			else if (buyOnBuilding.intValue() == 1)
				buyableLand = onlyBuildingLand(buyableLand);
			else if (buyOnBuilding.intValue() != 2)
				throw new TranslationException("buy_specified_land: wrong argument");

			List<Polygon> polygons = JTSUtils.getPolygons(buyableLand);
			List<MultiPolygon> multipolygons = new ArrayList<MultiPolygon>();

			for (Polygon polygon : polygons) {
				if ((depth.doubleValue() + width.doubleValue()) > 0) {
					List<LineString> lineSegments = getLineSegments(polygon);
					for (LineString lineString : lineSegments) {
						List<MultiPolygon> specifiedMultiPolygon = createSpecifiedPolygons(polygon, lineString,
								depth.doubleValue(), width.doubleValue(), distanceToRoad.doubleValue());
						multipolygons.addAll(specifiedMultiPolygon);
					}
				} else {
					multipolygons.add(JTSUtils.createMP(polygon));
				}
			}

			if (multipolygons.isEmpty())
				throw new TranslationException("buy_specified_land: no result");

			Random random = new Random();
			int randomPointer = random.nextInt(multipolygons.size() - 1);

			LinkedList<Parameter> landParams = new LinkedList<Parameter>();
			landParams.add(new Function("multipolygon", new Identifier(multipolygons.get(randomPointer).toString())));
			landParams.add(new Numeral(price));

			return caller.performAction(new Action("map_buy_land", landParams));
		} catch (Exception e) {
			TLogger.exception(e);
			throw e;
		}
	}

	@Override
	public String getName() {
		return "buy_specified_land";
	}

	/**
	 * Gets land which is buyable, excluding land from the stakeholder and
	 * reserved land.
	 * 
	 * @param stakeholderID
	 *            the stakeholder ID.
	 * @param zoneID
	 *            the zone ID.
	 * @return the buyable land.
	 */
	public static MultiPolygon getBuyableLand(final Integer stakeholderID, final Integer zoneID) {

		Zone zone = EventManager.getItem(MapLink.ZONES, zoneID);
		MultiPolygon constructableLand = zone.getMultiPolygon();

		// Reserved land is land currently awaiting land transaction
		Setting reservedLandSetting = EventManager.getItem(MapLink.SETTINGS, Setting.Type.RESERVED_LAND);
		MultiPolygon reservedLand = reservedLandSetting.getMultiPolygon();
		if (JTSUtils.containsData(reservedLand)) {
			constructableLand = JTSUtils.difference(constructableLand, reservedLand);
		}

		List<Geometry> buyableLands = new ArrayList<>();
		for (Land land : EventManager.<Land> getItemMap(MapLink.LANDS)) {
			if (!land.getOwnerID().equals(stakeholderID)) {
				MultiPolygon mp = JTSUtils.intersection(constructableLand, land.getMultiPolygon());
				if (JTSUtils.containsData(mp)) {
					buyableLands.add(mp);
				}
			}
		}

		MultiPolygon myLandsMP = JTSUtils.createMP(buyableLands);
		return myLandsMP;
	}

	/**
	 * Excludes land which contains water from the given land.
	 * 
	 * @param land
	 *            the land which should be excluded form water.
	 * @return the difference of the given land and the water areas.
	 */
	public static MultiPolygon excludeWaterLand(final MultiPolygon land) {
		MultiPolygon filteredLand = land;
		for (Terrain terrain : EventManager.<Terrain> getItemMap(MapLink.TERRAINS)) {
			if (terrain.getType().isWater())
				filteredLand = JTSUtils.difference(land, terrain.getMultiPolygon(MAPTYPE));
		}
		return filteredLand;
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

		MultiPolygon filteredLand = land;

		PreparedGeometry prepMyLand = PreparedGeometryFactory.prepare(land);
		for (Building building : EventManager.<Building> getItemMap(MapLink.BUILDINGS)) {
			if (prepMyLand.intersects(building.getMultiPolygon(MAPTYPE))) {
				filteredLand = JTSUtils.difference(filteredLand, building.getMultiPolygon(MAPTYPE));
			}
		}

		return filteredLand;
	}

	/**
	 * filters land so that it only contains areas where buildings are on it.
	 * 
	 * @param land
	 *            the land that should be filtered.
	 * @return land which only contains buildings.
	 */
	public static MultiPolygon onlyBuildingLand(final MultiPolygon land) {

		MultiPolygon filteredLand = land;

		PreparedGeometry prepMyLand = PreparedGeometryFactory.prepare(land);
		for (Building building : EventManager.<Building> getItemMap(MapLink.BUILDINGS)) {
			if (prepMyLand.intersects(building.getMultiPolygon(MAPTYPE))) {
				filteredLand = JTSUtils.intersection(filteredLand, building.getMultiPolygon(MAPTYPE));
			}
		}

		return filteredLand;
	}

	/**
	 * Creates line segments based on the given polygon.
	 * 
	 * @param polygon
	 *            the given polygon where the lines should be from.
	 * @return a list of line strings.
	 */
	public static List<LineString> getLineSegments(final Polygon polygon) {
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
			final double depth, final double width, final double distanceToRoad) {
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
									building.getMultiPolygon(MAPTYPE))) {
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
		return specifiedPolygons;
	}

}
