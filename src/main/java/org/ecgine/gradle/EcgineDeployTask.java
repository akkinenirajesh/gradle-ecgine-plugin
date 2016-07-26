package org.ecgine.gradle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import com.sun.xml.internal.ws.api.message.Attachment;

/**
 *
 * create EBundle for each dev-bundle,Calculate all dependencies,Create
 * PackageVersion,if fail then Create Package and again Create PackageVersion.
 * 
 * @author lingarao
 *
 */
public class EcgineDeployTask extends DefaultTask {

	private static final String VERSION_NAME = "versionName";
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
	public void deploy() {

		HttpClient client = HttpClientBuilder.create().build();

		Package pak = (Package) getProject().getExtensions().getByName("package");

		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);

		Set<EManifest> projects = EcgineUtils.getAllProjects(getProject(), m -> true);

		projects.forEach(project -> createEbundle(client, ext, project));

		Map<String, String> dependencies = EcgineUtils.getDependencies(projects);
		projects.forEach(eManifest -> dependencies.put(eManifest.getSymbolicName(), eManifest.getVersion()));

		createPackageVersion(client, ext, pak, dependencies);

	}

	public String uploadFile() {
		HttpPost post = new HttpPost("");
		post.removeHeaders("content-type");
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addBinaryBody("upfile", file, ContentType.DEFAULT_BINARY, file.getName());
		builder.addPart("info", new StringBody(getJson().toString(), ContentType.APPLICATION_JSON));
		HttpEntity build = builder.build();
		entityWraper = new ProgressHttpEntityWrapper(build, calbak);
		post.setEntity(entityWraper);
		HttpResponse response = null;
		try {
			response = WSUtils.postRequest(post, httpClient);
		} catch (BundlesNotActivatedException e) {
			throw new AuthenticationFailedException(e.getMessage());
		}
		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() != HttpStatus.SC_OK) {
			throw new AuthenticationFailedException(status.getReasonPhrase());
		}
		HttpEntity entity = response.getEntity();
		String responseContent = IOUtils.toString(entity.getContent());
		JSONObject responseResult = new JSONObject(responseContent);
		JSONArray jsonArray = responseResult.getJSONArray("files");
		JSONObject obj = (JSONObject) jsonArray.get(0);
		String attId = obj.getString(ATTACHMENT_ID);
		String name = obj.getString(ATTACHMENT_NAME);
		int refcount = obj.getInt(ATTACHMENT_REFCOUNT);
		long size = obj.getLong(ATTACHMENT_SIZE);
		// database id
		long id = obj.getLong(SAVED_ATTACHEMENT_ID);
		Attachment att = new Attachment(UUID.fromString(attId), name, refcount);
		att.setId(id);
		att.setSize(size);
		return att;
	}

	private void createEbundle(HttpClient client, EcgineExtension ext, EManifest p) {

		uploadFile();
		
		try {
			HttpPost request = new HttpPost(EcgineUtils.getCreateEbundleUrl());
			request.addHeader("apikey", ext.getApiKey());
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			// TODO
			request.setEntity(new UrlEncodedFormEntity(urlParameters));
			HttpResponse response = client.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code == HttpStatus.SC_OK) {
			} else {
				EntityUtils.consume(response.getEntity());
			}
		} catch (Exception e) {
			throw new GradleException("", e);
		}
	}

	private void createPackageVersion(HttpClient client, EcgineExtension ext, Package ePackage,
			Map<String, String> dependencies) {
		try {
			HttpPost request = new HttpPost(EcgineUtils.getCreatePackageVersionUrl());
			request.addHeader("apikey", ext.getApiKey());
			// preparing body
			JSONObject body = new JSONObject();
			body.put(PACKAGE_NAME_SPACE, ePackage.getNamespace());
			body.put(VERSION_NAME, ePackage.getVersionName());
			JSONArray bundles = new JSONArray();
			body.put(BUNDLES, bundles);
			dependencies.forEach((k, v) -> {
				JSONObject bundle = new JSONObject();
				bundle.put(PACKAGE_NAME_SPACE, ePackage.getNamespace());
				bundle.put(VERSION_NAME, ePackage.getVersionName());
				bundle.put(BUNLDE_TYPE, getBundleType(k, v));
				bundles.put(bundle);
			});
			request.setEntity(new StringEntity(body.toString()));
			HttpResponse response = client.execute(request);

			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				System.out.println("Failed to created Package version");
				EntityUtils.consume(response.getEntity());
				return;
			}

			JSONObject result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
			switch (result.getInt("code")) {
			case SUCESS:
				System.out.println(result.getString("message"));
				break;
			case PACKAGE_NOT_FOUND:
				System.out.println(result.getString("message"));
				createPackage(client, ext, ePackage, dependencies);
				break;
			case FAILED:
				System.out.println(result.getString("message"));
				break;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new GradleException("", e);
		}
	}

	private String getBundleType(String name, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	private void createPackage(HttpClient client, EcgineExtension ext, Package ePackage,
			Map<String, String> dependencies) {
		try {
			HttpPost request = new HttpPost(EcgineUtils.getCreatePackageUrl());
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
				return;
			}
			JSONObject result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
			if (result.getInt("code") == SUCESS) {
				System.out.println(result.getString("message"));
				System.out.println("Creating Package version");
				createPackageVersion(client, ext, ePackage, dependencies);
			} else {
				System.out.println(result.getString("message"));
			}
		} catch (Exception e) {
			throw new GradleException("", e);
		}
	}
}
