package contextvh.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import contextvh.ContextEntity;
import contextvh.util.MapUtilsExt;

import eis.eis2java.exception.TranslationException;

import eis.iilang.Action;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.Percept;

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

			MultiPolygon buyableLand = MapUtilsExt.getLand("buy_land", sID.intValue(), zoneID.intValue());

			/**
			 * Checks the buyOnBuilding value if: 0: the land will be excluded
			 * form areas with buildings on it. 1: the land will contain areas
			 * where buildings are on it. 2: the land will include the areas
			 * where buildings are on it.
			 */
			if (buyOnBuilding.intValue() == 0)
				buyableLand = MapUtilsExt.excludeBuildingLand(buyableLand);
			else if (buyOnBuilding.intValue() == 1)
				buyableLand = MapUtilsExt.confineBuildingLand(buyableLand);
			else if (buyOnBuilding.intValue() != 2)
				throw new TranslationException("buy_specified_land: wrong argument");

			buyableLand = MapUtilsExt.getSpecifiedLand(buyableLand, depth.doubleValue(), width.doubleValue(),
					distanceToRoad.doubleValue());

			LinkedList<Parameter> landParams = new LinkedList<Parameter>();
			landParams.add(new Function("multipolygon", new Identifier(buyableLand.toString())));
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

}
