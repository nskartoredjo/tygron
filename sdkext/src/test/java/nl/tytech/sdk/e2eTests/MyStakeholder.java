package nl.tytech.sdk.e2eTests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.vividsolutions.jts.geom.MultiPolygon;

import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.client.net.ServicesManager;
import nl.tytech.core.client.net.TSlotConnection;
import nl.tytech.core.net.Network.AppType;
import nl.tytech.core.net.event.IOServiceEventType;
import nl.tytech.core.net.serializable.JoinReply;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.core.util.SettingsManager;
import nl.tytech.data.core.item.Answer;
import nl.tytech.data.core.item.Item;
import nl.tytech.data.engine.event.ParticipantEventType;
import nl.tytech.data.engine.item.Land;
import nl.tytech.data.engine.item.PopupData;
import nl.tytech.data.engine.item.SpecialOption;
import nl.tytech.data.engine.item.Stakeholder;
import nl.tytech.data.engine.item.Stakeholder.Type;

/**
 * Test stakeholder.
 *
 * @author W.Pasman
 *
 */
public class MyStakeholder {

	private Type type;
	private ExampleEventHandler events;
	private TSlotConnection connection;
	/**
	 * The actual matched stakeholder object that we found
	 */
	private Stakeholder actualStakeholder;

	final static int TIMEOUT = 5000;

	public MyStakeholder(Stakeholder.Type type, Integer slotID) {
		if (Item.NONE.equals(slotID)) {
			throw new NullPointerException("project=null");
		}
		this.type = type;
		participate(slotID);
		events = new ExampleEventHandler(connection);
	}

	/**
	 * @return event handler for this stakeholder
	 */
	public ExampleEventHandler getEventHandler() {
		return events;
	}

	/**
	 * @return the actual stakeholder. null until we have been connected with
	 *         the real stakeholder on the server.
	 */
	public Stakeholder getStakeholder() {
		return actualStakeholder;
	}

	/**
	 * Wait till this stakeholder appeared.
	 *
	 * @param timeoutMs
	 * @throws InterruptedException
	 */
	public void waitForAppearance() throws InterruptedException {

		events.waitFor(MapLink.STAKEHOLDERS);

		ItemMap<Stakeholder> allstakeholders = EventManager.getItemMap(connection.getConnectionID(),
				MapLink.STAKEHOLDERS);
		for (Stakeholder s : allstakeholders) {
			System.out.println("Stakeholder " + s + " entered");
			if (s.getType() == type) {
				actualStakeholder = s;
				return;
			}
		}
		throw new IllegalStateException("requested stakeholder never appeared!");
	}

	/**
	 * Make listener for the session.
	 *
	 * @return slotconnection on which to
	 */
	private TSlotConnection participate(Integer slotID) {
		assertTrue(slotID != null && slotID >= 0);

		JoinReply reply = ServicesManager.fireServiceEvent(IOServiceEventType.JOIN_SESSION, slotID,
				AppType.PARTICIPANT);
		assertNotNull(reply);

		connection = TSlotConnection.createSlotConnection();
		connection.initSettings(AppType.PARTICIPANT, SettingsManager.getServerIP(), slotID, reply.serverToken,
				reply.client.getClientToken());
		assertTrue(connection.connect());
		return connection;
	}

	/**
	 * Sell some piece of land.
	 *
	 * @param land
	 *            sellable land, see {@link #getSellableLand()}.
	 * @param buyerStakeholderID
	 *            stakeholder id, see {@link #getStakeholder()}
	 * @throws Exception
	 */
	public void sellLand(Land land, Integer buyerStakeholderID) {
		Integer sellerID = actualStakeholder.getID();

		MultiPolygon multiPolygon = land.getMultiPolygon();
		double sellPrice = 400;
		connection.fireServerEvent(true, ParticipantEventType.MAP_SELL_LAND, sellerID, buyerStakeholderID, multiPolygon,
				sellPrice);

	}

	/**
	 * @return a piece of sellable land, or null if no such land.
	 *
	 */
	public Land getSellableLand() {
		ItemMap<Land> lands = EventManager.getItemMap(connection.getConnectionID(), MapLink.LANDS);

		Integer myId = actualStakeholder.getID();

		for (Land land : lands) {
			if (land.getOwnerID() == myId) {
				return land;
			}
		}
		return null;
	}

	/**
	 * Confirm that land has been bought
	 *
	 * @param buyerID
	 *            the buyer ID
	 * @param direction
	 *            SpecialOption.Type
	 * @throws InterruptedException
	 */
	public void confirmLandTransaction(SpecialOption.Type direction) throws InterruptedException {
		System.out.println("" + actualStakeholder.getType() + " confirms land transaction");
		events.waitFor(MapLink.POPUPS);
		Integer me = actualStakeholder.getID();

		ItemMap<PopupData> popups = EventManager.getItemMap(connection.getConnectionID(), MapLink.POPUPS);
		for (PopupData popupData : popups) {
			boolean forMe = popupData.getVisibleForStakeholderIDs().contains(me);
			boolean correctMapLink = popupData.getContentMapLink() == MapLink.SPECIAL_OPTIONS;
			SpecialOption specialOption = EventManager.getItem(connection.getConnectionID(), MapLink.SPECIAL_OPTIONS,
					popupData.getContentLinkID());
			boolean isSellLand = specialOption != null && specialOption.getType() == direction;

			if (forMe && correctMapLink && isSellLand) {
				// time to react:
				Answer defaultAnswer = popupData.getAnswers().get(0);
				connection.fireServerEvent(true, ParticipantEventType.POPUP_ANSWER, me, popupData.getID(),
						defaultAnswer.getID());
				return;
			} else {
				System.out.println("popup " + popupData.getText() + ":" + forMe + correctMapLink + isSellLand);
			}

		}
		throw new IllegalStateException("Failed to confirm land transaction, the expected request didn't appear");

	}

	/**
	 * Close down connections and finish stakeholder
	 */
	public void close() {
		if (events != null) {
			events.close();
			events = null;
		}
		connection.disconnect(true);
		actualStakeholder = null;
	}

}