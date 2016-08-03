package org.ecgine.gradle.extensions;

public class Package {

	private String name;

	private String namespace;

	private String version;

	private String category;

	private String verticals;

	public void name(String name) {
		this.name = name;
	}

	public void namespace(String namespace) {
		this.namespace = namespace;
	}

	public void version(String version) {
		this.version = version;
	}

	public void category(String category) {
		this.category = category;
	}

	public void verticals(String verticals) {
		this.verticals = verticals;
	}

	public String getName() {
		return name;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getVersion() {
		return version;
	}

	public String getCategory() {
		return category;
	}

	public String getVerticals() {
		return verticals;
	}

}
