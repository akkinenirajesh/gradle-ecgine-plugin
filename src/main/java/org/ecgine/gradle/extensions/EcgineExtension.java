package org.ecgine.gradle.extensions;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.internal.impldep.org.apache.commons.lang.SystemUtils;

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
	public static final String DEFAULT_JRE_VERSION = "jre-8u77";

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

	private HttpClient httpClient;

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

	public HttpClient getHttpClient() {
		if (httpClient == null) {
			try {
				SSLContextBuilder builder = SSLContexts.custom();
				builder.loadTrustMaterial(null, new TrustStrategy() {

					@Override
					public boolean isTrusted(java.security.cert.X509Certificate[] chain, String authType)
							throws java.security.cert.CertificateException {
						return true;
					}
				});
				SSLContext sslContext = builder.build();
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
						new X509HostnameVerifier() {
							@Override
							public void verify(String host, SSLSocket ssl) throws IOException {
							}

							@Override
							public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
							}

							@Override
							public boolean verify(String s, SSLSession sslSession) {
								return true;
							}

							@Override
							public void verify(String host, X509Certificate cert) throws SSLException {
							}
						});

				Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
						.<ConnectionSocketFactory> create().register("https", sslsf).build();

				PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
				httpClient = HttpClients.custom().setConnectionManager(cm).build();
			} catch (Exception e) {
				throw new GradleException(e.getMessage(), e);
			}
		}
		return httpClient;
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

	public String getCertificateUrl() {
		return "http://s1.infra.ecgine.com/certificate/vimukti_codegen_bundle.crt";
	}

	public String getJre(String jreName) {
		String osName = System.getProperty("os.name");
		if (osName.equalsIgnoreCase("Linux")) {
			jreName = jreName + "-linux";
		}
		String model = System.getProperty("sun.arch.data.model");
		return jreName + "-x" + model + ".zip";
	}

	/**
	 * def jres_download_url="http://192.168.0.2/ecgine/jres";
	 * 
	 * println
	 * "downloading jre: {jres_download_url}/${jre_version}-${jre_platform}.zip"
	 * 
	 * @return URL Of JRE
	 */
	public String getJREURL(String jre) {
		return "http://s1.infra.ecgine.com/ecgine/jres/" + jre;
	}
}
