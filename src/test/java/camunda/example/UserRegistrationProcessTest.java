package camunda.example;

import io.micronaut.test.annotation.MicronautTest;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

/**
 * @author Lukasz Frankowski
 */
@MicronautTest
public class UserRegistrationProcessTest {

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
	public void testUserRegistationHappyPath() {
		String key = process.start(PHONE, CountryCode.PL);
		process.sendUserForm(key, "user@luna");
	}

}
