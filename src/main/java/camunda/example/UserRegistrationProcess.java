package camunda.example;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * @author Lukasz Frankowski
 */
@Singleton
public class UserRegistrationProcess {

	public static final Logger logger = LoggerFactory.getLogger(UserRegistrationProcess.class);

	@Inject protected RepositoryService repositoryService;
	@Inject protected FormService formService;
	@Inject protected TaskService taskService;
	@Inject protected RuntimeService runtimeService;

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

		if (false) {

			// this example shows how to start a process with custom business key, but without form data
			// though we won't use this method, because we have some form data
			instance = runtimeService.createProcessInstanceByKey(USER_REGISTRATION_PROCESS)
				.setVariable(UserRegistration.NAME, new UserRegistration())
				.businessKey(processBusinessKey)
				.execute();

		} else {

			// this example shows how to start a process with custom business key with form data
			instance = formService.submitStartForm(processDefinition(USER_REGISTRATION_PROCESS).getId(), processBusinessKey, Map.of(
				"phone", phone,
				"country", countryCode.toString()
			));
			runtimeService.setVariable(instance.getProcessInstanceId(), UserRegistration.NAME, new UserRegistration());

		}

		// TODOLF at this point we have following variables on this process instance: registration, phone and country

		return instance.getBusinessKey();
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

	/**********************************************************************************************************
	 * Model to keep process variables in a standarized from
	 **********************************************************************************************************/

	public static class UserRegistration implements Serializable {

		public static final String NAME = "registration";
		private static final long serialVersionUID = 8430676294952662979L;
		@Pattern(regexp = "\\+\\d{11}?") protected String phone;
		@Size(min = 6, max = 6) protected String code;
		protected String firstName;
		protected String lastName;
		@Email protected String email;
		protected CountryCode country;

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public CountryCode getCountry() {
			return country;
		}

		public void setCountry(CountryCode country) {
			this.country = country;
		}
	}
}
