package nl.tytech.core.client.net;

import java.util.concurrent.atomic.AtomicInteger;

public class TSlotConnection extends SlotConnection {

	private static AtomicInteger connectionCounter = new AtomicInteger(0);

	public static TSlotConnection createSlotConnection() {
		Integer connectionID = connectionCounter.incrementAndGet();
		return new TSlotConnection(connectionID);
	}

	private Integer connectionID;

	private TSlotConnection(Integer connectionID) {
		super(connectionID);
		this.connectionID = connectionID;
	}

	public Integer getConnectionID() {
		return connectionID;
	}

}
