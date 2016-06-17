package contextvh.actions;

import java.util.Iterator;
import java.util.LinkedList;

import com.vividsolutions.jts.geom.MultiPolygon;

import contextvh.ContextEntity;
import contextvh.util.MapUtilsExt;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Action;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import nl.tytech.util.logger.TLogger;

/**
 * Extends the internal building_plan_construction action to allow the agent to
 * specify additional specifications like: width and depth for the area, the
 * zone of the area, and the distance to a road. The action takes care for the
 * specified allocation of the area, delivering a multipolygon for the
 * building_plan_construction action. It does return the percept from the
 * building_plan_construction action.
 *
 * Special Thanks to Frank, which wrote the code where this action is based on.
 *
 * Land is a synonym for multi-polygon, where area is a synonym for polygon.
 *
 * @author Nando Kartoredjo
 */
public class BuildSpecifiedBuilding implements CustomAction {

	protected static final int LEVEL = 1;

	/**
	 * This method includes multiple procedures:
	 *
	 * - It retrieves the parameter values.
	 *
	 * - If there are more parameters, then it will return an exception.
	 *
	 * - It will create a land which is buildable.
	 *
	 * - It will create a random located land which are based on the
	 * specifications.
	 */
	@Override
	public Percept call(final ContextEntity caller, final LinkedList<Parameter> parameters)
			throws TranslationException {
		try {
			Iterator<Parameter> params = parameters.iterator();
			Number sID = ((Numeral) params.next()).getValue();
			Number zoneID = ((Numeral) params.next()).getValue();
			Number buildingID = ((Numeral) params.next()).getValue();
			Number depth = ((Numeral) params.next()).getValue();
			Number width = ((Numeral) params.next()).getValue();
			Number distanceToRoad = ((Numeral) params.next()).getValue();

			if (params.hasNext()) {
				throw new TranslationException("buy_specified_land: to many arguments");
			}

			MultiPolygon buildableLand = MapUtilsExt.getLand("build_building", sID.intValue(),
					zoneID.intValue());

			buildableLand = MapUtilsExt.excludeBuildingLand(buildableLand);
			buildableLand = MapUtilsExt.getSpecifiedLand(buildableLand, depth.doubleValue(),
					width.doubleValue(), distanceToRoad.doubleValue());

			LinkedList<Parameter> landParams = new LinkedList<Parameter>();
			landParams.add(new Numeral(buildingID));
			landParams.add(new Numeral(LEVEL));
			landParams.add(new Function("multipolygon", new Identifier(buildableLand.toString())));

			return caller.performAction(new Action("building_plan_construction", landParams));
		} catch (Exception e) {
			TLogger.exception(e);
			throw e;
		}
	}

	/**
	 * Get name to which will be used in the GOAL agent as action.
	 */
	@Override
	public String getName() {
		return "build_specified_building";
	}

}
