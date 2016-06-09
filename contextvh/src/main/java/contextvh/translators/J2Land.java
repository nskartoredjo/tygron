package contextvh.translators;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Java2Parameter;
import eis.eis2java.translation.Translator;
import eis.iilang.Function;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.data.engine.item.Land;
import nl.tytech.data.engine.item.Zone;

/**
 * Translate {@link Land} into land(id, ownerID, multiPolygon, zoneList, area).
 * @author W.Pasman
 *
 */
public class J2Land implements Java2Parameter<Land> {

	private final Translator translator = Translator.getInstance();

	@Override
	public Parameter[] translate(final Land b) throws TranslationException {

		ParameterList pl = new ParameterList();
		ItemMap<Zone> zones = EventManager.getItemMap(MapLink.ZONES);
		if (zones != null) {
			for (Zone zone : zones) {
				if (zone.getMultiPolygon().intersects(b.getMultiPolygon())) {
					pl.add(new Numeral(zone.getID()));
				}
			}
		}

		return new Parameter[] {
			new Function("land",
				new Numeral(b.getID()),
				translator.translate2Parameter(b.getOwnerID())[0],
				translator.translate2Parameter(b.getMultiPolygon())[0],
				pl,
				new Numeral(b.getMultiPolygon().getArea())
				)};
	}

	@Override
	public Class<? extends Land> translatesFrom() {
		return Land.class;
	}

}
