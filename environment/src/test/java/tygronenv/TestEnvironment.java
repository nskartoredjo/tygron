package tygronenv;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eis.EnvironmentListener;
import eis.exceptions.ManagementException;
import eis.exceptions.NoEnvironmentException;
import eis.exceptions.PerceiveException;
import eis.iilang.Action;
import eis.iilang.EnvironmentState;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;

public class TestEnvironment {

	private static final String MUNICIPALITY = "MUNICIPALITY";
	private static final String STAKEHOLDERS = "stakeholders";
	private EisEnv env;
	private static String PROJECT = "project";
	private static Identifier PROJECTNAME = new Identifier("testmap");

	@Before
	public void before() {
		env = createEnvironment();
	}

	/**
	 * Factory method that delivers the environment under test
	 * 
	 * @return new {@link EisEnv} for testing
	 */
	public EisEnv createEnvironment() {
		return new EisEnv();
	}

	@After
	public void after() throws ManagementException, InterruptedException {
		env.kill();
		env = null;
	}

	@Test
	public void testBasicConnect() throws ManagementException {
		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		// any stakeholder so not specified.
		// any slot so not specified.
		env.init(parameters);
	}

	@Test
	public void testConnectAndKill() throws ManagementException {
		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		// any stakeholder so not specified.
		// any slot so not specified.
		env.init(parameters);
	}

	@Test
	public void testGetStakeHolder() throws ManagementException {
		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		// parameters.put(STAKEHOLDER, new Identifier("MUNICIPALITY"));
		parameters.put(STAKEHOLDERS, new ParameterList(new Identifier(MUNICIPALITY)));
		// any slot so not specified.
		env.init(parameters);
	}

	@Test(expected = ManagementException.class)
	public void testGetWrongStakeHolder() throws ManagementException {
		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		parameters.put(STAKEHOLDERS, new Identifier("BADSTAKEHOLDER"));
		// any slot so not specified.
		env.init(parameters);
	}

	enum StateType {
		WAITING, DONE, ERROR
	};

	class State {
		private StateType type = StateType.WAITING;
		private Exception exc;

		public void setState(StateType t, Exception e) {
			exc = e;
			type = t;
		}

		public void waitTillDone() throws Exception {
			while (type == StateType.WAITING) {
				Thread.sleep(100);
			}
			if (type == StateType.ERROR) {
				throw exc;
			}
		}
	}

	@Test
	public void testEntityReady() throws Exception {
		connectAndInit();
	}

	class myListener implements EnvironmentListener {
		List<Percept> initPercepts = null;
		private State state;

		public myListener(State state) {
			this.state = state;
		}

		@Override
		public void handleStateChange(EnvironmentState newState) {
		}

		@Override
		public void handleNewEntity(String entity) {
			try {
				initPercepts = env.getAllPerceptsFromEntity(entity);
				state.setState(StateType.DONE, null);
			} catch (PerceiveException | NoEnvironmentException e) {
				state.setState(StateType.ERROR, e);
			}
		}

		@Override
		public void handleFreeEntity(String entity, Collection<String> agents) {
		}

		@Override
		public void handleDeletedEntity(String entity, Collection<String> agents) {
		}

		public List<Percept> getInitPercepts() {
			return initPercepts;
		}

	}

	/**
	 * init and get init percepts
	 * 
	 * @return
	 * @throws Exception
	 */
	private List<Percept> connectAndInit() throws Exception {
		State state = new State();

		myListener listener = new myListener(state);
		env.attachEnvironmentListener(listener);

		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		parameters.put(STAKEHOLDERS, new ParameterList(new Identifier(MUNICIPALITY)));
		env.init(parameters);

		state.waitTillDone();
		return listener.getInitPercepts();

	}

	@Test
	public void testSellLand() throws Exception {
		List<Percept> percepts = connectAndInit();
		assertNotNull("no initial percepts!", percepts);

		// search some land that we own
		Parameter landlist = null;
		for (Percept p : percepts) {
			if (!p.getName().equals("lands"))
				continue;
			landlist = p.getParameters().get(0);
		}

		assertNotNull("no lands in percepts!", landlist);
		assertTrue(landlist instanceof ParameterList);
		Parameter land = ((ParameterList) landlist).get(0);

		assertTrue("land is not a function", land instanceof Function);
		Parameter polygon = ((Function) land).getParameters().get(2);

		Action action = new Action("map_sell_land", new Numeral(1), polygon, new Numeral(400.0));
		env.performEntityAction(MUNICIPALITY, action);

		// wait for new percepts related to the sell action
		Thread.sleep(3000);
		while (env.getAllPerceptsFromEntity(MUNICIPALITY).isEmpty()) {
			Thread.sleep(100);
		}

	}

}
