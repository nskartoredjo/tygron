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

public class BuildSpecifiedBuilding implements CustomAction {

	private final int LEVEL = 1;

	/**
	 * this method includes multiple procedures:
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
	public Percept call(ContextEntity caller, LinkedList<Parameter> parameters) throws TranslationException {
		try {
			Iterator<Parameter> params = parameters.iterator();
			Number sID = ((Numeral) params.next()).getValue();
			Number zoneID = ((Numeral) params.next()).getValue();
			Number BuildingID = ((Numeral) params.next()).getValue();
			Number depth = ((Numeral) params.next()).getValue();
			Number width = ((Numeral) params.next()).getValue();
			Number distanceToRoad = ((Numeral) params.next()).getValue();

			if (params.hasNext())
				throw new TranslationException("buy_specified_land: to many arguments");

			MultiPolygon buildableLand = MapUtilsExt.getLand("build_building", sID.intValue(), zoneID.intValue());

			buildableLand = MapUtilsExt.excludeBuildingLand(buildableLand);
			buildableLand = MapUtilsExt.getSpecifiedLand(buildableLand, depth.doubleValue(), width.doubleValue(),
					distanceToRoad.doubleValue());

			LinkedList<Parameter> landParams = new LinkedList<Parameter>();
			landParams.add(new Numeral(BuildingID));
			landParams.add(new Numeral(LEVEL));
			landParams.add(new Function("multipolygon", new Identifier(buildableLand.toString())));

			return caller.performAction(new Action("building_plan_construction", landParams));
		} catch (Exception e) {
			TLogger.exception(e);
			throw e;
		}
	}

	/**
	 * get name to which will be used in the GOAL agent as action
	 */
	@Override
	public String getName() {
		return "build_specified_building";
	}

}
