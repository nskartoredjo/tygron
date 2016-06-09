package nl.tytech.sdk.e2eTests;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.math.Vector2D;

import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.item.Function.PlacementType;
import nl.tytech.data.engine.item.Land;
import nl.tytech.data.engine.item.Setting;
import nl.tytech.data.engine.item.Terrain;
import nl.tytech.data.engine.item.Zone;
import nl.tytech.data.engine.serializable.Category;
import nl.tytech.data.engine.serializable.MapType;
import nl.tytech.util.JTSUtils;

public class SDKTestUtil {

	/**
	 * Find land that can be built on.
	 * 
	 * @param connectionID
	 * @param mapType
	 * @param stakeholderID
	 * @param zoneID
	 * @param placementType
	 * @return
	 */
	public static List<Polygon> getBuildableLand(Integer connectionID, MapType mapType, Integer stakeholderID,
			Integer zoneID, PlacementType placementType) {
		Zone zone = EventManager.getItem(connectionID, MapLink.ZONES, zoneID);

		//
		MultiPolygon constructableLand = zone.getMultiPolygon();
		for (Terrain terrain : EventManager.<Terrain> getItemMap(connectionID, MapLink.TERRAINS)) {
			if (placementType == PlacementType.LAND && terrain.getType().isWater()) {
				constructableLand = JTSUtils.difference(constructableLand, terrain.getMultiPolygon(mapType));

			} else if (placementType == PlacementType.WATER && !terrain.getType().isWater()) {
				constructableLand = JTSUtils.difference(constructableLand, terrain.getMultiPolygon(mapType));

			}
		}

		// Reserved land is land currently awaiting land transaction
		Setting reservedLandSetting = EventManager.getItem(connectionID, MapLink.SETTINGS, Setting.Type.RESERVED_LAND);
		MultiPolygon reservedLand = reservedLandSetting.getMultiPolygon();
		if (JTSUtils.containsData(reservedLand)) {
			constructableLand = JTSUtils.difference(constructableLand, reservedLand);
		}

		List<Geometry> myLands = new ArrayList<>();
		for (Land land : EventManager.<Land> getItemMap(connectionID, MapLink.LANDS)) {
			if (land.getOwnerID().equals(stakeholderID)) {
				MultiPolygon mp = JTSUtils.intersection(constructableLand, land.getMultiPolygon());
				if (JTSUtils.containsData(mp)) {
					myLands.add(mp);
				}
			}
		}

		MultiPolygon myLandsMP = JTSUtils.createMP(myLands);
		// (Frank) For faster intersection checks, used prepared geometries.
		PreparedGeometry prepMyLand = PreparedGeometryFactory.prepare(myLandsMP);
		for (Building building : EventManager.<Building> getItemMap(connectionID, MapLink.BUILDINGS)) {
			if (prepMyLand.intersects(building.getMultiPolygon(mapType))) {
				myLandsMP = JTSUtils.difference(myLandsMP, building.getMultiPolygon(mapType));
			}
		}

		List<Polygon> buildablePolygons = JTSUtils.getPolygons(myLandsMP);
		return buildablePolygons;
	}

	/**
	 * TODO DOC what is this
	 * 
	 * @param connectionID
	 * @param mapType
	 * @param stakeholderID
	 * @param zoneID
	 * @param placementType
	 * @param width
	 * @param depth
	 * @param distanceToRoad
	 * @return list of polygons.
	 */
	public static List<MultiPolygon> createBlueprintMPs(Integer connectionID, MapType mapType, Integer stakeholderID,
			Integer zoneID, PlacementType placementType, double width, double depth, double distanceToRoad) {

		List<Polygon> polygons = getBuildableLand(connectionID, mapType, stakeholderID, zoneID, placementType);
		List<MultiPolygon> result = new ArrayList<>();

		List<Building> buildings = new ArrayList<>(
				EventManager.<Building> getItemMap(connectionID, MapLink.BUILDINGS).values());

		for (Polygon polygon : polygons) {
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

			for (LineString lineString : lineSegments) {
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
										|| building.getCategories().contains(Category.INTERSECTION)
										|| building.getCategories().contains(Category.BRIDGE)) {

									if (JTSUtils.intersectsBorderIncluded(roadQueryPrepGeom,
											building.getMultiPolygon(mapType))) {
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
							result.add(mp);
						}
					}
				}
			}

		}
		return result;
	}

	public static List<Polygon> getBuyableLand(Integer connectionID, Integer stakeholderID, Integer zoneID) {

		Zone zone = EventManager.getItem(connectionID, MapLink.ZONES, zoneID);

		//
		MultiPolygon constructableLand = zone.getMultiPolygon();

		// Reserved land is land currently awaiting land transaction
		Setting reservedLandSetting = EventManager.getItem(connectionID, MapLink.SETTINGS, Setting.Type.RESERVED_LAND);
		MultiPolygon reservedLand = reservedLandSetting.getMultiPolygon();
		if (JTSUtils.containsData(reservedLand)) {
			constructableLand = JTSUtils.difference(constructableLand, reservedLand);
		}

		List<Geometry> buyableLands = new ArrayList<>();
		for (Land land : EventManager.<Land> getItemMap(connectionID, MapLink.LANDS)) {
			if (!land.getOwnerID().equals(stakeholderID)) {
				MultiPolygon mp = JTSUtils.intersection(constructableLand, land.getMultiPolygon());
				if (JTSUtils.containsData(mp)) {
					buyableLands.add(mp);
				}
			}
		}

		MultiPolygon myLandsMP = JTSUtils.createMP(buyableLands);
		return JTSUtils.getPolygons(myLandsMP);
	}
}
