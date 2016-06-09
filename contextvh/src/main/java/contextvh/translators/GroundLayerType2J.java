package contextvh.translators;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Parameter2Java;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import nl.tytech.data.engine.item.Building.GroundLayerType;

/**
 * Translates a GroundLayerType argument to a java.
 * @author Rico
 *
 */
public class GroundLayerType2J implements Parameter2Java<GroundLayerType> {

	private static final String SURFACE_IDENTIFIER = "SURFACE";
	private static final String UNDERGROUND_IDENTIFIER = "UNDERGROUND";
	@Override

	public GroundLayerType translate(final Parameter parameter) throws TranslationException {
		String name =  ((Identifier) parameter).getValue();
		name = name.toUpperCase();

		if (name.equals(SURFACE_IDENTIFIER)) {
			return GroundLayerType.SURFACE;
		} else if (name.equals(UNDERGROUND_IDENTIFIER)) {
			return GroundLayerType.UNDERGROUND;
		}
		throw new TranslationException("Undefined identifier found " + name
				+ ", should be " + SURFACE_IDENTIFIER
				+ " or " + UNDERGROUND_IDENTIFIER);
	}

	@Override
	public Class<GroundLayerType> translatesTo() {
		return GroundLayerType.class;
	}
}
