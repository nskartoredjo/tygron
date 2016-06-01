package tygronenv.translators;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Java2Parameter;
import eis.eis2java.translation.Translator;
import eis.iilang.Function;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import nl.tytech.data.engine.serializable.UpgradePair;

/**
 * Translate {@link UpgradePair} into {@code upgrade_pair(<SourceFunctionID>, <TargetFunctionID>)}.
 *
 * @author Stefan Breetveld
 */
public class J2UpgradePair implements Java2Parameter<UpgradePair> {

    private final Translator translator = Translator.getInstance();

    /**
     * Translates the UpgradeType object in the form of:
     * {@code upgrade_type(<ID>, <SourceFunctionID>, <TargetFunctionID>)} or
     * {@code upgrade_type(<ID>)} if there is no pair available.
     */
    @Override
    public Parameter[] translate(final UpgradePair u) throws TranslationException {
        return new Parameter[]{
                new Function(
                        "upgrade_pair",
                        new Numeral(u.getSourceFunctionID()),
                        new Numeral(u.getTargetFunctionID())
                )
        };
    }

    /**
     * This class translates all objects that extend {@link UpgradePair}.
     * @return {@code UpgradePair.class}
     */
    @Override
    public Class<? extends UpgradePair> translatesFrom() {
        return UpgradePair.class;
    }

}
