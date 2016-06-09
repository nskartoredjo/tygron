package tygronenv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eis.EnvironmentListener;
import eis.exceptions.ActException;
import eis.exceptions.AgentException;
import eis.exceptions.ManagementException;
import eis.exceptions.NoEnvironmentException;
import eis.exceptions.PerceiveException;
import eis.exceptions.RelationException;
import eis.iilang.Action;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import nl.tytech.data.engine.event.ParticipantEventType;

public class TestEnvironmentStates {

	private static final String STAKEHOLDERS = "stakeholders";
	private static final String MUNICIPALITY = "MUNICIPALITY";
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
	}

	@Test
	public void testEntityAppears()
			throws ManagementException, RelationException, AgentException, InterruptedException {

		EnvironmentListener envlistener = mock(EnvironmentListener.class);

		env.attachEnvironmentListener(envlistener);
		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		parameters.put(STAKEHOLDERS, new ParameterList(new Identifier(MUNICIPALITY)));
		// any slot so not specified.
		env.init(parameters);
		Thread.sleep(5000); // give system sufficient time to create the entity.

		// after the init, a new entity should appear that we can connect to.
		verify(envlistener).handleNewEntity(MUNICIPALITY);

	}

	@Test
	public void testStateChange() throws ManagementException, RelationException, AgentException, InterruptedException,
			PerceiveException, NoEnvironmentException {

		joinAsMunicipality();

		LinkedList<Percept> percepts = env.getAllPerceptsFromEntity(MUNICIPALITY);
		Percept expectedPercept = new Percept("stakeholders",
				new ParameterList(new ParameterList(new Identifier("Municipality"), new Numeral(0), new Numeral(0d)),
						new ParameterList(new Identifier("Inhabitants"), new Numeral(1), new Numeral(0d))));
		assertTrue(percepts.contains(expectedPercept));

	}

	@Test
	public void testFunctionPercept() throws ManagementException, RelationException, AgentException,
			InterruptedException, PerceiveException, NoEnvironmentException {

		joinAsMunicipality();

		findRoadFunction();

	}

	@Test
	public void testBuildRoad() throws ManagementException, RelationException, AgentException, InterruptedException,
			PerceiveException, NoEnvironmentException, ActException {

		joinAsMunicipality();

		// should give us [Cemetery,17,[[OTHER]]],[Highway A13,33,[ROAD]]
		// where arg 1 is the function ID.
		ParameterList buildroadfunction = findRoadFunction();
		assertNotNull("There is no road function in the provided functions list", buildroadfunction);

		/**
		 * Check the javadoc for the tygron SDK for ParticipantEventType. You
		 * will see
		 *
		 * params = { "Stakeholder ID", "Function ID", "Amount of floors",
		 * "MultiPolygon describing the build contour" })
		 *
		 * Leave out the Stakeholder.
		 */
		Action action = new Action(ParticipantEventType.BUILDING_PLAN_CONSTRUCTION.name().toLowerCase(),
				buildroadfunction.get(1), new Numeral(1),
				// square(10,10,200,10)
				new Function("multipolygon", new Identifier("MULTIPOLYGON (((20 10, 20 40, 220 40, 220 10, 20 10)))")));

		env.performEntityAction(MUNICIPALITY, action);

	}

	/**
	 * We use a circle instead of a proper object for a square in the action.
	 */
	@Test(expected = ActException.class)
	public void testBuildRoadWrongArgs() throws ManagementException, RelationException, AgentException,
			InterruptedException, PerceiveException, NoEnvironmentException, ActException {

		joinAsMunicipality();

		// should give us [Cemetery,17,[[OTHER]]],[Highway A13,33,[ROAD]]
		// where arg 1 is the function ID.
		ParameterList buildroadfunction = findRoadFunction();
		assertNotNull("There is no road function in the provided functions list", buildroadfunction);

		/**
		 * Check the javadoc for the tygron SDK for ParticipantEventType. You
		 * will see
		 *
		 * params = { "Stakeholder ID", "Function ID", "Amount of floors",
		 * "MultiPolygon describing the build contour" })
		 *
		 * Leave out the Stakeholder.
		 */
		Action action = new Action(ParticipantEventType.BUILDING_PLAN_CONSTRUCTION.name().toLowerCase(),
				buildroadfunction.get(1), new Numeral(1), new Identifier("circle"));

		env.performEntityAction(MUNICIPALITY, action);

	}

	/********************** UTIL FUNCTIONS **************************/
	/**
	 * Init env and ask for municipality as stakeholder.
	 *
	 * @throws ManagementException
	 * @throws InterruptedException
	 */
	protected void joinAsMunicipality() throws ManagementException, InterruptedException {
		MyEnvListener listener = new MyEnvListener();
		env.attachEnvironmentListener(listener);

		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		parameters.put(STAKEHOLDERS, new ParameterList(new Identifier(MUNICIPALITY)));
		// any slot so not specified.
		env.init(parameters);

		assertEquals("MUNICIPALITY", listener.waitForEntity());
	}

	/**
	 * Search for a road function in the percepts. This runs through all
	 * elements of the function and checks their type
	 *
	 * @return a function in the percepts that is for building roads.
	 *
	 * @throws PerceiveException
	 */
	private ParameterList findRoadFunction() throws PerceiveException {
		LinkedList<Percept> percepts = env.getAllPerceptsFromEntity(MUNICIPALITY);

		// find the FUNCTIONS percept
		Percept function = null;
		for (Percept percept : percepts) {
			if (percept.getName().equals("functions")) {
				function = percept;
			}
		}
		assertNotNull("no FUNCTIONS percept found", function);
		assertEquals(1, function.getParameters().size());

		/**
		 * By checking J2BaseFunction you can see that
		 *
		 * we should have received something like <code>
		 * functions([['Vacant Lot',0,[OTHER]],['Mid-Century affordable
		 * housing',1,[SOCIAL]],...])	 </code>
		 */
		ParameterList roadfunction = null;
		Parameter functions = function.getParameters().get(0);
		assertEquals(ParameterList.class, functions.getClass());
		for (Parameter f : (ParameterList) functions) {
			// f is something like ['Vacant Lot',0,[OTHER]]
			assertEquals(ParameterList.class, f.getClass());
			ParameterList flist = (ParameterList) f;
			assertEquals(Identifier.class, flist.get(0).getClass());
			assertEquals(Numeral.class, flist.get(1).getClass());
			assertEquals(ParameterList.class, flist.get(2).getClass());

			// check the Categories part in detail.
			Parameter categories = ((ParameterList) f).get(2);
			assertEquals(ParameterList.class, categories.getClass());

			// Check that all contents of the category are Identifiers.
			for (Parameter category : (ParameterList) categories) {
				assertEquals(Identifier.class, category.getClass());

				// while we're at it, check if we can find a road build func
				if (roadfunction == null && "ROAD".equals(((Identifier) category).getValue())) {
					roadfunction = flist;
				}

			}
		}

		return roadfunction;
	}

}
