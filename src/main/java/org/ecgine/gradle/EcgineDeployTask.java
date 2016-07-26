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
import org.apache.http.StatusLine;
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
import org.ecgine.gradle.extensions.Package;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.json.JSONObject;

import com.amazonaws.util.json.JSONArray;

/**
 *
 * create EBundle for each dev-bundle,Calculate all dependencies,Create
 * PackageVersion,if fail then Create Package and again Create PackageVersion.
 * 
 * @author lingarao
 *
 */
public class EcgineDeployTask extends DefaultTask {

	private static final String VERSION = "versionName";
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

		Package pak = (Package) getProject().getExtensions().getByName("package");

		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);

		Set<EManifest> projects = EcgineUtils.getAllProjects(getProject(), m -> true);

		Set<Bundle> bundles = EcgineUtils.getBundles(projects, getProject());

		projects.forEach(m -> {
			try {
				createEbundle(client, ext, m);
			} catch (Exception e) {
				throw new GradleException(e.getMessage(), e);
			}
			bundles.add(new Bundle(m.getSymbolicName(), m.getVersion(), m.getEcgineBundleType()));
		});

		createPackageVersion(client, ext, pak, bundles);

	}

	private void createEbundle(HttpClient client, EcgineExtension ext, EManifest p) throws Exception {
		HttpPost post = new HttpPost(ext.getUploadBundleUrl());
		post.addHeader("apikey", ext.getApiKey());
		post.removeHeaders("content-type");
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		File file = p.getJar();
		builder.addBinaryBody("upfile", file, ContentType.DEFAULT_BINARY, file.getName());
		JSONObject json = new JSONObject();
		json.put("multiple", false);
		builder.addPart("info", new StringBody(json.toString(), ContentType.APPLICATION_JSON));
		post.setEntity(builder.build());
		HttpResponse response = client.execute(post);
		StatusLine status = response.getStatusLine();
		HttpEntity entity = response.getEntity();
		EntityUtils.consume(entity);
		if (status.getStatusCode() != HttpStatus.SC_OK) {
			throw new GradleException("StatusCode:" + status.getStatusCode() + " URL:" + ext.getUploadBundleUrl());
		}
	}

	private void createPackageVersion(HttpClient client, EcgineExtension ext, Package pkg, Set<Bundle> bundles)
			throws Exception {
		HttpPost request = new HttpPost(ext.getCreatePackageVersionUrl());
		request.addHeader("apikey", ext.getApiKey());
		// preparing body
		JSONObject body = new JSONObject();
		body.put(PACKAGE_NAME_SPACE, pkg.getNamespace());
		body.put(VERSION, pkg.getVersionName());
		JSONArray bundlesArray = new JSONArray();
		body.put(BUNDLES, bundlesArray);
		bundles.forEach(b -> {
			JSONObject bundle = new JSONObject();
			bundle.put(NAME, b.getName());
			bundle.put(VERSION, b.getVersion());
			bundle.put(BUNLDE_TYPE, b.getType());
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
			createPackage(client, ext, pkg);
			createPackageVersion(client, ext, pkg, bundles);
			break;
		case FAILED:
			System.err.println("Unable to create PackageVersion");
			System.err.println(result.getString("message"));
			break;
		case SUCESS:
		}
	}

	private void createPackage(HttpClient client, EcgineExtension ext, Package ePackage) throws Exception {
		System.out.println("Creating Package...");
		HttpPost request = new HttpPost(ext.getCreatePackageUrl());
		request.addHeader("apikey", ext.getApiKey());
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(NAME, ePackage.getName()));
		urlParameters.add(new BasicNameValuePair(PACKAGE_NAME_SPACE, ePackage.getNamespace()));
		urlParameters.add(new BasicNameValuePair(CATEGORY, ePackage.getCategory()));
		urlParameters.add(new BasicNameValuePair(VERTICALS, ePackage.getVerticals()));
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
