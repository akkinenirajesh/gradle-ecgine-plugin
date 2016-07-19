package org.ecgine.gradle.extensions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Extension to configure the Ecgine project
 */
public class EcgineExtension {

	private static final String MASTER_BUNDLE = "com.vimukti.ecgine.master";
	private static final String BUNDLE_DOWNLOAD = "/download/bundle";
	private static final String DEPENDENCY = "/api/dependencies";
	private static final String CONFIG = "/api/config";

	public static final String NAME = "ecgine";

	/**
	 * This is master server address
	 */
	private String url = "https://vimukti.ecgine.com/";

	private String apiKey;

	/**
	 * This directory is used store all downloaded bundles
	 */
	private File plugins = new File("plugins");

	private File setup;

	private Map<String, String> bundles = new HashMap<>();

	public File getPlugins() {
		if (!plugins.exists()) {
			plugins.mkdirs();
		}
		return plugins;
	}

	public void setPlugins(File destDir) {
		this.plugins = destDir;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getUrl() {
		return url;
	}

	public void bundle(String name, String version) {
		this.bundles.put(name, version);
	}

	public void bundle(String nameAndVersion) {
		String[] split = nameAndVersion.split("_");
		this.bundles.put(split[0], split[1]);
	}

	public Map<String, String> getBundles() {
		return bundles;
	}

	public void setup(File setup) {
		this.setup = setup;
	}

	public File getSetup() {
		if (setup == null) {
			setup = new File(System.getProperty("user.home"), ".ecgine/setup");
		}
		if (!setup.exists()) {
			setup.mkdirs();
		}
		return setup;
	}

	public String getDependenciesUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append(DEPENDENCY);
		return b.toString();
	}

	public String getDownloadUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append(BUNDLE_DOWNLOAD);
		return b.toString();
	}

	public String getConfigUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append(CONFIG);
		return b.toString();
	}
}
