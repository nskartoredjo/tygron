package contextvh.translators;

import java.text.ParseException;
import java.util.List;

import com.vividsolutions.jts.geom.MultiPolygon;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Java2Parameter;
import eis.eis2java.translation.Translator;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.data.engine.item.ActionLog;
import nl.tytech.data.engine.item.Building;
import nl.tytech.data.engine.item.PopupData;
import nl.tytech.data.engine.item.Setting;
import nl.tytech.data.engine.item.SpecialOption;
import nl.tytech.data.engine.item.SpecialOption.Type;
import nl.tytech.data.engine.serializable.TimeState;
import nl.tytech.locale.TCurrency;
import nl.tytech.locale.unit.UnitSystem;
import nl.tytech.locale.unit.UnitSystemType;
import nl.tytech.util.StringUtils;

/**
 * Translate {@link Popup} into request(type, category, contentlinkID,
 * [visibleStakeholderIDs], [answers], price, multipolygon). Multipolygon and
 * price are optional.
 *
 * @author W.Pasman
 *
 */
public class J2PopupData implements Java2Parameter<PopupData> {

	/**
	 * Translator for additional translations.
	 */
	private final Translator translator = Translator.getInstance();

	/**
	 * Empty constructor.
	 */
	public J2PopupData() {
	}

	/**
	 * Method to translate the PopupData class.
	 */
	@Override
	public Parameter[] translate(final PopupData popup) throws TranslationException {
		String typeOfPopup = null;
		ParameterList actionLogIds = new ParameterList();

		if (popup.getContentMapLink() == MapLink.SPECIAL_OPTIONS) {
			SpecialOption specialOption = EventManager.getItem(
					MapLink.SPECIAL_OPTIONS, popup.getContentLinkID());
			Type optionType = specialOption.getType();
			typeOfPopup = optionType.name();
		} else if (popup.getContentMapLink() == MapLink.BUILDINGS) {
			Building building = popup.getItem(MapLink.BUILDINGS, popup.getContentLinkID());
			if (building != null && building.getTimeState() == TimeState.REQUEST_ZONING_APPROVAL) {
				typeOfPopup = "ZONING_PERMIT";
			} else {
				typeOfPopup = "PERMIT";
			}
		} else {
			typeOfPopup = "POPUP";
		}

		MultiPolygon mpolygon = popup.getMultiPolygon();
		Parameter parPolygon = new Identifier("NO_MULTIPOLYGON");
		Parameter parAreaSize = new Identifier("NO_SIZE");
		if (mpolygon != null) {
			parPolygon = translator.translate2Parameter(mpolygon)[0];
			parAreaSize = new Numeral(mpolygon.getArea());
		}
		Double price = getPriceFromPopup(popup);
		Parameter parPrice = new Identifier("NO_PRICE");
		if (price != null) {
			parPrice = new Numeral(price);
		}

		return new Parameter[] {
			new Function("request",
				new Identifier(popup.getType().name()),
				new Identifier(typeOfPopup),
				new Numeral(popup.getID()),
				new Numeral(popup.getContentLinkID()),
				getVisibleForStakeholderIDs(popup.getVisibleForStakeholderIDs()),
				actionLogIds,
				parPrice,
				parPolygon,
				parAreaSize,
				translator.translate2Parameter(popup.getAnswers())[0]
			)
		};
	}

	/**
	 * Method to return the PopupData class.
	 */
	@Override
	public Class<? extends PopupData> translatesFrom() {
		return PopupData.class;
	}

	/**
	 * Method to change the visibleStakeholderID list into a ParameterList.
	 *
	 * @param visibleList
	 *            list of stakeholderIDs.
	 * @return ParameterList of shIDs.
	 */
	public ParameterList getVisibleForStakeholderIDs(final List<Integer> visibleList) {
		ParameterList parVisibleList = new ParameterList();
		for (Integer id : visibleList) {
			parVisibleList.add(new Numeral(id));
		}
		return parVisibleList;
	}

	/**
	 * Method to find out to which action logs have a connection to a permit
	 * related popup.
	 *
	 * @param popup
	 *            the request to which actionlogs will be linked
	 * @return a list containing the revelant action log IDs
	 */
	public ParameterList getActionLogIds(final PopupData popup) {
		ParameterList correctActionLogIDs = new ParameterList();
		MapLink contentMapLink = popup.getContentMapLink();
		Building building = EventManager.getItem(contentMapLink, popup.getContentLinkID());

		ItemMap<ActionLog> actionLogs = EventManager.getItemMap(MapLink.ACTION_LOGS);
		for (ActionLog actionlog : actionLogs.values()) {
			if (actionlog.getBuildingIDs().contains(building.getID())) {
				correctActionLogIDs.add(new Numeral(actionlog.getID()));
			}
		}
		return correctActionLogIDs;
	}

	/**
	 * Method to get the price from the PopupData.
	 *
	 * @param popupData
	 *            PopupData to get the price from.
	 * @return Double price.
	 */
	public Double getPriceFromPopup(final PopupData popupData) {
		Setting unitSystemSetting = EventManager.getItem(
				MapLink.SETTINGS, Setting.Type.MEASUREMENT_SYSTEM_TYPE);
		UnitSystemType type = unitSystemSetting.getEnumValue(UnitSystemType.class);
		UnitSystem unitSystem = type.getImpl();
		Setting currency = EventManager.getItem(MapLink.SETTINGS, Setting.Type.CURRENCY);
		TCurrency tcurrency = currency.getEnumValue(TCurrency.class);

		char decimalSeperator = '.';
		if (type == UnitSystemType.SI) {
			decimalSeperator = ',';
		}

		String text = popupData.getText();
		String[] split = text.split(tcurrency.getCurrencyCharacter() + StringUtils.WHITESPACE);
		if (split.length == 2) {
			String numberString = split[1];
			int i = 0;
			for (; i < numberString.length(); ++i) {
				if (numberString.charAt(i) == decimalSeperator) {
					break;
				}
			}
			String result = numberString.substring(0, i);

			try {
				return unitSystem.parseDouble(result);
			} catch (ParseException e) {
				return null;
			}
		}

		return null;
	}
}
