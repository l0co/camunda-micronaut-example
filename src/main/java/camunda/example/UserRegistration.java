package camunda.example;

import io.micronaut.core.annotation.Introspected;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import javax.annotation.Nonnull;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * Model to keep process variables in a standarized from
 */
@Introspected
public class UserRegistration implements Serializable {

	public static final String NAME = "registration";

	private static final long serialVersionUID = 8430676294952662979L;

	@Pattern(regexp = "\\+\\d{11}?") protected String phone;
	@Size(min = 4, max = 4) protected String code;
	protected String firstName; // TODOLF remove not null
	protected String lastName;
	@Email protected String email;
	protected String country;

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

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String variableName() {
		return NAME;
	}

	public void bindToExecution(@Nonnull DelegateExecution execution) {
		execution.getVariables().forEach((name, value) -> {
			if (!variableName().equals(name)) {
				try {
					Field field = getClass().getDeclaredField(name);
					field.setAccessible(true);
					field.set(this, value);
					execution.removeVariable(name);
				} catch (Exception e) {
					UserRegistrationProcess.logger.warn(String.format("Can't bind property: %s on class: %s", name, getClass().getSimpleName()), e);
				}
			}
		});
	}

}
