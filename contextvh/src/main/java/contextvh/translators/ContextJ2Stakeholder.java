package contextvh.translators;

import eis.eis2java.exception.TranslationException;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import nl.tytech.data.engine.item.Indicator;
import nl.tytech.data.engine.item.Stakeholder;

import java.util.List;

/**
 * Translator class from Stakeholder class into IndicatorLink and stakeholder
 * percept. IndicatorLink(<stakeholderID>, [(<indicatorID>, <indicatorName>,
 * <indicatorWeight>), ..]) stakeholder(<ID>, <Name>, <StartBudget>,
 * <YearlyIncome>)
 *
 * @author Haoming - Danshal & Rico - WhySoSerious
 */
public class ContextJ2Stakeholder extends tygronenv.translators.J2Stakeholder {

	/**
	 * Translate the stakeholder object in the form of: stakeholder/4 -
	 * stakeholders([<ID>, <Name>, <Budget>, <Income>]) and indicatorlink/2
	 * indicatorLink([<ID>,[indicatorWeights(<IndID>,<IndName>,<IndWeight>]]).
	 */
	@Override
	public Parameter[] translate(final Stakeholder stakeholder) throws TranslationException {
		Double budget = stakeholder.getBudget();
		if (budget == null) {
			budget = 0d;
		}
		return new Parameter[] {
			new Function("stakeholder",
				new Numeral(stakeholder.getID()),
				new Identifier(stakeholder.getName()),
				new Numeral(budget),
				new Numeral(stakeholder.getYearlyIncome())
			), new Function("indicatorLink",
				new Numeral(stakeholder.getID()),
				indicator(stakeholder.getMyIndicators(), stakeholder)
			)
		};
	}

	/**
	 * Method for creating the tupled list of indicators and weights.
	 *
	 * @param indicatorList
	 *            List of indicators of the stakeholder.
	 * @param stakeholder
	 *            Stakeholder that has these indicators.
	 * @return ParameterList with indicatorWeights.
	 */
	public ParameterList indicator(final List<Indicator> indicatorList, final Stakeholder stakeholder) {
		ParameterList pList = new ParameterList();
		for (Indicator ind : indicatorList) {
			pList.add(
				new Function("indicatorWeights",
					new Numeral(ind.getID()),
					new Identifier(ind.getName()),
					new Numeral(stakeholder.getCurrentIndicatorWeight(ind))
				)
			);
		}
		return pList;
	}

}
