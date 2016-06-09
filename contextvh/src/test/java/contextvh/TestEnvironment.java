package contextvh;

import eis.exceptions.ManagementException;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import org.junit.After;
import org.junit.Test;
import tygronenv.EisEnv;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the additional functionality in ContextEnv.
 * @author Max Groenenboom
 */
public class TestEnvironment extends tygronenv.TestEnvironment {

	private ContextEnv env;
	private static final String STAKEHOLDERS = "stakeholders";

	@Override
	public EisEnv createEnvironment() {
		env =  new ContextEnv();
		return env;
	}

	/**
	 * Method executed after each test.
	 * @throws ManagementException Unexpected Exception.
	 * @throws InterruptedException Unexpected Exception.
	 */
	@After
	public void after() throws ManagementException, InterruptedException {
		super.after();
		env.kill();
		env = null;
	}

	/**
	 * Tests if the environment can be correctly initiated.
	 * @throws ManagementException Unexpected Exception.
	 */
	@Test
	public void testAnotherOwnerConnect() throws ManagementException {
		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
		final String project = "project";
		parameters.put(project, new Identifier("vhproject"));
		// No stakeholders with a default name
		parameters.put(STAKEHOLDERS, new ParameterList(new Identifier("TU")));
		// any slot so not specified.
		env.init(parameters);
	}
}
