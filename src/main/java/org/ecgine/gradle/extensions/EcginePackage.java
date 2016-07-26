package org.ecgine.gradle.extensions;

public class EcginePackage {

	private String version;

	private String namespace;

	private String name;

	private String category;

	private String verticals;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getVerticals() {
		return verticals;
	}

	public void setVerticals(String verticals) {
		this.verticals = verticals;
	}

}
