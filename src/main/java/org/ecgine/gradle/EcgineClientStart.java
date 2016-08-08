package org.ecgine.gradle;

import java.io.File;

import org.ecgine.gradle.extensions.EcgineExtension;

@SuppressWarnings("unchecked")
public class EcgineClientStart extends AbstractStart {

	@Override
	protected void exec() {
		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);
		File plugins = new File(ext.getPlugins());
		if (!plugins.exists()) {
			plugins.mkdirs();
		}
		File ecgineStart = new File(plugins, "ecgine-start.jar");
		if (!ecgineStart.exists()) {
			downloadConfigFile(ext.getHttpClient(),ecgineStart, ext.getEcgineStartUrl());
		}
		prepareSetup(ext, ext.getClient(), "client");
	}

	@Override
	protected boolean filterDevBundle(EManifest manifest) {
		return manifest.isClient() || manifest.isShared();
	}
}
