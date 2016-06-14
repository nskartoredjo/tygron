package contextvh;

import contextvh.translators.ContextJ2Building;
import contextvh.translators.ContextJ2Stakeholder;
import contextvh.translators.ContextJ2Zone;
import contextvh.translators.J2ActionLog;
import contextvh.translators.J2Indicator;
import contextvh.translators.J2UpgradePair;
import contextvh.translators.J2UpgradeType;
import contextvh.translators.J2Land;
import contextvh.translators.J2PopupData;
import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Java2Parameter;
import eis.eis2java.translation.Parameter2Java;
import eis.eis2java.translation.Translator;
import eis.exceptions.ActException;
import eis.exceptions.NoEnvironmentException;
import eis.exceptions.PerceiveException;
import eis.iilang.Action;
import eis.iilang.EnvironmentState;
import eis.iilang.Percept;
import tygronenv.EisEnv;
import tygronenv.EntityListener;
import tygronenv.TygronEntity;
import tygronenv.translators.HashMap2J;
import tygronenv.translators.J2ActionMenu;
import tygronenv.translators.J2Answer;
import tygronenv.translators.J2Category;
import tygronenv.translators.J2ClientItemMap;
import tygronenv.translators.J2Function;
import tygronenv.translators.J2MultiPolygon;
import tygronenv.translators.J2Setting;
import tygronenv.translators.J2TimeState;
import tygronenv.translators.MultiPolygon2J;
import tygronenv.translators.ParamEnum2J;
import tygronenv.translators.Stakeholder2J;

import java.util.LinkedList;

/**
 * @author Stefan Breetveld
 */
public class ContextEnv extends EisEnv {
	private static final long serialVersionUID = 2795668071082192962L;

	private Java2Parameter<?>[] j2p = new Java2Parameter<?>[] {new J2ClientItemMap(), new ContextJ2Stakeholder(),
			new J2Setting(), new J2Function(), new J2Category(), new ContextJ2Building(), new J2TimeState(),
			new J2ActionLog(), new J2ActionMenu(), new ContextJ2Zone(), new J2Land(), new J2MultiPolygon(),
			new J2PopupData(), new J2Answer(), new J2Indicator(), new J2UpgradeType(), new J2UpgradePair()};
	private Parameter2Java<?>[] p2j = new Parameter2Java<?>[] {new ParamEnum2J(), new HashMap2J(),
			new Stakeholder2J(), new MultiPolygon2J()};

	/**
	 * General initialization: translators.
	 */
	public ContextEnv() {
		installTranslators();
	}

	@Override
	protected boolean isSupportedByEnvironment(final Action action) {
		return true;
	}

	@Override
	protected boolean isSupportedByType(final Action action, final String type) {
		return isSupportedByEnvironment(action); // ignore type.
	}

	@Override
	protected boolean isSupportedByEntity(final Action action, final String ent) {
		return getEntity(ent).isSupported(action);
	}

	/**
	 * Perform action for a given entity, possibly return a {@link Percept}.
	 *
	 * @param ent
	 *            the entity to perform this {@link Action}.
	 * @param action
	 *            the {@link Action} to perform.
	 * @return returns a {@link Percept} if the {@link Action} returns one.
	 * @throws ActException
	 *             thrown if the action failed to execute
	 */
	@Override
	protected Percept performEntityAction(final String ent, final Action action) throws ActException {
		try {
			return getEntity(ent).performAction(action);
		} catch (TranslationException | IllegalArgumentException e1) {
			throw new ActException("Failed to execute action " + action, e1);
		}
	}

	@Override
	protected LinkedList<Percept> getAllPerceptsFromEntity(final String e)
			throws PerceiveException, NoEnvironmentException {
		return super.getAllPerceptsFromEntity(e);
	}

	/**
	 * Factory method. Creates new entity. The entity should announce itself to
	 * GOAL, but only when it is ready to handle getPercepts.
	 *
	 * @param listener
	 *            Listener supplying updates
	 * @param stakeholder
	 *            name of the stakeholder
	 * @param slot
	 *            slot connection ID that this entity connects to
	 * @return new entity
	 */
	@Override
	public TygronEntity createNewEntity(
			final EntityListener listener, final String stakeholder, final Integer slot) {
		return new ContextEntity(listener, stakeholder, slot);
	}

	@Override
	public boolean isStateTransitionValid(final EnvironmentState oldState, final EnvironmentState newState) {
		return true;
	}

	/**
	 * Installs the required EIS2Java translators.
	 */
	private void installTranslators() {
		Translator translatorfactory = Translator.getInstance();

		for (Java2Parameter<?> translator : j2p) {
			translatorfactory.registerJava2ParameterTranslator(translator);
		}
		for (Parameter2Java<?> translator : p2j) {
			translatorfactory.registerParameter2JavaTranslator(translator);
		}
	}

	/**
	 * Get an entity by stakeholder name.
	 *
	 * @param stakeholder
	 *            The stakeholder.
	 * @return The entity
	 */
	public ContextEntity getEntity(final String stakeholder) {
		return (ContextEntity) super.getEntity(stakeholder);
	}
}
