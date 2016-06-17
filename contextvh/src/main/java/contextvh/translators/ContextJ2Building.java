package contextvh.translators;

import com.vividsolutions.jts.geom.MultiPolygon;
import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Translator;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.serializable.MapType;

/**
 * Translate {@link Building} into building(ID, name, ownerID, constructionYear,
 * [categories], FunctionID, numFloors).
 *
 * @author W.Pasman
 */
public class ContextJ2Building extends tygronenv.translators.J2Building {

	private final Translator translator = Translator.getInstance();

	@Override
	public Parameter[] translate(final Building building) throws TranslationException {
		final MultiPolygon multiPolygon = building.getMultiPolygon(MapType.MAQUETTE);
		return new Parameter[] {
			new Function("building",
				new Numeral(building.getID()),
				new Identifier(building.getName()),
				new Numeral(building.getOwnerID()),
				new Numeral(building.getConstructionYear()),
				translator.translate2Parameter(building.getCategories())[0],
				new Numeral(building.getFunctionID()),
				new Numeral(building.getFloors()),
				translator.translate2Parameter(multiPolygon)[0],
				new Numeral(multiPolygon.getArea())
			)
		};
	}

}
