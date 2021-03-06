package camunda.example;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Model to keep process variables in a standarized from
 */
@Introspected
public class UserRegistration extends BaseProcessModel {

	private static final long serialVersionUID = 8430676294952662979L;

	@Pattern(regexp = "\\+\\d{11}?") protected String phone;
	@Size(min = 4, max = 4) protected String code;
	protected String firstName;
	protected String lastName;
	@Email protected String email;
	protected String country;
	protected boolean codeVerificationStatus = false;

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

	public boolean isCodeVerificationStatus() {
		return codeVerificationStatus;
	}

	public void setCodeVerificationStatus(boolean codeVerificationStatus) {
		this.codeVerificationStatus = codeVerificationStatus;
	}

}
