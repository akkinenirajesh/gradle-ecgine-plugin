package org.ecgine.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

public class EManifest {

	private BundleType ecgineBundleType;
	private String symbolicName;
	private String version;
	private Project project;
	private Map<String, String> requiredBundles;

	public EManifest(Project project, File file) {
		this.project = project;

		Map<String, String> allProperties = readProperties(file);
		String sn = allProperties.get("Bundle-SymbolicName");
		if (sn != null) {
			symbolicName = sn.split(";")[0].trim();
		}

		String bt = allProperties.get("Ecgine-BundleType");
		if (bt != null) {
			ecgineBundleType = BundleType.valueOf(bt.trim().toLowerCase());
		}

		String bv = allProperties.get("Bundle-Version");
		if (bv != null) {
			version = bv.trim();
		}

		String rb = allProperties.get("Require-Bundle");
		if (rb != null) {
			prepareRequiredBundles(rb);
		}

		if (requiredBundles == null) {
			requiredBundles = new HashMap<>();
		}
	}

	private Map<String, String> readProperties(File file) {
		Map<String, String> allProperties = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			String name = null;
			StringBuilder value = new StringBuilder();
			while ((line = br.readLine()) != null) {
				if (line.contains(":")) {
					if (name != null) {
						allProperties.put(name, value.toString());
					}
					String[] split = line.split(":");
					name = split[0];
					value = new StringBuilder(split[1]);
				} else {
					value.append("\n").append(line);
				}
			}
			if (name != null) {
				allProperties.put(name, value.toString());
			}
		} catch (Exception e) {
			throw new GradleException("Unable to read .ecgine file", e);
		}
		return allProperties;
	}

	private void prepareRequiredBundles(String all) {
		try {
			requiredBundles = new HashMap<>();
			ManifestHeaderValue value = new ManifestHeaderValue(all);
			value.getElements().forEach(e -> {
				String name = e.getValues().get(0);
				String val = e.getAttributes().get("bundle-version");
				requiredBundles.put(name, val);
			});
		} catch (ParseException e) {
			throw new GradleException("", e);
		}
	}

	public boolean isServer() {
		return ecgineBundleType == BundleType.SERVER;
	}

	public boolean isShared() {
		return ecgineBundleType == BundleType.SHARED;
	}

	public boolean isClient() {
		return ecgineBundleType == BundleType.CLIENT;
	}

	public boolean isUnknown() {
		return ecgineBundleType == null;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public String getVersion() {
		return version;
	}

	public BundleType getEcgineBundleType() {
		return ecgineBundleType;
	}

	public Project getProject() {
		return project;
	}

	@Override
	public String toString() {
		return symbolicName;
	}

	public void foreachRequiredBundle(BiConsumer<String, String> requiredBundle) {
		requiredBundles.forEach(requiredBundle);
	}

	public File getJar() {
		return new File(getProject().getRootProject().getBuildDir(), project.getName() + ".jar");
	}

}
