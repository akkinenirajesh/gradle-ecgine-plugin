package org.ecgine.gradle;

public class Bundle {

	private String name;
	private String version;

	private BundleType type;

	public Bundle(String name, String version, BundleType type) {
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

	public BundleType getType() {
		return type;
	}

}
