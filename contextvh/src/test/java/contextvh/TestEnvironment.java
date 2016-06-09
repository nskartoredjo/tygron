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

public class TestEnvironment extends tygronenv.TestEnvironment{

	private EisEnv env;
    private static final String STAKEHOLDERS = "stakeholders";

    @Override
    public EisEnv createEnvironment() {
        env =  new ContextEnv();
        return env;
    }

    @After
	public void after() throws ManagementException, InterruptedException {
        super.after();
		env.kill();
		env = null;
	}

	@Test
	public void testAnotherOwnerConnect() throws ManagementException {
		Map<String, Parameter> parameters = new HashMap<String, Parameter>();
        String PROJECT = "project";
        parameters.put(PROJECT, new Identifier("vhproject"));
        // No stakeholders with a default name
        parameters.put(STAKEHOLDERS, new ParameterList(new Identifier("TU")));
		// any slot so not specified.
		env.init(parameters);
	}
}
