package contextvh.util;

import java.util.Arrays;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.item.Land;
import nl.tytech.data.engine.item.Setting;
import nl.tytech.data.engine.item.Terrain;
import nl.tytech.data.engine.item.Zone;
import nl.tytech.data.engine.serializable.MapType;
import nl.tytech.util.JTSUtils;

/**
 * Utility class for performing often used operations on map elements.
 *
 * @author Max Groenenboom
 */
public final class MapUtils {

	/**
	 * Private constructor.
	 */
	private MapUtils() {
	}

	protected static final MapType DEFAULT_MAPTYPE = MapType.MAQUETTE;

	/**
	 * Returns a Geometry of all zones with its ID in ids combined, if an empty
	 * list or no ids are given, a Geometry containing all zones will be
	 * returned.
	 *
	 * @param connectionID
	 *            The id of the connection.
	 * @param ids
	 *            The list of zone ID's. If the list is empty, all zones will be
	 *            used.
	 * @return The resulting Geometry.
	 */
	public static Geometry getZonesCombined(final Integer connectionID, final List<Integer> ids) {
		final boolean getAll = ids.size() == 0;
		final ItemMap<Zone> zones = EventManager.getItemMap(connectionID, MapLink.ZONES);
		Geometry result = JTSUtils.EMPTY;
		for (Zone zone : zones) {
			if (getAll || ids.contains(zone.getID())) {
				result = result.union(zone.getMultiPolygon());
			}
		}
		return result;
	}

	/**
	 * Returns a Geometry of all zones with its ID in ids combined, if an empty
	 * list or no ids are given, a Geometry containing all zones will be
	 * returned.
	 *
	 * @param connectionID
	 *            The id of the connection.
	 * @param ids
	 *            The list of zone ID's. If the list is empty, all zones will be
	 *            used.
	 * @return The resulting Geometry.
	 */
	public static Geometry getZonesCombined(final Integer connectionID, final Integer... ids) {
		List<Integer> zoneIDs = Arrays.asList(ids);
		return getZonesCombined(connectionID, zoneIDs);
	}

	/**
	 * Returns a Geometry of all land of the stakeholder with ID stakeholderID.
	 *
	 * @param connectionID
	 *            The id of the connection.
	 * @param stakeholderID
	 *            The ID of the stakeholder.
	 * @return The resulting Geometry.
	 */
	public static MultiPolygon getStakeholderLands(final Integer connectionID, final Integer stakeholderID) {
		MultiPolygon result = JTSUtils.EMPTY;
		final ItemMap<Land> lands = EventManager.getItemMap(connectionID, MapLink.LANDS);
		for (Land land : lands) {
			if (land.getOwnerID().equals(stakeholderID)) {
				result = JTSUtils.union(result, land.getMultiPolygon());
			}
		}
		return result;
	}

	/**
	 * Removes all water from the multiPolygon mp.
	 *
	 * @param connectionID
	 *            The id of the connection.
	 * @param mp
	 *            The MultiPolygon to perform the operation on.
	 * @return The MultiPolygon without all water.
	 */
	public static MultiPolygon removeWater(final Integer connectionID, final MultiPolygon mp) {
		return removeWaterOrLand(connectionID, mp, true);
	}

	/**
	 * Removes all land from the multiPolygon mp.
	 *
	 * @param mp
	 *            The MultiPolygon to perform the operation on.
	 * @param connectionID
	 *            The id of the connection.
	 * @return The MultiPolygon without all land.
	 */
	public static MultiPolygon removeLand(final Integer connectionID, final MultiPolygon mp) {
		return removeWaterOrLand(connectionID, mp, false);
	}

	/**
	 * Removes all water or land from the multiPolygon mp.
	 *
	 * @param connectionID
	 *            The id of the connection.
	 * @param mp
	 *            The MultiPolygon to perform the operation on.
	 * @param removeWater
	 *            Toggles whether water or land will be removed.
	 * @return The MultiPolygon without all water or land.
	 */
	private static MultiPolygon removeWaterOrLand(final Integer connectionID, final MultiPolygon mp,
			final boolean removeWater) {
		final ItemMap<Terrain> terrains = EventManager.getItemMap(connectionID, MapLink.TERRAINS);
		MultiPolygon mpResult = mp;
		for (Terrain terrain : terrains) {
			if (terrain.getType().isWater() == removeWater) {
				mpResult = JTSUtils.difference(mpResult, terrain.getMultiPolygon(DEFAULT_MAPTYPE));
			}
		}
		return mpResult;
	}

	/**
	 * Removes all reserved land from the MultiPolygon mp.
	 *
	 * @param connectionID
	 *            The id of the connection.
	 * @param mp
	 *            The MultiPolygon to perform the operation on.
	 * @return The MultiPolygon without all reserved land.
	 */
	public static MultiPolygon removeReservedLand(final Integer connectionID, final MultiPolygon mp) {
		final Setting reservedLandSetting = EventManager.getItem(connectionID, MapLink.SETTINGS,
				Setting.Type.RESERVED_LAND);
		MultiPolygon reservedLand = reservedLandSetting.getMultiPolygon();
		MultiPolygon mpResult = mp;
		if (JTSUtils.containsData(reservedLand)) {
			mpResult = JTSUtils.difference(mpResult, reservedLand);
		}
		return mpResult;
	}

	/**
	 * Remove all buildings' areas from a given multipolygon.
	 *
	 * @param connectionID
	 *            The id of the connection.
	 * @param stakeHolderID
	 *            The id of the stakeholder whose buildings' areas have to be
	 *            removed. Removes all buildings' areas if it is null.
	 * @param mp
	 *            The multipolygon where buildings' areas need to be removed.
	 * @return A multipolygon with all buildings' areas removed.
	 */
	public static MultiPolygon removeBuildings(final Integer connectionID, final Integer stakeHolderID,
			final MultiPolygon mp) {
		final ItemMap<Building> buildings = EventManager.getItemMap(connectionID, MapLink.BUILDINGS);
		MultiPolygon constructableLand = mp;
		final PreparedGeometry prepped = PreparedGeometryFactory.prepare(constructableLand);
		for (Building building : buildings) {
			final MultiPolygon buildingMP = building.getMultiPolygon(DEFAULT_MAPTYPE);
			// Only remove areas of buildings that are owned by the given
			// stakeholder, or all if it is null.
			if ((stakeHolderID == null || building.getOwner().getID().equals(stakeHolderID))
					&& prepped.intersects(buildingMP)) {
				constructableLand = JTSUtils.difference(constructableLand, buildingMP);
			}
		}
		return constructableLand;
	}
}
