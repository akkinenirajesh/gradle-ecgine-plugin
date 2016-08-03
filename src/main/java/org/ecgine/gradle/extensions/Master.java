package org.ecgine.gradle.extensions;

public class Master {
	private String company = "Test";
	private String country = "India";
	private String email = "test@example.com";
	private String firstName = "First";
	private String lastName = "Last";
	private String password = "password";
	private String subDomain = "master";

	public String toProperty() {
		return company + "," + country + "," + email + "," + firstName + "," + lastName + "," + password + ","
				+ subDomain;
	}

	public void company(String company) {
		this.company = company;
	}

	public String getCompany() {
		return company;
	}

	public void country(String country) {
		this.country = country;
	}

	public String getCountry() {
		return country;
	}

	public void email(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public void firstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void lastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLastName() {
		return lastName;
	}

	public void password(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void domain(String subDomain) {
		this.subDomain = subDomain;
	}

	public String getSubDomain() {
		return subDomain;
	}
}
