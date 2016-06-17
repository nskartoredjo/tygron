package contextvh.translators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.LinkedList;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import eis.eis2java.exception.TranslationException;
import eis.iilang.Function;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.data.core.item.Item;
import nl.tytech.data.engine.item.ActionLog;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.item.PopupData;
import nl.tytech.data.engine.item.PopupData.Type;

/**
 * Test class for the PopupData class.
 *
 * @author Nick Cleintuar
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ EventManager.class, PopupData.class })
public class J2PopupDataTest {

	/**
	 * Translator to be tested.
	 */
	private J2PopupData translator;

	private PopupData popupdata;

	private J2PopupData j2popupdataspy;

	/**
	 * Initialization method for the Test class. In this method mocks are
	 * created with PowerMockito for the dependencies. Also some of the required
	 * methods are stubbed in order for the tests to work.
	 */
	@Before
	public void init() {
		translator = new J2PopupData();
		popupdata = PowerMockito.mock(PopupData.class);
		j2popupdataspy = PowerMockito.spy(translator);
		PowerMockito.mockStatic(EventManager.class);
		PowerMockito.doReturn(0.0).when(j2popupdataspy).getPriceFromPopup(popupdata);
		PowerMockito.when(popupdata.getType()).thenReturn(Type.INTERACTION);
	}

	/**
	 * Test method for translating PopupData and only checking that the right
	 * methods are called. Also check that the name of the function is correct.
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@Test
	public void testTranslateVerifyCalls() throws TranslationException {
		Function function = (Function) j2popupdataspy.translate(popupdata)[0];
		assertEquals(function.getName(), "request");
		verify(popupdata).getVisibleForStakeholderIDs();
		verify(popupdata).getMultiPolygon();
		verify(popupdata).getType();
		verify(popupdata).getID();
	}

	/**
	 * Test method for translating PopupData which checks if the right
	 * typeOfPopup string is returned. Here it should just return "POPUP" as the
	 * MapLink is irrelevant
	 *
	 * @throws TranslationException
	 *             if translating goes wrong
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testCorrectTypeofPopupElse() throws TranslationException {
		PowerMockito.when(popupdata.getContentMapLink()).thenReturn(MapLink.ZIP_CODES);
		Function function = (Function) j2popupdataspy.translate(popupdata)[0];
		assertEquals(function.getName(), "request");
		assertTrue(function.getParameters().get(1).toString().contains("POPUP"));
	}

	/**
	 * Test method for translating PopupData which checks if the right
	 * typeOfPopup string is returned Here it should return "PERMIT" as a
	 * MapLink for the buildinds is given.
	 *
	 * @throws Exception
	 *             exception if something goes wrong.
	 */
	@SuppressWarnings({ "deprecation" })
	@Test
	public void testCorrectTypeofPopupBuildings() throws Exception {
		PowerMockito.when(popupdata.getContentMapLink()).thenReturn(MapLink.BUILDINGS);
		PowerMockito.when(popupdata.getContentLinkID()).thenReturn(0);
		LinkedList<Item> list = new LinkedList<Item>();
		list.add(new ActionLog());
		@SuppressWarnings("unchecked")
		ItemMap<Item> map = mock(ItemMap.class);
		PowerMockito.when(map.values()).thenReturn(list);
		PowerMockito.when(EventManager.getItemMap(MapLink.ACTION_LOGS)).thenReturn(map);
		PowerMockito.when(EventManager.getItem(MapLink.BUILDINGS, 0)).thenReturn(new Building());
		Function function = (Function) j2popupdataspy.translate(popupdata)[0];
		assertEquals(function.getName(), "request");
		assertTrue(function.getParameters().get(1).toString().contains("PERMIT"));
	}
}
