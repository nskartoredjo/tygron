package nl.tytech.sdk.e2eTests;

import static org.junit.Assert.assertEquals;

import javax.security.auth.login.LoginException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import login.ProjectException;
import nl.tytech.core.client.event.EventManager;
import nl.tytech.core.net.serializable.MapLink;
import nl.tytech.core.structure.ItemMap;
import nl.tytech.data.core.item.Item;
import nl.tytech.data.engine.item.Stakeholder;

public class FirstUpdateTest {

	private GameField gameField;

	/**
	 * login, create Game field.
	 */
	@Before
	public void before() throws LoginException, ProjectException {
		gameField = new GameField();
	}

	@After
	public void after() throws InterruptedException, ProjectException {
		gameField.close();
	}

	@Test
	public void checkInitialStakeholders() throws InterruptedException {
		MyStakeholder municipality = gameField.addStakeholder(Stakeholder.Type.MUNICIPALITY);
		municipality.getEventHandler().waitForFirstUpdate(5000);

		ItemMap<Item> map = EventManager.getItemMap(gameField.getSlotConnection().getConnectionID(),
				MapLink.STAKEHOLDERS);
		assertEquals(2, map.size());
		municipality.close();
	}

	@Test
	public void checkExactlyOneFirstUpdate() throws InterruptedException {

		MyStakeholder municipality = gameField.addStakeholder(Stakeholder.Type.MUNICIPALITY);

		municipality.getEventHandler().waitForFirstUpdate(5000);

		MyStakeholder civilian = gameField.addStakeholder(Stakeholder.Type.CIVILIAN);
		civilian.getEventHandler().waitForFirstUpdate(5000);

		assertEquals(1, municipality.getEventHandler().getNumberOfFirstUpdates());
		assertEquals(1, civilian.getEventHandler().getNumberOfFirstUpdates());

		municipality.close();
		civilian.close();

	}

}
