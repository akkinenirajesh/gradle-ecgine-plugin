package org.ecgine.gradle;

import org.ecgine.gradle.extensions.Configuration;
import org.ecgine.gradle.extensions.EcgineExtension;

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

}
