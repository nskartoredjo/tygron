package contextvh.actions;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.Test;
import org.powermock.reflect.Whitebox;

import contextvh.ContextEntity;
import eis.eis2java.exception.TranslationException;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;

/**
 * Test the FilterPercepts class.
 * @author Dennis van Peer
 *
 */
public class FilterPerceptsTest {


	private ContextEntity mockEntity = mock(ContextEntity.class);

	private static final String STAKEHOLDERS = "stakeholders";
	private static final String BUILDINGS = "buildings";
	private static final String INDICATORS = "indicators";
	private static final String FUNCTIONS = "functions";
	/**
	 * Tests the call function.
	 * @throws TranslationException  When an invalid internal action parameter is provided.
	 */
	@Test
	public void testCall() throws TranslationException {
		FilterPercepts action = new FilterPercepts();
		LinkedList<Parameter> parameters = new LinkedList<Parameter>();
		ParameterList percepts = new ParameterList();
		Parameter percept = new Identifier(STAKEHOLDERS);
		percepts.add(percept);
		parameters.add(percepts);
		action.call(mockEntity, parameters);
		ArrayList<String> disabledPercepts = Whitebox.getInternalState(action, "disabledPercepts");
		assertTrue(disabledPercepts.contains(STAKEHOLDERS));
	}

	/**
	 * Tests the getName function.
	 * @throws TranslationException  When an invalid internal action parameter is provided.
	 */
	@Test
	public void testGetName() {
		FilterPercepts action = new FilterPercepts();
		assertTrue(action.getName().equals("filter_percepts"));
	}

	/**
	 * Tests the filterPercepts function.
	 * @throws TranslationException  When an invalid internal action parameter is provided.
	 */
	@Test
	public void testFilterPercepts() throws TranslationException {
		FilterPercepts action = new FilterPercepts();
		LinkedList<Parameter> parameters = new LinkedList<Parameter>();
		ParameterList filter = new ParameterList();
		Parameter perceptParameter1 = new Identifier(STAKEHOLDERS);
		Parameter perceptParameter2 = new Identifier(INDICATORS);
		Parameter perceptParameter3 = new Identifier(BUILDINGS);
		filter.add(perceptParameter1);
		filter.add(perceptParameter2);
		filter.add(perceptParameter3);
		parameters.add(filter);
		LinkedList<Percept> percepts = new LinkedList<Percept>();
		Percept percept1 = new Percept(FUNCTIONS);
		Percept percept2 = new Percept(STAKEHOLDERS);
		Percept percept3 = new Percept(BUILDINGS);
		percepts.add(percept1);
		percepts.add(percept2);
		percepts.add(percept3);


		action.call(mockEntity, parameters);
		LinkedList<Percept> filteredPercepts = action.filterPercepts(percepts);
		LinkedList<String> filteredPerceptNames = getPerceptNames(filteredPercepts);

		assertTrue(filteredPerceptNames.contains(FUNCTIONS)
				&& !(filteredPerceptNames.contains(STAKEHOLDERS)));
		filteredPerceptNames.remove(FUNCTIONS);
		assertTrue(filteredPerceptNames.isEmpty());

	}

	/**
	 * Creates a LinkedList of the names from a linkedlist of percepts.
	 * @param percepts the linkedlist of percepts
	 * @return linkedList of names from the input parameter
	 */
	private LinkedList<String> getPerceptNames(final LinkedList<Percept> percepts) {
		LinkedList<String> result = new LinkedList<String>();
		for (Percept percept : percepts) {
			result.add(percept.getName());
		}
		return result;
	}

}
