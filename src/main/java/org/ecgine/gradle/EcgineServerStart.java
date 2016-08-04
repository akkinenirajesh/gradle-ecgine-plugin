package org.ecgine.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ecgine.gradle.extensions.Configuration;
import org.ecgine.gradle.extensions.EcgineExtension;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unchecked")
public class EcgineServerStart extends AbstractStart {

	@Override
	protected void exec() {
		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);
		Configuration cfg = ext.getServer();
		cfg.property("ecgine.vimukti.create.default", ext.getMaster().toProperty());
		prepareSetup(ext, cfg, "server");
	}

	@Override
	protected boolean filterDevBundle(EManifest manifest) {
		return manifest.isServer() || manifest.isShared();
	}

	@Override
	protected JSONObject getConfiguration(EcgineExtension ext, String type) throws IOException {
		JSONObject configuration = super.getConfiguration(ext, type);
		JSONArray masters = configuration.getJSONArray("master");
		HttpClient client = HttpClientBuilder.create().build();
		for (int i = 0; i < masters.length(); i++) {
			JSONObject master = masters.getJSONObject(i);
			EcgineBundlesTask.downloadBundle(ext, client, master.getString("symbolicName"),
					master.getString("bundleVersion"));
		}
		return configuration;
	}

	@Override
	protected List<String> prepareSetup(File plugins, Configuration con, String setup, String type, JSONObject config)
			throws IOException {
		List<String> prepareSetup = super.prepareSetup(plugins, con, setup, type, config);
		JSONArray masters = config.getJSONArray("master");
		File masterDir = new File(plugins, "master");
		if (!masterDir.exists()) {
			masterDir.mkdirs();
		}
		for (int i = 0; i < masters.length(); i++) {
			JSONObject master = masters.getJSONObject(i);
			File source = new File(plugins,
					master.getString("symbolicName") + "_" + master.getString("bundleVersion") + ".jar");
			File dest = new File(masterDir, master.getString("name"));
			Files.copy(source.toPath(), dest.toPath());
		}
		return prepareSetup;
	}
}
