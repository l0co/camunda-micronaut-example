package camunda.example;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
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

	/**
	 * @return A business key of running process.
	 */
	public String start() {
		ProcessInstance instance = runtimeService.createProcessInstanceByKey("user-registration")
			.setVariable(UserRegistration.NAME, new UserRegistration())
			// this usage of business key is useless, however if we start process related to a given entity we can create meaningful
			// business keys, like "MyEntity:ID:shipmentProcess"
			.businessKey(UUID.randomUUID().toString())
			.execute();

		return instance.getBusinessKey();
	}

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
