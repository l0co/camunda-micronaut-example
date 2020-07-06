package camunda.example;

import io.micronaut.validation.validator.Validator;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * User registration process bean.
 *
 * @author Lukasz Frankowski
 */
@Singleton @Named(UserRegistrationProcess.BEAN_NAME)
public class UserRegistrationProcess {

	public static final String BEAN_NAME = "userRegistrationProcess";

	public static final Logger logger = LoggerFactory.getLogger(UserRegistrationProcess.class);

	@Inject protected RepositoryService repositoryService;
	@Inject protected FormService formService;
	@Inject protected TaskService taskService;
	@Inject protected RuntimeService runtimeService;
	@Inject protected Validator validator;

	// note that each swimlane in bpmn collaboration is created as a separate process, here go names of all processes in our collaboration
	public static final String USER_REGISTRATION_PROCESS = "user-registration";

	/**
	 * An example how to get the form data for the start task.
	 */
	public FormData getStartFormData() {
		return formService.getStartFormData(processDefinition(USER_REGISTRATION_PROCESS).getId());
	}

	/**
	 * An example how to get the form for the start task rendered in AngularJS.
	 */
	public String getStartFormHtml() {
		return (String) formService.getRenderedStartForm(processDefinition(USER_REGISTRATION_PROCESS).getId());
	}

	/**
	 * @return A business key of running process.
	 */
	@SuppressWarnings("ConstantConditions")
	public String start(@Nonnull String phone, @Nonnull CountryCode countryCode) {
		ProcessInstance instance;

		// this usage of business key is useless, however if we start process related to a given entity we can create meaningful
		// business keys, like "MyEntity:ID:shipmentProcess"
		String processBusinessKey = UUID.randomUUID().toString();

		if (true) {

			// this example shows how to start a process with custom business key, but without form data
			instance = runtimeService.createProcessInstanceByKey(USER_REGISTRATION_PROCESS)
				.setVariable(UserRegistration.NAME, new UserRegistration())
				.businessKey(processBusinessKey)
				.execute();

			Task task = findTask(instance.getRootProcessInstanceId(), "user-send-phone");
			formService.submitTaskForm(task.getId(), Map.of(
				"phone", phone,
				"country", countryCode.toString()
			));

		} else {

			// this example shows how to start a process with custom business key with form data
			// NEVER USE THIS, because you cannot set own variables (even if you put setting variables after the line below, firstly
			// the other states executors will be executed, and only THEN the variable will be set)
			// it's better to use first task form instead of start form
			instance = formService.submitStartForm(processDefinition(USER_REGISTRATION_PROCESS).getId(), processBusinessKey, Map.of(
				"phone", phone,
				"country", countryCode.toString()
			));

		}

		return instance.getBusinessKey();
	}

	public void bind(@Nonnull DelegateExecution execution) {
		UserRegistration model = findModel(execution);
		model.bindToExecution(execution);
		Set<ConstraintViolation<UserRegistration>> cv = validator.validate(model);
		if (!cv.isEmpty())
			throw new ConstraintViolationException(cv);
	}

	public String systemSendVerificationCode(@Nonnull DelegateExecution execution, @Nonnull UserRegistration model) {
		logger.debug("Sending verification code to phone: {}", model.getPhone());
		return "1234";
	}

	/**********************************************************************************************************
	 * Could go into the super class
	 **********************************************************************************************************/

	protected ProcessDefinition processDefinition(String processDefinitionKey) {
		return repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).singleResult();
	}

	protected ProcessInstance processInstance(String businessKey) {
		return runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
	}

	protected Task findTask(@Nonnull String processInstanceId, @Nonnull String taskDefinitionKey) {
		return taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey(taskDefinitionKey).singleResult();
	}

	protected UserRegistration findModel(@Nonnull DelegateExecution execution) {
		return ((UserRegistration) execution.getVariable(UserRegistration.NAME));
	}

}
