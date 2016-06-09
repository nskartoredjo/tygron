package nl.tytech.sdk.e2eTests;

import java.util.HashMap;
import java.util.Map;

import nl.tytech.core.client.event.EventIDListenerInterface;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.client.event.SlotEvent;
import nl.tytech.core.client.net.TSlotConnection;
import nl.tytech.core.event.Event;
import nl.tytech.core.event.EventListenerInterface;
import nl.tytech.core.net.Network;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.data.engine.item.Setting;

public class ExampleEventHandler implements EventListenerInterface, EventIDListenerInterface {

	private boolean mapUpdate = false;
	private Map<MapLink, Boolean> mapLinkUpdated = new HashMap<>();
	/**
	 * increments after every FIRST_UPDATE_FINISHED comes in
	 **/
	private int firstUpdate = 0;
	private final int TIMEOUT = 5000;

	private Integer connectionID;

	public ExampleEventHandler(TSlotConnection slotConnection) {
		this(slotConnection.getConnectionID());
	}

	public ExampleEventHandler(Integer connectionID) {
		this.connectionID = connectionID;

		EventManager.addListener(this, MapLink.class);
		EventManager.addEnumListener(this, MapLink.SETTINGS, Setting.Type.MAP_WIDTH_METERS);
		EventManager.addListener(this, Network.ConnectionEvent.FIRST_UPDATE_FINISHED);
	}

	public boolean isMapUpdated() {
		return mapUpdate;
	}

	public boolean isUpdated(MapLink mapLink) {
		Boolean result = mapLinkUpdated.get(mapLink);
		if (result == null) {
			result = Boolean.FALSE;
		}
		return result;
	}

	public boolean isUpdated(MapLink... mapLinks) {
		boolean result = true;
		for (MapLink mapLink : mapLinks) {
			result &= isUpdated(mapLink);
		}
		return result;
	}

	public void close() {
		EventManager.removeAllListeners(this);
	}

	public void resetUpdate(MapLink... mapLinks) {
		for (MapLink mapLink : mapLinks) {
			resetUpdate(mapLink);
		}
	}

	public void resetUpdate(MapLink mapLink) {
		mapLinkUpdated.put(mapLink, false);
	}

	@Override
	public void notifyEnumListener(Event event, Enum<?> enhum) {

		if (event instanceof SlotEvent) {
			SlotEvent slotEvent = (SlotEvent) event;
			if (!connectionID.equals(slotEvent.getConnectionID())) {
				return;
			}
		}

		if (enhum == Setting.Type.MAP_WIDTH_METERS) {
			Setting setting = EventManager.getItem(connectionID, MapLink.SETTINGS, Setting.Type.MAP_WIDTH_METERS);
			mapUpdate = true;
		}
	}

	@Override
	public void notifyIDListener(Event event, Integer arg1) {
		if (event instanceof SlotEvent) {
			SlotEvent slotEvent = (SlotEvent) event;
			if (!connectionID.equals(slotEvent.getConnectionID())) {
				return;
			}
		}
	}

	@Override
	public void notifyListener(Event event) {

		if (event instanceof SlotEvent) {
			SlotEvent slotEvent = (SlotEvent) event;
			if (!connectionID.equals(slotEvent.getConnectionID())) {
				return;
			}

		}
		if (event.getType() instanceof MapLink) {
			mapLinkUpdated.put((MapLink) event.getType(), true);

		} else if (event.getType() == Network.ConnectionEvent.FIRST_UPDATE_FINISHED) {
			firstUpdate();
		}
	}

	/**
	 * Called when first update comes in
	 */
	private void firstUpdate() {
		firstUpdate++;
	}

	/**
	 *
	 * @return number of calls to Network.ConnectionEvent.FIRST_UPDATE_FINISHED
	 */
	public int getNumberOfFirstUpdates() {
		return firstUpdate;
	}

	/**
	 * Wait for FIRST_UPDATE_FINISHED
	 *
	 * @param timeoutMs
	 *            the max time to wait (ms)
	 * @throws InterruptedException
	 *             if time-out occurs.
	 */
	public void waitForFirstUpdate(int timeoutMs) throws InterruptedException {
		// time to sleep if firstUpdate not yet
		final int SLEEPTIME = 100;

		while (firstUpdate == 0 && timeoutMs > 0) {
			Thread.sleep(SLEEPTIME);
			timeoutMs -= SLEEPTIME;
		}
		if (firstUpdate == 0) {
			throw new InterruptedException("Timed out on waiting for FIRST_UPDATE_FINISHED.");
		}
	}

	public void waitFor(MapLink... type) throws InterruptedException {
		int timeoutMs = TIMEOUT;
		// time to sleep if firstUpdate not yet
		final int SLEEPTIME = 100;

		while (!isUpdated(type) && timeoutMs > 0) {
			Thread.sleep(SLEEPTIME);
			timeoutMs -= SLEEPTIME;
		}
		if (!isUpdated(type)) {
			throw new InterruptedException("Timed out on waiting for Maplinks ." + toString(type));
		}
	}

	private String toString(MapLink... links) {
		String value = "";
		for (MapLink link : links) {
			value += link + ",";
		}
		return value;
	}

}
