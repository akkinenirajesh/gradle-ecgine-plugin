package org.ecgine.gradle;

public class Bundle {

	private String name;
	private String version;
	private String type;

	public Bundle(String name, String version, String type) {
		this.name = name;
		this.version = version;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getType() {
		return type;
	}

}
