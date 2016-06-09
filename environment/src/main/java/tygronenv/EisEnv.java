package tygronenv;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import eis.EIDefaultImpl;
import eis.eis2java.exception.NoTranslatorException;
import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Java2Parameter;
import eis.eis2java.translation.Parameter2Java;
import eis.eis2java.translation.Translator;
import eis.exceptions.ActException;
import eis.exceptions.EntityException;
import eis.exceptions.ManagementException;
import eis.exceptions.NoEnvironmentException;
import eis.exceptions.PerceiveException;
import eis.iilang.Action;
import eis.iilang.EnvironmentState;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import tygronenv.configuration.Configuration;
import tygronenv.connection.ServerConnection;
import tygronenv.translators.HashMap2J;
import tygronenv.translators.J2ActionLog;
import tygronenv.translators.J2ActionMenu;
import tygronenv.translators.J2Answer;
import tygronenv.translators.J2Building;
import tygronenv.translators.J2Category;
import tygronenv.translators.J2ClientItemMap;
import tygronenv.translators.J2Function;
import tygronenv.translators.J2Land;
import tygronenv.translators.J2MultiPolygon;
import tygronenv.translators.J2PopupData;
import tygronenv.translators.J2Setting;
import tygronenv.translators.J2Stakeholder;
import tygronenv.translators.J2TimeState;
import tygronenv.translators.J2Zone;
import tygronenv.translators.MultiPolygon2J;
import tygronenv.translators.ParamEnum2J;
import tygronenv.translators.Stakeholder2J;

/**
 * Implements the Tygron EIS adapter
 *
 * @author W.Pasman
 *
 */
@SuppressWarnings("serial")
public class EisEnv extends EIDefaultImpl implements EntityListener {

	private ServerConnection serverConnection = null;

	/**
	 * Map with {@link TygronEntity}s. The key String is equal to
	 * {@link TygronEntity#getName}.
	 */
	private Map<String, TygronEntity> entities = new HashMap<String, TygronEntity>();

	/**
	 * General initialization: translators,
	 */
	public EisEnv() {
		installTranslators();
	}

	@Override
	protected LinkedList<Percept> getAllPerceptsFromEntity(String e) throws PerceiveException, NoEnvironmentException {
		return getEntity(e).getPercepts();
	}

	protected TygronEntity getEntity(String e) {
		String entity = e.toUpperCase();
		if (!entities.containsKey(entity)) {
			throw new IllegalArgumentException("Unknown entity " + entity + ". Have:" + entities.keySet());
		}
		return entities.get(entity);
	}

	@Override
	protected boolean isSupportedByEnvironment(Action action) {
		return true;
	}

	@Override
	protected boolean isSupportedByType(Action action, String type) {
		return isSupportedByEnvironment(action); // ignore type.
	}

	@Override
	protected boolean isSupportedByEntity(Action action, String entity) {
		return getEntity(entity).isSupported(action);
	}

	@Override
	protected Percept performEntityAction(String e, Action action) throws ActException {
		try {
			return getEntity(e).performAction(action);
		} catch (TranslationException | IllegalArgumentException e1) {
			throw new ActException("Failed to execute action " + action, e1);
		}
	}

	@Override
	public void init(Map<String, Parameter> parameters) throws ManagementException {
		super.init(parameters);
		Configuration config;
		try {

			config = createConfiguration(parameters);
		} catch (TranslationException e) {
			throw new ManagementException("problem with the init settings", e);
		}
		serverConnection = createServerConnection(config);
		setState(EnvironmentState.RUNNING);

		for (String st : config.getStakeholders()) {
			String stakeholder = st.toUpperCase();
			TygronEntity entity = createNewEntity(this, stakeholder, serverConnection.getSession().getTeamSlot());
			// These will report themselves to EIS when they are ready.
			entities.put(stakeholder, entity);
		}

	}

	/**
	 * Factory method to create {@link ServerConnection}.
	 * 
	 * @param config
	 *            the configuration to use
	 * @return new {@link ServerConnection} that respects given configuration
	 * @throws ManagementException
	 */
	public ServerConnection createServerConnection(Configuration config) throws ManagementException {
		return new ServerConnection(config);
	}

	/**
	 * Factory method to create {@link Configuration}
	 * 
	 * @param parameters
	 *            map with the init parameters
	 * @return new {@link Configuration} that respects given parameters
	 * @throws TranslationException
	 * @throws NoTranslatorException
	 */
	public Configuration createConfiguration(Map<String, Parameter> parameters)
			throws NoTranslatorException, TranslationException {
		return new Configuration(parameters);
	}

	/**
	 * Factory method. Creates new entity. The entity should announce itself to
	 * GOAL, but only when it is ready to handle getPercepts.
	 * 
	 * @return new entity
	 */
	public TygronEntity createNewEntity(EntityListener listener, String stakeholder, Integer slot) {
		return new TygronEntityImpl(listener, stakeholder, slot);
	}

	@Override
	public void kill() throws ManagementException {
		super.kill();
		for (TygronEntity entity : entities.values()) {
			entity.close();
		}
		entities = new HashMap<>();
		if (serverConnection != null) {
			serverConnection.disconnect();
			serverConnection = null;
		}
	};

	// FIXME reset #3844

	@Override
	public boolean isStateTransitionValid(EnvironmentState oldState, EnvironmentState newState) {
		return true;
	}

	/**
	 * Entity with given name is ready for use. Report to EIS
	 *
	 * @param entity
	 *            the identifier of the entity
	 * @throws EntityException
	 */
	@Override
	public void entityReady(String entity) {
		try {
			addEntity(entity, "stakeholder");
		} catch (EntityException e) {
			e.printStackTrace();
		}
	}

	/************************* SUPPORT FUNCTIONS ****************************/

	Java2Parameter<?>[] j2p = new Java2Parameter<?>[] { new J2ClientItemMap(), new J2Stakeholder(), new J2Setting(),
			new J2Function(), new J2Category(), new J2Building(), new J2TimeState(), new J2ActionLog(),
			new J2ActionMenu(), new J2Zone(), new J2Land(), new J2MultiPolygon(), new J2PopupData(), new J2Answer() };
	Parameter2Java<?>[] p2j = new Parameter2Java<?>[] { new ParamEnum2J(), new HashMap2J(), new Stakeholder2J(),
			new MultiPolygon2J() };

	/**
	 * Installs the required EIS2Java translators
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

}
