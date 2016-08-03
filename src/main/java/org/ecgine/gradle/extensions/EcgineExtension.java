package org.ecgine.gradle.extensions;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;

import groovy.lang.Closure;

/**
 * Extension to configure the Ecgine project
 */
public class EcgineExtension {

	private static final String MASTER_BUNDLE = "com.vimukti.ecgine.master";
	private static final String BUNDLE_DOWNLOAD = "/api/download/bundle";
	private static final String DEPENDENCY = "/api/dependencies";
	private static final String CREATE_EBUNDLE = "/api/createebundle";
	private static final String CREATE_PACKAGE = "/api/createpackage";
	private static final String CREATE_PACKAGE_VERSION = "/api/createpackageversion";
	private static final String LOGIN = "/apikey";

	public static final String NAME = "ecgine";

	/**
	 * This is master server address
	 */
	private String url = "https://vimukti.ecgine.com/";

	/**
	 * This directory is used store all downloaded bundles
	 */
	private String plugins = "plugins";

	private Package pkg = new Package();

	private String setup;

	private Map<String, String> bundles = new HashMap<>();

	private Configuration client = new Configuration(8000, 0, "64m", "1g", null);
	private Configuration server = new Configuration(4000, 0, "64m", "1g", null);
	private Master master = new Master();

	private Project project;

	public EcgineExtension(Project project) {
		this.project = project;
	}

	public String getPlugins() {
		return plugins;
	}

	public void plugins(String destDir) {
		this.plugins = destDir;
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

	public void master(Closure<Master> master) {
		project.configure(this.master, master);
	}

	public void server(Closure<Configuration> server) {
		project.configure(this.server, server);
	}

	public void client(Closure<Configuration> client) {
		project.configure(this.client, client);
	}

	public void pkg(Closure<Package> pkg) {
		project.configure(this.pkg, pkg);
	}

	public Configuration getClient() {
		return client;
	}

	public Package getPkg() {
		return pkg;
	}

	public Configuration getServer() {
		return server;
	}

	public Master getMaster() {
		return master;
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
		b.append("/ecgine.config");
		return b.toString();
	}

	public String getEcgineStartUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append("/ecgine-start.jar");
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

	public String getLoginUrl() {
		StringBuilder b = new StringBuilder();
		b.append(getUrl());
		b.append(MASTER_BUNDLE);
		b.append(LOGIN);
		return b.toString();
	}

	public String getApiKey() {
		return (String) project.getProperties().get("ecgine.apikey");
	}

}
