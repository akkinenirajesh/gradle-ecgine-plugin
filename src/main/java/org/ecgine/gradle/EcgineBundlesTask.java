package org.ecgine.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.ecgine.gradle.extensions.EcgineExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.json.JSONArray;
import org.json.JSONObject;

public class EcgineBundlesTask extends DefaultTask {

	@TaskAction
	public void bundles() {
		HttpClient client = HttpClientBuilder.create().build();

		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);

		// Read from file
		Map<String, JSONObject> allDepends = EcgineUtils.readJarDependencies(getLogger(), getProject());

		// Get all project dependencies
		Map<String, String> dependencies = ext.getBundles();

		Map<String, String> allJars = new HashMap<>();
		boolean needUpdate = prepareJarDependencies(ext, client, allDepends, dependencies, allJars, true);
		if (needUpdate) {
			EcgineUtils.updateJarDependencies(getLogger(), getProject(), allDepends);
		}

		Set<String> notFoundJars = new HashSet<>();
		allJars.forEach((n, v) -> {
			File jar = downloadBundle(ext, client, n, v);
			if (!jar.exists()) {
				notFoundJars.add(jar.getName());
			}
		});
		if (!notFoundJars.isEmpty()) {
			throw new GradleException();
		}
		throw new GradleException();
	}

	private boolean prepareJarDependencies(EcgineExtension ext, HttpClient client, Map<String, JSONObject> allDepends,
			Map<String, String> dependencies, Map<String, String> output, boolean needDownload) {
		Map<String, String> notFound = new HashMap<>();
		dependencies.forEach((k, v) -> {
			JSONObject json = allDepends.get(k);
			if (json == null || !json.getString("specifiedVersion").equals(v)) {
				notFound.put(k, v);
			} else {
				if (!json.has("version")) {
					notFound.put(k, v);
					return;
				}
				output.put(k, json.getString("version"));
				addDependencies(output, notFound, allDepends, json);
			}
		});

		if (needDownload && !notFound.isEmpty()) {
			JSONObject result = getDependenciesFromServer(ext, client, notFound);
			Map<String, JSONObject> depends = new HashMap<>();
			result.keySet().forEach(e -> {
				JSONObject jar = result.getJSONObject(e);
				if (!jar.has("version")) {
					getLogger().error("Bundle not found:" + e);
				}
				depends.put(e, jar);
			});

			prepareJarDependencies(ext, client, depends, notFound, output, false);
			allDepends.putAll(depends);
			return true;
		}

		return false;
	}

	private void addDependencies(Map<String, String> output, Map<String, String> notFound,
			Map<String, JSONObject> allDepends, JSONObject json) {
		// Exists, add all dependencies
		JSONArray array = json.getJSONArray("dependents");
		array.forEach(a -> {
			JSONObject obj = allDepends.get(a.toString());
			if (!obj.has("version")) {
				notFound.put(a.toString(), obj.getString("specifiedVersion"));
				return;
			}
			String version = obj.getString("version");
			output.put(a.toString(), version);
			addDependencies(output, notFound, allDepends, obj);
		});

	}

	private JSONObject getDependenciesFromServer(EcgineExtension ext, HttpClient client,
			Map<String, String> notExisting) {
		try {
			String url = ext.getDependenciesUrl();
			getLogger().info("Downloading dependencies: " + url);
			HttpPost request = new HttpPost(url);
			request.addHeader("apikey", ext.getApiKey());
			JSONObject obj = new JSONObject();
			notExisting.forEach(obj::put);
			request.setEntity(new StringEntity(obj.toString()));
			HttpResponse response = client.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code != 200) {
				EntityUtils.consume(response.getEntity());
				throw new GradleException("StatusCode:" + code + " URL:" + url);
			}
			String json = EntityUtils.toString(response.getEntity());
			JSONObject result = new JSONObject(json);
			getLogger().info("Got dependencies");
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private File downloadBundle(EcgineExtension ext, HttpClient client, String name, String version) {
		File jar = new File(ext.getPlugins(), name + "_" + version + ".jar");
		if (jar.exists()) {
			return jar;
		}

		try {
			getLogger().info("Downloading bundle " + jar.getName());
			String url = ext.getDownloadUrl();
			HttpPost request = new HttpPost(url);
			request.addHeader("apikey", ext.getApiKey());
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("name", name));
			urlParameters.add(new BasicNameValuePair("version", version));
			request.setEntity(new UrlEncodedFormEntity(urlParameters));
			HttpResponse response = client.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code == 200) {
				IOUtils.copy(response.getEntity().getContent(), new FileOutputStream(jar));
			} else {
				EntityUtils.consume(response.getEntity());
				getLogger().error("Bundle not found in ecgine repository " + jar.getName());
			}
		} catch (Exception e) {
			throw new GradleException("", e);
		}
		return jar;
	}
}
