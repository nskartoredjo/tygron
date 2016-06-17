package contextvh.translators;

import eis.eis2java.exception.TranslationException;
import nl.tytech.data.engine.item.Indicator;
import nl.tytech.data.engine.item.Stakeholder;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for the ContextJ2Stakeholder translator.
 *
 * @author Haoming
 */
public class ContextJ2StakeholderTest {
	/**
	 * Translator for the stakeholder class to indicatorLink.
	 */
	private ContextJ2Stakeholder translator;

	/**
	 * Stakeholder instance to translate.
	 */
	private Stakeholder stakeholder;

	/**
	 * Initialization method called before every test.
	 */
	@Before
	public void init() {
		translator = new ContextJ2Stakeholder();

		stakeholder = mock(Stakeholder.class);

		final List<Indicator> list = new LinkedList<Indicator>();

		when(stakeholder.getMyIndicators()).thenReturn(list);

	}

	/**
	 * Test method which verifies that the mocked object calls the
	 * getMyIndicators method. The other methods cannot be verified, since they
	 * are final methods. That does not work with mockito.
	 *
	 * @throws TranslationException
	 *             thrown if translating fails.
	 */
	@Test
	public void testVerifyGetMyIndicators() throws TranslationException {
		translator.translate(stakeholder);
		verify(stakeholder).getMyIndicators();
	}
}
