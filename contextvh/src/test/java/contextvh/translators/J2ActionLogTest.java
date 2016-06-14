package contextvh.translators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import eis.eis2java.exception.TranslationException;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.data.engine.item.ActionLog;
import nl.tytech.data.engine.item.Indicator;
import nl.tytech.data.engine.item.Stakeholder;

/**
 * Test class for the J2ActionLog class.
 *
 * @author Haoming
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ EventManager.class, ActionLog.class, Indicator.class })
public class J2ActionLogTest {

	/**
	 * Translator to be tested.
	 */
	private J2ActionLog translator;

	/**
	 * ActionLog to be translated.
	 */
	private ActionLog actionLog;

	/**
	 * Indicator to be checked for increase.
	 */
	private Indicator indicator;

	/**
	 * Initialization method for the Test class. In this method mocks are
	 * created with PowerMockito for the dependencies. Also some of the required
	 * methods are stubbed in order for the tests to work.
	 */
	@SuppressWarnings("unchecked")
	@Before
	public void init() {
		translator = new J2ActionLog();
		actionLog = PowerMockito.mock(ActionLog.class);
		indicator = PowerMockito.mock(Indicator.class);
		ItemMap<Indicator> map = (ItemMap<Indicator>) mock(ItemMap.class);
		LinkedList<Indicator> list = new LinkedList<Indicator>();
		list.add(indicator);
		when(map.values()).thenReturn(list);
		PowerMockito.mockStatic(EventManager.class);
		PowerMockito.when(EventManager.<Indicator>getItemMap(MapLink.INDICATORS)).thenReturn(map);
		PowerMockito.when(actionLog.getID()).thenReturn(0);
		PowerMockito.when(actionLog.getAction()).thenReturn("action");
		Stakeholder stakeholder = PowerMockito.mock(Stakeholder.class);
		PowerMockito.when(stakeholder.getID()).thenReturn(0);
		PowerMockito.when(actionLog.getStakeholder()).thenReturn(stakeholder);
	}

	/**
	 * Test method for translating ActionLog and only checking that the right
	 * methods are called. Also check that the name of the function is correct.
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@Test
	public void testTranslateVerifyCalls() throws TranslationException {
		Function function = (Function) translator.translate(actionLog)[0];
		assertEquals(function.getName(), "actionlog");
		verify(actionLog).getAction();
		verify(actionLog).getIncrease(indicator);
		verify(actionLog).getStakeholder();
		verify(actionLog).getID();
	}

	/**
	 * Test method for translating ActionLog without an increased indicator.
	 * Check that the Id is the same as expected.
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@Test
	public void testTranslateWithNoIncreasedIndicatorId() throws TranslationException {
		when(actionLog.getIncrease(indicator)).thenReturn(0.0);
		Function function = (Function) translator.translate(actionLog)[0];
		for (Parameter p : function.getParameters()) {
			if (p instanceof Numeral) {
				assertEquals(((Numeral) p).getValue(), 0);
			}
		}
	}

	/**
	 * Test method for translating ActionLog without an increased indicator.
	 * Check that the identifier action is the same.
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@Test
	public void testTranslateWithNoIncreasedIndicatorIdentifier() throws TranslationException {
		when(actionLog.getIncrease(indicator)).thenReturn(0.0);
		Function function = (Function) translator.translate(actionLog)[0];
		verify(actionLog).getAction();
		verify(actionLog).getIncrease(indicator);
		verify(actionLog).getStakeholder();
		verify(actionLog).getID();
		for (Parameter p : function.getParameters()) {
			if (p instanceof Identifier) {
				assertEquals(((Identifier) p).getValue(), "action");
			}
		}
	}

	/**
	 * Test method for translating ActionLog without an increased indicator.
	 * Check that the list of increased/decreased indicators is empty.
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@Test
	public void testTranslateWithNoIncreasedIndicatorListEmpty() throws TranslationException {
		when(actionLog.getIncrease(indicator)).thenReturn(0.0);
		Function function = (Function) translator.translate(actionLog)[0];
		verify(actionLog).getAction();
		verify(actionLog).getIncrease(indicator);
		verify(actionLog).getStakeholder();
		verify(actionLog).getID();
		for (Parameter p : function.getParameters()) {
			if (p instanceof ParameterList) {
				assertTrue(((ParameterList) p).isEmpty());
			}
		}
	}

	/**
	 * Method to translate ActionLog with an increased indicator. Check that the
	 * list of increased indicators are not empty.
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@Test
	public void testTranslateWithIncreasedIndicatorNotEmptyList() throws TranslationException {
		when(actionLog.getIncrease(indicator)).thenReturn(2.0);
		Function function = (Function) translator.translate(actionLog)[0];
		for (Parameter p : function.getParameters()) {
			if (p instanceof ParameterList) {
				assertFalse(((ParameterList) p).isEmpty());
			}
		}
	}

	/**
	 * Method to translate ActionLog with an increased indicator. Check that the
	 * increased id of the increased indicator is correct.
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@Test
	public void testTranslateWithIncreasedIndicatorId() throws TranslationException {
		when(actionLog.getIncrease(indicator)).thenReturn(2.0);
		when(indicator.getID()).thenReturn(1);
		Function function = (Function) translator.translate(actionLog)[0];
		for (Parameter p : function.getParameters()) {
			if (p instanceof ParameterList) {
				for (Parameter par : (ParameterList) p) {
					Parameter indicatorId = ((ParameterList) par).get(0);
					assertEquals(((Numeral) indicatorId).getValue(), 1);
				}
			}
		}
	}

	/**
	 * Method to translate ActionLog with an increased indicator. Check that the
	 * increased value of the indicator is correct.
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@Test
	public void testTranslateWithIncreasedIndicatorValue() throws TranslationException {
		when(actionLog.getIncrease(indicator)).thenReturn(2.0);
		when(indicator.getID()).thenReturn(1);
		Function function = (Function) translator.translate(actionLog)[0];
		for (Parameter p : function.getParameters()) {
			if (p instanceof ParameterList) {
				for (Parameter par : (ParameterList) p) {
					Parameter increase = ((ParameterList) par).get(1);
					assertEquals(((Numeral) increase).getValue(), 2.0);
				}
			}
		}
	}
}
