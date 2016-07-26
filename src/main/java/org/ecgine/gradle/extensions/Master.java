package org.ecgine.gradle.extensions;

public class Master {
	private String company = "Test";
	private String country = "India";
	private String email = "test@example.com";
	private String firstName = "First";
	private String lastName = "Last";
	private String password = "#55java";
	private String subDomain = "master";

	public String toProperty() {
		return company + "," + country + "," + email + "," + firstName + "," + lastName + "," + password + ","
				+ subDomain;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getCompany() {
		return company;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCountry() {
		return country;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setSubDomain(String subDomain) {
		this.subDomain = subDomain;
	}

	public String getSubDomain() {
		return subDomain;
	}
}
