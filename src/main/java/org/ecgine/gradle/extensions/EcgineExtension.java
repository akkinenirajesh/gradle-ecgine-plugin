package org.ecgine.gradle.extensions;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension to configure the Ecgine project
 */
public class EcgineExtension {

	private static final String MASTER_BUNDLE = "com.vimukti.ecgine.master";
	private static final String BUNDLE_DOWNLOAD = "/api/download/bundle";
	private static final String DEPENDENCY = "/api/dependencies";
	private static final String CONFIG = "/api/config";
	private static final String ECGINE_START = "/api/ecginestart";
	private static final String CREATE_EBUNDLE = "/api/createebundle";
	private static final String CREATE_PACKAGE = "/api/createpackage";
	private static final String CREATE_PACKAGE_VERSION = "/api/createpackageversion";

	public static final String NAME = "ecgine";

	/**
	 * This is master server address
	 */
	private String url = "https://vimukti.ecgine.com/";

	private String apiKey;

	/**
	 * This directory is used store all downloaded bundles
	 */
	private String plugins = "plugins";

	private String setup;

	private Map<String, String> bundles = new HashMap<>();

	public String getPlugins() {
		return plugins;
	}

	public void setPlugins(String destDir) {
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

	public void setup(String setup) {
		this.setup = setup;
	}

	public String getSetup() {
		if (setup == null) {
			setup = System.getProperty("user.home") + "/.ecgine/setup";
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

	public String getEcgineStartUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append(ECGINE_START);
		return b.toString();
	}

	public String getUploadBundleUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append(CREATE_EBUNDLE);
		return b.toString();
	}

	public String getCreatePackageVersionUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append(CREATE_PACKAGE_VERSION);
		return b.toString();
	}

	public String getCreatePackageUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append(CREATE_PACKAGE);
		return b.toString();
	}

}
