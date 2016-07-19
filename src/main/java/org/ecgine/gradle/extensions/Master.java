package org.ecgine.gradle.extensions;

public class Master {
	public String company = "Test";
	public String country = "India";
	public String email = "test@example.com";
	public String firstName = "First";
	public String lastName = "Last";
	public String password = "#55java";
	public String subDomain = "master";

	public String toProperty() {
		return company + "," + country + "," + email + "," + firstName + "," + lastName + "," + password + ","
				+ subDomain;
	}
}
