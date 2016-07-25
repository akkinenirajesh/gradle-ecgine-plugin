package org.ecgine.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.ecgine.gradle.extensions.Configuration;
import org.ecgine.gradle.extensions.EcgineExtension;
import org.ecgine.gradle.extensions.Master;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Exec;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unchecked")
public abstract class AbstractStart extends Exec {

	protected void prepareSetup(String type) {
		try {
			EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);
			Configuration cfg = (Configuration) getProject().getExtensions().getByName(type);
			Master master = (Master) getProject().getExtensions().getByName("master");
			cfg.property("ecgine.vimukti.master", master.subDomain + ".ecgine.com");
			JSONObject config = getConfiguration(ext, type);
			File plugins = new File(ext.getPlugins());
			if (!plugins.exists()) {
				plugins.mkdirs();
			}
			List<String> cmds = prepareSetup(plugins, cfg, ext.getSetup(), type, config);
			setCommandLine(cmds);
			setWorkingDir(new File(ext.getSetup(), type));
			super.exec();
		} catch (IOException e) {
			getLogger().error("Unable to start ecgine", e);
			throw new GradleException("", e);
		}
	}

	private JSONObject getConfiguration(EcgineExtension ext, String type) throws IOException {
		File plugins = new File(ext.getPlugins());
		if (!plugins.exists()) {
			plugins.mkdirs();
		}
		File config = new File(plugins, ".config");
		if (!config.exists()) {
			downloadConfigFile(ext, config, ext.getConfigUrl());
		}
		getLogger().debug("loading .config file->" + config.getAbsolutePath());
		String string = new String(Files.readAllBytes(config.toPath()));
		JSONObject c = new JSONObject(string).getJSONObject(type);
		JSONObject bundles = c.getJSONObject("bundles");

		StringBuilder testBundles = new StringBuilder();
		JSONArray devBundles = new JSONArray();
		Set<String> notFound = new HashSet<>();
		getLogger().debug("adding developer bundles");
		EcgineUtils.forEachProject(getProject(), p -> {
			EManifest manifest = EcgineUtils.readManifest(p);
			if (manifest == null) {
				return;
			}
			if (filterDevBundle(manifest)) {
				JSONObject jar = new JSONObject();
				jar.put("jar", p.getName() + ".jar");
				jar.put("start", 5);
				devBundles.put(jar);
				File sourceJar = new File(p.getRootProject().getBuildDir(), jar.getString("jar"));
				if (!EcgineUtils.copy(sourceJar, new File(plugins, jar.getString("jar")))) {
					notFound.add(manifest.getSymbolicName());
				}
				testBundles.append(manifest.getSymbolicName()).append(",");
			}
		});
		if (!notFound.isEmpty()) {
			System.err.println("following jars are not found");
			System.err.println("run build task");
			notFound.forEach(j -> getLogger().error(j));
			throw new RuntimeException();
		}

		if (devBundles.length() != 0) {
			c.getJSONObject("properties").put("ecgine.tenent.testbundles",
					testBundles.substring(0, testBundles.length() - 1));
			bundles.put("dev-bundles", devBundles);
		} else {
			getLogger().debug("No developer bundles found");
		}

		return c;
	}

	protected abstract boolean filterDevBundle(EManifest manifest);

	protected void downloadConfigFile(EcgineExtension ext, File file, String url) {
		System.out.println(file.getName() + " file not found->" + file.getAbsolutePath());
		try {
			System.out.println("Downloading " + file.getName() + " file->" + url);
			HttpPost request = new HttpPost(url);
			request.addHeader("apikey", ext.getApiKey());
			JSONArray array = new JSONArray();
			request.setEntity(new StringEntity(array.toString()));
			HttpResponse response = HttpClientBuilder.create().build().execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code != 200) {
				EntityUtils.consume(response.getEntity());
				if (code == 401) {
					throw new GradleException("Invalid api key");
				} else {
					throw new GradleException("StatusCode:" + code + " URL:" + url);
				}
			}
			System.out.println("Got " + file.getName() + " file.");
			IOUtils.copy(response.getEntity().getContent(), new FileOutputStream(file));
		} catch (Exception e) {
			throw new GradleException("", e);
		}
	}

	private List<String> prepareSetup(File plugins, Configuration con, String setup, String type, JSONObject config)
			throws IOException {
		File root = new File(setup, type);
		if (root.exists()) {
			FileUtils.deleteDirectory(root);
		}
		root.mkdirs();

		Map<String, String> configProps = new HashMap<>();

		// copy all bundles(jars)
		String bundles = copyAllJars(plugins, root, config.getJSONObject("bundles"), true);
		configProps.put("osgi.bundles", bundles);

		copyHomeJars(plugins, root, config.getJSONArray("home"));

		JSONObject props = config.getJSONObject("properties");
		props.keySet().forEach(k -> configProps.put(k, props.getString(k)));

		configProps.putAll(con.getProperties());

		File configuration = new File(root, "configuration");
		Files.createDirectories(configuration.toPath());
		configuration.mkdir();
		EcgineUtils.writeProperties(getLogger(), new File(configuration, "config.ini"), configProps);

		return prepareRunner(root, config.getString("runJar"), con);
	}

	private List<String> prepareRunner(File root, String jar, Configuration cfg) {
		List<String> cmds = new ArrayList<>();
		// exec $JAVA $* -Xdebug
		// -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -jar
		// org.eclipse.osgi_3.10.101.v20150820-1432.jar -console 2501

		cmds.add("java");

		String ms = cfg.getMs();
		if (ms != null) {
			cmds.add("-Xms" + ms);
		}
		String mx = cfg.getMx();
		if (mx != null) {
			cmds.add("-Xmx" + mx);
		}

		int port = cfg.getDebugPort();
		if (port > 0) {
			cmds.add("-Xrunjdwp:server=y,transport=dt_socket,address=" + port + ",suspend=n");
		}

		cmds.add("-jar");
		cmds.add(jar);
		cmds.add(".");

		int cp = cfg.getConsolePort();
		if (cp > 0) {
			cmds.add("-console");
			cmds.add(String.valueOf(cp));
		}

		getLogger().debug("Command:" + cmds.toString());
		return cmds;
	}

	private void copyHomeJars(File plugins, File server, JSONArray array) {
		Set<String> notFound = new HashSet<>();
		array.forEach(j -> {
			File jar = new File(server, j.toString());
			if (!jar.exists()) {
				if (!EcgineUtils.copy(new File(plugins, j.toString()), jar)) {
					notFound.add(jar.getName());
				}
			}
		});

		if (!notFound.isEmpty()) {
			System.err.println("following jars are not found in :" + plugins);
			System.err.println("run bundles task");
			notFound.forEach(j -> getLogger().error(j));
			throw new RuntimeException();
		}
	}

	private String copyAllJars(File plugins, File root, JSONObject bundles, boolean needDownload) {
		getLogger().debug("Copy all jars from:" + plugins + " to setup folder");
		Set<String> notFound = new HashSet<>();
		StringBuilder property = new StringBuilder();
		bundles.keySet().forEach(k -> {
			JSONArray array = bundles.getJSONArray(k);
			File folder = new File(root, k);
			folder.mkdir();
			array.forEach(j -> {
				JSONObject obj = (JSONObject) j;
				File jar = new File(folder, obj.getString("jar"));
				if (!jar.exists()) {
					if (!EcgineUtils.copy(new File(plugins, jar.getName()), jar)) {
						notFound.add(jar.getName());
					}
				}
				property.append("reference\\:file\\:./");
				property.append(k).append("/");
				property.append(jar.getName());
				if (!obj.has("isFragment") || !obj.getBoolean("isFragment")) {
					if (obj.has("start")) {
						property.append("@").append(obj.getInt("start")).append("\\:start");
					} else {
						property.append("@start");
					}
				}
				property.append(",");
			});
		});

		if (needDownload && !notFound.isEmpty()) {
			EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);
			HttpClient client = HttpClientBuilder.create().build();
			notFound.forEach(n -> {
				String[] split = n.split("_");
				String version = split[split.length - 1];
				n = n.replaceFirst("_" + version, "");
				version = version.replaceFirst(".jar", "");
				EcgineBundlesTask.downloadBundle(ext, client, n, version);
			});
			return copyAllJars(plugins, root, bundles, false);
		}

		if (notFound.isEmpty()) {
			property.subSequence(0, property.length() - 2);
			return property.toString();
		}

		System.err.println("following jars are not found in :" + plugins);
		System.err.println("run bundles task");
		notFound.forEach(j -> getLogger().error(j));
		throw new RuntimeException();
	}
}
