package camunda.example;

import io.micronaut.test.annotation.MicronautTest;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorException;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Lukasz Frankowski
 */
@MicronautTest
public class UserRegistrationProcessTest {

	public static final String ROLE_ADMIN = "ROLE_ADMIN";
	public static final String ADMIN_ID = "1";

	@Inject protected UserRegistrationProcess process;

	public static final String PHONE = "+00123456789";

	@Test
	public void testUserRegistationStartFormValidation() {
		// Invalid phone (minlength=6)
		Assertions.assertThrows(FormFieldValidatorException.class, () -> process.start("11", CountryCode.PL));
		// Invalid value for enum form property: FR (we only allow PL and GB in the process config)
		Assertions.assertThrows(ProcessEngineException.class, () -> process.start(PHONE, CountryCode.FR));
	}

	@Test
	public void testUserRegistrationHappyPath() {
		String key = process.start(PHONE, CountryCode.PL);
		assertNotNull(process.processInstance(key));
		process.sendUserForm(key, "user@luna", "1234");
		assertNull(process.processInstance(key));
	}

	@Test
	public void testUserRegistrationInvalidCode() {
		String key = process.start(PHONE, CountryCode.PL);
		assertNotNull(process.processInstance(key));
		process.sendUserForm(key, "user@luna", "1235");
		assertNotNull(process.processInstance(key));

		List<Task> tasks = process.findActiveTasks(key);
		assertEquals(1, tasks.size());

		tasks = process.findUnassignedActiveTasks(key, List.of(ROLE_ADMIN));
		assertEquals(1, tasks.size());
	}

}
