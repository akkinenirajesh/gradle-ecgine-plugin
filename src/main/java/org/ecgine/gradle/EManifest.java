package org.ecgine.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

public class EManifest {

	private String ecgineBundleType;
	private String symbolicName;
	private String version;
	private Project project;

	public EManifest(Project project, File file) {
		this.project = project;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(":");
				String name = split[0];
				if (name.equals("Bundle-SymbolicName")) {
					symbolicName = split[1].split(";")[0].trim();
				} else if (name.equals("Ecgine-BundleType")) {
					ecgineBundleType = split[1].trim();
				} else if (name.equals("Bundle-Version")) {
					version = split[1].trim();
				}
				if (symbolicName != null && ecgineBundleType != null) {
					// Over no need to read entire file.
					break;
				}
			}
		} catch (Exception e) {
			throw new GradleException("Unable to read .ecgine file", e);
		}
		if (ecgineBundleType == null) {
			ecgineBundleType = "unknown";
		}
	}

	public boolean isServer() {
		return ecgineBundleType.equals("server");
	}

	public boolean isShared() {
		return ecgineBundleType.equals("shared");
	}

	public boolean isClient() {
		return ecgineBundleType.equals("client");
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public String getVersion() {
		return version;
	}

	public Project getProject() {
		return project;
	}

	@Override
	public String toString() {
		return symbolicName;
	}

}
