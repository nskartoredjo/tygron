package contextvh;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Translator;
import eis.exceptions.EntityException;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.client.event.SlotEvent;
import nl.tytech.core.event.Event;
import nl.tytech.core.event.Event.EventTypeEnum;
import nl.tytech.core.net.Network;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.data.core.item.Item;
import nl.tytech.data.engine.item.Indicator;
import nl.tytech.data.engine.item.Land;
import nl.tytech.data.engine.item.Stakeholder;
import nl.tytech.data.engine.item.UpgradeType;
import tygronenv.EntityEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Listen to entity events and store them till they are needed. Thread safe
 * because callbacks and calls from GOAL will be asynchronous. The events that
 * are reported are set up in the constructor.
 *
 * @author W.Pasman
 */
public class ContextEntityEventHandler extends tygronenv.EntityEventHandler {

	private static final String ENTITY = "entity";
	private Translator translator = Translator.getInstance();
	private ConcurrentMap<EventTypeEnum, List<Percept>> collectedPercepts = new ConcurrentHashMap<>();
	private EntityEventListener entity;
	private ContextEntity stakeholder;
	private Integer connectionID;

	/**
	 * Creates a new {@link ContextEntityEventHandler} instance.
	 *
	 * @param listener
	 *            The listener that is used to serve updates
	 * @param connectID
	 *            The ID of the current connection
	 * @param contextEntity
	 *            The Entity this {@link ContextEntityEventHandler} applies to
	 */
	public ContextEntityEventHandler(final EntityEventListener listener, final Integer connectID,
			final ContextEntity contextEntity) {
		super(listener, connectID);
		this.entity = listener;
		this.connectionID = connectID;
		this.stakeholder = contextEntity;
		EventManager.addListener(this, MapLink.STAKEHOLDERS, MapLink.UPGRADE_TYPES, MapLink.INDICATORS);
	}

	/**
	 * Add new percept to the collection.
	 *
	 * @param type
	 *            The type of these percepts
	 * @param percepts
	 *            The list of the percepts to add
	 */
	private synchronized void addPercepts(final EventTypeEnum type, final List<Percept> percepts) {
		collectedPercepts.put(type, percepts);
	}

	/**
	 * Get the percepts and clean our {@link #collectedPercepts}.
	 *
	 * @return percepts collected since last call to this
	 */
	@Override
	public Map<EventTypeEnum, List<Percept>> getPercepts() {
		final Map<EventTypeEnum, List<Percept>> copy = super.getPercepts();
		copy.putAll(collectedPercepts);
		collectedPercepts = new ConcurrentHashMap<>();
		return copy;
	}

	/**
	 * Used to notify this event handler of an update to {@code event}. Converts
	 * registered Events to percepts that can be used in GOAL.
	 *
	 * @param event
	 *            The event that has received an update
	 */
	@Override
	public void notifyListener(final Event event) {
		if (!isForMe(event)) {
			return;
		}
		final EventTypeEnum type = event.getType();
		try {
			if (type instanceof MapLink) {
				switch ((MapLink) type) {
				case STAKEHOLDERS:
					createStakeholderPercepts(event.<ItemMap<Stakeholder>>
							getContent(MapLink.COMPLETE_COLLECTION),
							type);
					break;
				case UPGRADE_TYPES:
					createPercepts(event.<ItemMap<UpgradeType>>
							getContent(MapLink.COMPLETE_COLLECTION), type);
					break;
				case LANDS:
					createPercepts(event.<ItemMap<Land>>
							getContent(MapLink.COMPLETE_COLLECTION), type);
					break;
				case INDICATORS:
					createPercepts(event.<ItemMap<Indicator>>
							getContent(MapLink.COMPLETE_COLLECTION), type);
					break;
				default:
					super.notifyListener(event);
				}
			} else if (type == Network.ConnectionEvent.FIRST_UPDATE_FINISHED) {
				System.out.println("received  FIRST_UPDATE_FINISHED in " + toString());
				// entity is ready to run! Report to EIS
				entity.notifyReady(ENTITY);

				// remove stakeholders from the entity, so we don't get new
				// percepts of type stakeholders
				EventManager.removeListener(this, MapLink.STAKEHOLDERS);
			}
		} catch (EntityException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check if the received event update is for the current entity.
	 *
	 * @param event
	 *            the updated {@link Event}
	 * @return true if null or the slot numbers are the same
	 */
	private boolean isForMe(final Event event) {
		if (event instanceof SlotEvent) {
			final SlotEvent slotEvent = (SlotEvent) event;
			return connectionID.equals(slotEvent.getConnectionID());
		}
		return true;
	}

	/**
	 * Create percepts contained in a ClientItemMap array.
	 * <p>
	 * see {@link #createPercepts(ItemMap, EventTypeEnum, String)}. The
	 * perceptname is {@link EventTypeEnum#name()}.
	 *
	 * @param itemMap
	 *            list of ClientItemMap elements.
	 * @param type
	 *            the type of elements in the map.
	 * @param <T>
	 *            T should extend an Item.
	 */
	private <T extends Item> void createPercepts(final ItemMap<T> itemMap, final EventTypeEnum type) {
		createPercepts(itemMap, type, type.name().toLowerCase());
	}

	/**
	 * Create percepts contained in a ClientItemMap array and add them to the
	 * {@link #collectedPercepts}.
	 *
	 * @param itemMap
	 *            list of ClientItemMap elements.
	 * @param type
	 *            the type of elements in the map.
	 * @param perceptName
	 *            the name of the percept.
	 * @param <T>
	 *            T should extend an Item.
	 */
	private <T extends Item> void createPercepts(final ItemMap<T> itemMap, final EventTypeEnum type,
			final String perceptName) {
		final ArrayList<T> items = new ArrayList<>(itemMap.values());
		final List<Percept> percepts = new ArrayList<>();
		Parameter[] parameters = null;
		try {
			parameters = translator.translate2Parameter(items);
		} catch (TranslationException e) {
			e.printStackTrace();
		}
		if (parameters != null) {
			percepts.add(new Percept(perceptName, parameters));
		}
		addPercepts(type, percepts);
	}

	/**
	 * Create all percepts that involve stakeholders.
	 *
	 * @param itemMap
	 *            list of ClientItemMap elements.
	 * @param type
	 *            the type of elements in the map.
	 * @throws EntityException
	 *             Exception when we can't find a the correct stakeholder.
	 */
	private void createStakeholderPercepts(final ItemMap<Stakeholder> itemMap, final EventTypeEnum type)
			throws EntityException {
		createPercepts(itemMap, type, type.name().toLowerCase());
		final List<Percept> percepts = collectedPercepts.get(type);
		if (stakeholder.getStakeholder() != null) {
			final Percept myIdPercept = new Percept("my_stakeholder_id",
					new Numeral(stakeholder.getStakeholder().getID()));
			percepts.add(myIdPercept);
		}
	}

	/**
	 * Removes this entity from the event manager.
	 */
	@Override
	public void stop() {
		super.stop();
		EventManager.removeAllListeners(this);
	}
}
