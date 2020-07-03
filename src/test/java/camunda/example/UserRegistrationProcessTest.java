package camunda.example;

import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

/**
 * @author Lukasz Frankowski
 */
@MicronautTest
public class UserRegistrationProcessTest {

	@Inject protected UserRegistrationProcess process;

	@Test
	public void testUserRegistationHappyPath() {
		String key = process.start();
	}

}
