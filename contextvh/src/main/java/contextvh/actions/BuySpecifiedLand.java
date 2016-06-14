package contextvh.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import contextvh.ContextEntity;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.data.engine.item.Land;
import nl.tytech.data.engine.item.Setting;
import nl.tytech.data.engine.item.Zone;
import nl.tytech.util.JTSUtils;
import nl.tytech.util.logger.TLogger;

public class BuySpecifiedLand implements CustomAction {

	
	// sell_specified_land(stackholderId, zone, min area, max area)
	@Override
	public Percept call(ContextEntity caller, LinkedList<Parameter> parameters) throws TranslationException {
		try{
			Iterator<Parameter> params = parameters.iterator();
			Number sID = ((Numeral) params.next()).getValue();
			Number zoneID = ((Numeral) params.next()).getValue();
			Number minArea = ((Numeral) params.next()).getValue();
			Number maxArea = ((Numeral) params.next()).getValue();
			
			List<Polygon> buyableLand = getBuyableLand(sID.intValue(), zoneID.intValue());
			LinkedList<Parameter> para = new LinkedList<Parameter>();
			para.add(new Numeral(sID.intValue()));
			para.add(new Identifier(buyableLand.get(0).toString()));
			para.add(new Numeral(0.0));
			return caller.performAction(new Action("map_buy_land", para));
			//return caller.performAction(action)
		} catch (Exception e) {
			TLogger.exception(e);
			throw e;
		}
	}

	@Override
	public String getName() {
		return "buy_specified_land";
	}
	
	public static List<Polygon> getBuyableLand(Integer stakeholderID, Integer zoneID) {

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
		return JTSUtils.getPolygons(myLandsMP);
	}

}
