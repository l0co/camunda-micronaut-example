package camunda.example;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * User registration process bean.
 *
 * @author Lukasz Frankowski
 */
@Singleton @Named(UserRegistrationProcess.BEAN_NAME)
public class UserRegistrationProcess extends BaseProcessHandler<UserRegistration> {

	public static final Logger logger = LoggerFactory.getLogger(UserRegistrationProcess.class);

	public static final String BEAN_NAME = "userRegistrationProcess";
	public static final String MODEL_NAME = "registration";

	public static final String TEST_CODE = "1234";

	@Override
	protected String name() {
		return BEAN_NAME;
	}

	@Override
	protected String modelName() {
		return MODEL_NAME;
	}

	@Override
	protected Optional<UserRegistration> newModel() {
		return Optional.of(new UserRegistration());
	}

	/**
	 * An example how to get the form data for the start task.
	 */
	public FormData getStartFormData() {
		return formService.getStartFormData(processDefinition().getId());
	}

	/**
	 * An example how to get the form for the start task rendered in AngularJS.
	 */
	public String getStartFormHtml() {
		return (String) formService.getRenderedStartForm(processDefinition().getId());
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
			instance = start(processBusinessKey);

			Task task = processService.findActiveTask(instance.getBusinessKey(), "user-send-phone");
			formService.submitTaskForm(task.getId(), Map.of(
				"phone", phone,
				"country", countryCode.toString()
			));

		} else {

			// this example shows how to start a process with custom business key with form data
			// NEVER USE THIS, because you cannot set own variables (even if you put setting variables after the line below, firstly
			// the other states executors will be executed, and only THEN the variable will be set)
			// it's better to use first task form instead of start form
			instance = formService.submitStartForm(processDefinition().getId(),
				processBusinessKey, Map.of(
					"phone", phone,
					"country", countryCode.toString()
				));

		}

		return instance.getBusinessKey();
	}

	public void systemSendVerificationCode(@Nonnull DelegateExecution execution, @Nonnull UserRegistration model) {
		logger.debug("Sending verification code to phone: {}", model.getPhone());
	}

	public void sendUserForm(@Nonnull String processBusinessKey, @Nonnull String email, @Nonnull String code) {
		Task task = processService.findActiveTask(processBusinessKey, "user-send-form");
		formService.submitTaskForm(task.getId(), Map.of(
			"email", email,
			"code", code
		));
	}

	public void systemVerifyCode(@Nonnull DelegateExecution execution, @Nonnull UserRegistration model) {
		model.setCodeVerificationStatus(TEST_CODE.equals(model.getCode()));
	}

}
