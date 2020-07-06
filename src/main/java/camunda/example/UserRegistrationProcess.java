package camunda.example;

import io.micronaut.validation.validator.Validator;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.form.TaskFormData;
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
import java.util.*;

/**
 * User registration process bean.
 *
 * @author Lukasz Frankowski
 */
@Singleton @Named(UserRegistrationProcess.BEAN_NAME)
public class UserRegistrationProcess {

	public static final String BEAN_NAME = "userRegistrationProcess";

	public static final Logger logger = LoggerFactory.getLogger(UserRegistrationProcess.class);
	public static final String TEST_CODE = "1234";

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

			Task task = findActiveTask(instance.getBusinessKey(), "user-send-phone");
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

	public void systemSendVerificationCode(@Nonnull DelegateExecution execution, @Nonnull UserRegistration model) {
		logger.debug("Sending verification code to phone: {}", model.getPhone());
	}

	public void sendUserForm(@Nonnull String processBusinessKey, @Nonnull String email, @Nonnull String code) {
		Task task = findActiveTask(processBusinessKey, "user-send-form");
		formService.submitTaskForm(task.getId(), Map.of(
			"email", email,
			"code", code
		));
	}

	public void systemVerifyCode(@Nonnull DelegateExecution execution, @Nonnull UserRegistration model) {
		model.setCodeVerificationStatus(TEST_CODE.equals(model.getCode()));
	}

	/**********************************************************************************************************
	 * Could go into the super class
	 **********************************************************************************************************/

	protected ProcessDefinition processDefinition(String processDefinitionKey) {
		return repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).singleResult();
	}

	public ProcessInstance processInstance(String businessKey) {
		return runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
	}

	public List<Task> findActiveTasks(@Nonnull String processBusinessKey) {
		return taskService.createTaskQuery()
			.processInstanceBusinessKey(processBusinessKey)
			.active()
			.list();
	}

	public List<Task> findUnassignedActiveTasks(@Nonnull String processBusinessKey, @Nonnull List<String> assignableRoleNames) {
		return taskService.createTaskQuery()
			.processInstanceBusinessKey(processBusinessKey)
			.active()
			.taskUnassigned()
			.taskCandidateGroupIn(assignableRoleNames)
			.list();
	}

	public Task findActiveTask(@Nonnull String processBusinessKey, @Nonnull String taskDefinitionKey) {
		return taskService.createTaskQuery()
			.processInstanceBusinessKey(processBusinessKey)
			.taskDefinitionKey(taskDefinitionKey)
			.active()
			.singleResult();
	}

	public List<Task> findActiveTasksAssignedToUser(@Nonnull String processBusinessKey, @Nonnull String userId) {
		return taskService.createTaskQuery()
			.processInstanceBusinessKey(processBusinessKey)
			.taskAssignee(userId)
			.active()
			.list();
	}

	public void claimTask(@Nonnull Task task, @Nonnull String userId) {
		taskService.claim(task.getId(), userId);
	}

	/**
	 * Completes task with no form filling
	 */
	public void completeTask(@Nonnull Task task) {
		taskService.complete(task.getId());
	}

	public Optional<TaskFormData> getTaskForm(@Nonnull Task task) {
		return Optional.ofNullable(formService.getTaskFormData(task.getId()));
	}

	public void completeTask(@Nonnull Task task, @Nonnull Map<String, Object> form) {
		formService.submitTaskForm(task.getId(), form);
	}

	protected UserRegistration findModel(@Nonnull DelegateExecution execution) {
		return ((UserRegistration) execution.getVariable(UserRegistration.NAME));
	}

}
