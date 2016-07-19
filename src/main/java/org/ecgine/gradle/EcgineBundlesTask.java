package org.ecgine.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;
import org.json.JSONArray;
import org.json.JSONObject;

import groovy.lang.Closure;

public class EcgineBundlesTask extends DefaultTask {

	private HttpClient client;

	@Override
	public Task configure(Closure closure) {
		client = HttpClientBuilder.create().build();
		return super.configure(closure);
	}

	@TaskAction
	public void bundles() {
		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);

		// Get all project dependencies
		Map<String, String> dependencies = ext.getBundles();

		// Get all jar dependencies
		Map<String, String> jarDepends = calculateJarDependencies(ext, dependencies);

		// Combine all jars
		Set<String> allJars = EcgineUtils.combineAllJars(getLogger(), jarDepends, dependencies);

		allJars.forEach(j -> downloadBundle(ext, j));
	}

	private Map<String, String> calculateJarDependencies(EcgineExtension ext, Map<String, String> dependencies) {
		Map<String, String> fromFile = EcgineUtils.readJarDependencies(getLogger(), getProject());

		Map<String, String> existing = new HashMap<>();

		Map<String, String> notExisting = new HashMap<>();
		dependencies.forEach((n, v) -> {
			if (fromFile.containsKey(n + "_" + v)) {
				existing.put(n, v);
			} else {
				notExisting.put(n, v);
			}
		});

		if (!notExisting.isEmpty()) {
			Map<String, String> newDepends = getDependenciesFromServer(ext, notExisting);
			existing.putAll(newDepends);
		}

		if (!notExisting.isEmpty() || dependencies.size() != fromFile.size()) {
			EcgineUtils.updateJarDependencies(getLogger(), getProject(), existing);
		}

		return existing;
	}

	private Map<String, String> getDependenciesFromServer(EcgineExtension ext, Map<String, String> notExisting) {
		try {
			String url = ext.getDependenciesUrl();
			getLogger().info("Downloading dependencies: " + url);
			HttpPost request = new HttpPost(url);
			request.addHeader("apikey", ext.getApiKey());
			JSONArray array = new JSONArray();

			notExisting.forEach((n, v) -> {
				JSONObject obj = new JSONObject();
				obj.put("name", n);
				obj.put("version", v);
				array.put(obj);
			});
			request.setEntity(new StringEntity(array.toString()));
			HttpResponse response = client.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code != 200) {
				throw new GradleException("StatusCode:" + code + " URL:" + url);
			}
			JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity()));
			getLogger().debug("Got dependencies");
			Map<String, String> depends = new HashMap<>();
			result.keySet().forEach(e -> {
				depends.put(e, result.getString(e));
			});
			return depends;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private File downloadBundle(EcgineExtension ext, String bundle) {
		File jar = new File(ext.getPlugins(), bundle + ".jar");
		if (jar.exists()) {
			return jar;
		}

		try {
			getLogger().info("Downloading bundle " + jar.getName());
			String url = ext.getDownloadUrl();
			HttpPost request = new HttpPost(url);
			request.addHeader("apikey", ext.getApiKey());
			String[] split = bundle.split("_");
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("name", split[0]));
			urlParameters.add(new BasicNameValuePair("version", split[1]));
			request.setEntity(new UrlEncodedFormEntity(urlParameters));
			HttpResponse response = client.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code == 200) {
				IOUtils.copy(response.getEntity().getContent(), new FileOutputStream(jar));
			} else {
				getLogger().error("StatusCode:" + code + " URL:" + url);
			}
		} catch (Exception e) {
			throw new GradleException("", e);
		}
		return jar;
	}
}
