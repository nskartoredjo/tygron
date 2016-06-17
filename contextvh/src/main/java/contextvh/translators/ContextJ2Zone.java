package contextvh.translators;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Translator;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import nl.tytech.data.engine.item.Zone;

/**
 * Translate {@link Zone} into zones(ID, name, maxFloors, size, [categories],
 * [FunctionIDs]).
 *
 * @author T.Brunner, J.N de Vries
 */
public class ContextJ2Zone extends tygronenv.translators.J2Zone {

	private final Translator translator = Translator.getInstance();

	/**
	 * @return a parameter for the zone, containing zone id, zone name, maximum
	 *         allowed floor size, the total size of the zone, the allowed
	 *         categories and the allowed function IDs within the zone.
	 */
	@Override
	public Parameter[] translate(final Zone z) throws TranslationException {
		return new Parameter[] {
			new Function("zone",
				new Numeral(z.getID()),
				new Identifier(z.getName()),
				new Numeral(z.getMaxAllowedFloors()),
				new Numeral(z.getMultiPolygon().getArea()),
				translator.translate2Parameter(z.getAllowedCategories())[0]
			)
		};
	}
}
