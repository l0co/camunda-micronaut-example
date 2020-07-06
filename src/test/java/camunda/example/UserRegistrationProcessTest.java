package camunda.example;

import io.micronaut.test.annotation.MicronautTest;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorException;
import org.camunda.bpm.engine.task.Task;
import org.javatuples.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
		Pair<String, Task> pair = goToFirstAdminTask();
		String key = pair.getValue0();
		Task task = pair.getValue1();

		Optional<TaskFormData> taskForm = process.getTaskForm(task);
		assertTrue(taskForm.isPresent());
		process.completeTask(task, Map.of("path", "END"));
		assertNull(process.processInstance(key));
	}

	@Test
	public void testUserRegistrationInvalidCodeWithAdminContinuation() {
		Pair<String, Task> pair = goToFirstAdminTask();
		String key = pair.getValue0();
		Task task = pair.getValue1();

		process.completeTask(task, Map.of("path", "CONTINUE"));
		assertNotNull(process.processInstance(key));

		List<Task> tasks = process.findActiveTasks(key);
		// TODOLF this task should already be assigned to the same admin, and isn't
	}

	public Pair<String, Task> goToFirstAdminTask() {
		String key = process.start(PHONE, CountryCode.PL);
		assertNotNull(process.processInstance(key));
		process.sendUserForm(key, "user@luna", "1235");
		assertNotNull(process.processInstance(key));

		List<Task> tasks = process.findActiveTasks(key);
		assertEquals(1, tasks.size());

		tasks = process.findUnassignedActiveTasks(key, List.of(ROLE_ADMIN));
		assertEquals(1, tasks.size());
		Task task = tasks.iterator().next();

		process.claimTask(task, ADMIN_ID);

		tasks = process.findUnassignedActiveTasks(key, List.of(ROLE_ADMIN));
		assertEquals(0, tasks.size());

		tasks = process.findActiveTasksAssignedToUser(key, ADMIN_ID);
		assertEquals(1, tasks.size());
		task = tasks.iterator().next();

		return new Pair<>(key, task);
	}

}
