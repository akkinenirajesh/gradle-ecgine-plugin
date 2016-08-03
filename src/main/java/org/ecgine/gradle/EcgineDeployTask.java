package org.ecgine.gradle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.ecgine.gradle.extensions.EcgineExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * create EBundle for each dev-bundle,Calculate all dependencies,Create
 * PackageVersion,if fail then Create Package and again Create PackageVersion.
 * 
 * @author lingarao
 *
 */
public class EcgineDeployTask extends DefaultTask {

	private static final String VERSION = "version";
	private static final String PACKAGE_NAME_SPACE = "namespace";
	private static final String NAME = "name";
	private static final String CATEGORY = "category";
	private static final String VERTICALS = "verticals";
	private static final String BUNDLES = "bundles";
	private static final String BUNLDE_TYPE = "bundletype";
	// STATUS'S OF PACKAGE VERSION CREATION
	private static final int SUCESS = 1;
	private static final int PACKAGE_NOT_FOUND = 2;
	private static final int FAILED = 3;

	@TaskAction
	public void deploy() throws Exception {

		HttpClient client = HttpClientBuilder.create().build();

		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);

		Set<EManifest> projects = EcgineUtils.getAllProjects(getProject(), m -> !m.isUnknown());

		Set<Bundle> bundles = EcgineUtils.getBundles(projects, getProject());

		projects.forEach(m -> {
			try {
				createEbundle(client, ext, m);
			} catch (Exception e) {
				throw new GradleException(e.getMessage(), e);
			}
			bundles.add(new Bundle(m.getSymbolicName(), m.getVersion(), m.getEcgineBundleType()));
		});

		createPackageVersion(client, ext, bundles);

	}

	private void createEbundle(HttpClient client, EcgineExtension ext, EManifest p) throws Exception {
		HttpPost post = new HttpPost(ext.getUploadBundleUrl());
		post.addHeader("apikey", ext.getApiKey());
		post.removeHeaders("content-type");
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		File file = p.getJar();
		if (!file.exists()) {
			throw new GradleException("Run build before deploy");
		}
		builder.addBinaryBody("upfile", file, ContentType.DEFAULT_BINARY, file.getName());
		JSONObject json = new JSONObject();
		json.put("multiple", false);
		builder.addPart("info", new StringBody(json.toString(), ContentType.APPLICATION_JSON));
		post.setEntity(builder.build());
		HttpResponse response = client.execute(post);
		HttpEntity entity = response.getEntity();
		EntityUtils.consume(entity);
	}

	private void createPackageVersion(HttpClient client, EcgineExtension ext, Set<Bundle> bundles) throws Exception {
		HttpPost request = new HttpPost(ext.getCreatePackageVersionUrl());
		request.addHeader("apikey", ext.getApiKey());
		// preparing body
		JSONObject body = new JSONObject();
		body.put(PACKAGE_NAME_SPACE, ext.getPkg().getNamespace());
		body.put(VERSION, ext.getPkg().getVersion());
		JSONArray bundlesArray = new JSONArray();
		body.put(BUNDLES, bundlesArray);
		bundles.forEach(b -> {
			JSONObject bundle = new JSONObject();
			bundle.put(NAME, b.getName());
			bundle.put(VERSION, b.getVersion());
			bundle.put(BUNLDE_TYPE, b.getType().name().toLowerCase());
			bundlesArray.put(bundle);
		});
		request.setEntity(new StringEntity(body.toString()));
		HttpResponse response = client.execute(request);

		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			EntityUtils.consume(response.getEntity());
			throw new GradleException("StatusCode:" + response.getStatusLine().getStatusCode() + " URL:"
					+ ext.getCreatePackageVersionUrl());
		}

		JSONObject result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
		switch (result.getInt("code")) {
		case PACKAGE_NOT_FOUND:
			createPackage(client, ext);
			createPackageVersion(client, ext, bundles);
			break;
		case FAILED:
			System.err.println("Unable to create PackageVersion");
			throw new GradleException(result.getString("message"));
		case SUCESS:
		}
	}

	private void createPackage(HttpClient client, EcgineExtension ext) throws Exception {
		System.out.println("Creating Package...");
		HttpPost request = new HttpPost(ext.getCreatePackageUrl());
		request.addHeader("apikey", ext.getApiKey());
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(NAME, ext.getPkg().getName()));
		urlParameters.add(new BasicNameValuePair(PACKAGE_NAME_SPACE, ext.getPkg().getNamespace()));
		urlParameters.add(new BasicNameValuePair(CATEGORY, ext.getPkg().getCategory()));
		urlParameters.add(new BasicNameValuePair(VERTICALS, ext.getPkg().getVerticals()));
		request.setEntity(new UrlEncodedFormEntity(urlParameters));
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			EntityUtils.consume(response.getEntity());
			throw new GradleException(
					"StatusCode:" + response.getStatusLine().getStatusCode() + " URL:" + ext.getCreatePackageUrl());
		}
		JSONObject result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
		if (result.getInt("code") != SUCESS) {
			System.err.println("Unable to create package");
			throw new GradleException(result.getString("message"));
		}
	}
}
