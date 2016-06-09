
package contextvh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import eis.exceptions.AgentException;
import eis.exceptions.ManagementException;
import eis.exceptions.NoEnvironmentException;
import eis.exceptions.PerceiveException;
import eis.exceptions.RelationException;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import tygronenv.EisEnv;
import tygronenv.MyEnvListener;

public class TestEnvironmentStates extends tygronenv.TestEnvironmentStates{

	private static final String STAKEHOLDERS = "stakeholders";
	private static final String MUNICIPALITY = "MUNICIPALITY";
	private static final String INHABITANTS = "INHABITANTS";
	private ContextEnv env;
	private static String PROJECT = "project";
	private static Identifier PROJECTNAME = new Identifier("testmap");

	/**
	 * Factory method that delivers the environment under test
	 *
	 * @return new {@link EisEnv} for testing
	 */
    @Override
	public EisEnv createEnvironment() {
        env = new ContextEnv();
		return env;
	}

	@After
	public void after() throws ManagementException, InterruptedException {
        super.after();
		env.kill();
        env = null;
	}

	@Test
    @Override
	public void testStateChange() throws ManagementException, RelationException, AgentException, InterruptedException,
			PerceiveException, NoEnvironmentException {

		joinAsMunicipality();

		Deque<Percept> percepts = env.getAllPerceptsFromEntity(MUNICIPALITY);
		Percept expectedPercept = new Percept("stakeholders", new ParameterList( new ParameterList(
				new Function("stakeholder", new Numeral(0), new Identifier("Municipality"),
						new Numeral(0.0), new Numeral(0)), new Function("indicatorLink", new Numeral(0), 
						        new ParameterList())),
				new ParameterList(new Function("stakeholder", new Numeral(1), new Identifier("Inhabitants"),
						new Numeral(0.0), new Numeral(0)), new Function("indicatorLink", new Numeral(1), 
		                        new ParameterList()))));
		assertTrue(percepts.contains(expectedPercept));

	}
	
	/**
	 * Test my_stakeholder_id percept.
	 * @throws ManagementException {@link ManagementException} 
	 * @throws RelationException {@link RelationException}
	 * @throws AgentException {@link AgentException}
	 * @throws InterruptedException {@link InterruptedException}
	 * @throws PerceiveException {@link PerceiveException}
	 * @throws NoEnvironmentException {@link NoEnvironmentException}
	 */
	@Test 
	public void testMyStakeholderIdInhabitant() throws ManagementException, 
	RelationException, AgentException, InterruptedException,
	PerceiveException, NoEnvironmentException {
		
		joinAsInhabitants();
		LinkedList<Percept> percepts = env.getAllPerceptsFromEntity(INHABITANTS);
		Percept expectedPercept = new Percept("my_stakeholder_id", 
				new Numeral(1));
		assertTrue(percepts.contains(expectedPercept));

	}

	/**
	 * Test my_stakeholder_id percept.
	 * @throws ManagementException {@link ManagementException} 
	 * @throws RelationException {@link RelationException}
	 * @throws AgentException {@link AgentException}
	 * @throws InterruptedException {@link InterruptedException}
	 * @throws PerceiveException {@link PerceiveException}
	 * @throws NoEnvironmentException {@link NoEnvironmentException}
	 */
	@Test 
	public void testMyStakeholderIdMunicipality() throws ManagementException, 
	RelationException, AgentException, InterruptedException,
	PerceiveException, NoEnvironmentException {
		
		joinAsMunicipality();
		LinkedList<Percept> percepts = env.getAllPerceptsFromEntity(MUNICIPALITY);
		Percept expectedPercept = new Percept("my_stakeholder_id", 
				new Numeral(0));
		assertTrue(percepts.contains(expectedPercept));

	}

	/**
	 * Init env and ask for inhabitant as stakeholder.
	 *
	 * @throws ManagementException {@link ManagementException}
	 * @throws InterruptedException {@link InterruptedException}
	 */
	private void joinAsInhabitants() throws ManagementException, InterruptedException {
		MyEnvListener listener = new MyEnvListener();
		env.attachEnvironmentListener(listener);

		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		parameters.put(PROJECT, PROJECTNAME);
		parameters.put(STAKEHOLDERS, new ParameterList(new Identifier(INHABITANTS)));

		// any slot so not specified.
		env.init(parameters);

		assertEquals(INHABITANTS, listener.waitForEntity());
	}

}
