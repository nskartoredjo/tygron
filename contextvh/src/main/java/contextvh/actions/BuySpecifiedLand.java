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

public class BuySpecifiedLand implements CustomAction {

	private static final MapType MAPTYPE = MapType.MAQUETTE;

	@Override
	public Percept call(ContextEntity caller, LinkedList<Parameter> parameters) throws TranslationException {
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

			if (buyOnBuilding.intValue() == 0)
				buyableLand = excludeBuildingLand(buyableLand);
			else if (buyOnBuilding.intValue() == 1)
				buyableLand = onlyBuildingLand(buyableLand);
			else if (buyOnBuilding.intValue() != 2)
				throw new TranslationException("buy_specified_land: wrong argument");
						
			List<Polygon> polygons = JTSUtils.getPolygons(buyableLand);
			List<MultiPolygon> multipolygons = new ArrayList<MultiPolygon>();
			
			for (Polygon polygon : polygons) {
				if((depth.doubleValue() + width.doubleValue()) > 0) {
					List<LineString> lineSegments = getLineSegments(polygon);
					for (LineString lineString : lineSegments) {
						List<MultiPolygon> specifiedMultiPolygon = createSpecifiedPolygons(polygon, lineString, depth.doubleValue(),
								width.doubleValue(), distanceToRoad.doubleValue());
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

	public static MultiPolygon getBuyableLand(Integer stakeholderID, Integer zoneID) {

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
	
	public static MultiPolygon excludeWaterLand(final MultiPolygon buyableLand) {
		MultiPolygon filteredLand = buyableLand;
		for (Terrain terrain : EventManager.<Terrain> getItemMap(MapLink.TERRAINS)) {
			if (terrain.getType().isWater())
				filteredLand = JTSUtils.difference(buyableLand, terrain.getMultiPolygon(MAPTYPE));
		}
		return filteredLand;
	}

	public static MultiPolygon excludeBuildingLand(final MultiPolygon buyableLand) {

		MultiPolygon filteredLand = buyableLand;

		PreparedGeometry prepMyLand = PreparedGeometryFactory.prepare(buyableLand);
		for (Building building : EventManager.<Building> getItemMap(MapLink.BUILDINGS)) {
			if (prepMyLand.intersects(building.getMultiPolygon(MAPTYPE))) {
				filteredLand = JTSUtils.difference(filteredLand, building.getMultiPolygon(MAPTYPE));
			}
		}

		return filteredLand;
	}

	public static MultiPolygon onlyBuildingLand(final MultiPolygon buyableLand) {

		MultiPolygon filteredLand = buyableLand;

		PreparedGeometry prepMyLand = PreparedGeometryFactory.prepare(buyableLand);
		for (Building building : EventManager.<Building> getItemMap(MapLink.BUILDINGS)) {
			if (prepMyLand.intersects(building.getMultiPolygon(MAPTYPE))) {
				filteredLand = JTSUtils.intersection(filteredLand, building.getMultiPolygon(MAPTYPE));
			}
		}

		return filteredLand;
	}

	public static List<LineString> getLineSegments(Polygon polygon) {
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

	public static List<MultiPolygon> createSpecifiedPolygons(Polygon polygon, LineString lineString, double depth,
			double width, double distanceToRoad) {
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
