package contextvh.translators;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Java2Parameter;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.data.engine.item.ActionLog;
import nl.tytech.data.engine.item.Indicator;
import nl.tytech.data.engine.item.Stakeholder;

/**
 * Translates {@link ActionLog} into actionlog(StakeholderID, ActionDescription,
 * ActionLogID, X). X is a list of (IndicatorID,Increase).
 *
 * @author Frank Baars
 */
public class J2ActionLog implements Java2Parameter<ActionLog> {

	/**
	 * Empty constructor.
	 */
	public J2ActionLog() {
	}

	/**
	 * Translates the actionlog into a parameter.
	 */
	@Override
	public Parameter[] translate(final ActionLog actionLog) throws TranslationException {
		ParameterList parList = new ParameterList();
		ItemMap<Indicator> map = EventManager.<Indicator>getItemMap(MapLink.INDICATORS);
		if (map != null) {
			for (Indicator indicator : map.values()) {
				Double increase = actionLog.getIncrease(indicator);
				if (increase != null && increase != 0.0) {
					parList.add(
						new ParameterList(
							new Numeral(indicator.getID()),
							new Numeral(increase)
						)
					);
				}
			}
		}

		Stakeholder stakeholder = actionLog.getStakeholder();
		return new Parameter[] {
			new Function("actionlog",
				new Numeral(stakeholder.getID()),
				new Identifier(actionLog.getAction()),
				new Numeral(actionLog.getID()),
				parList
			)
		};
	}

	/**
	 * Get the class which is translated from.
	 */
	@Override
	public Class<? extends ActionLog> translatesFrom() {
		return ActionLog.class;
	}

}
