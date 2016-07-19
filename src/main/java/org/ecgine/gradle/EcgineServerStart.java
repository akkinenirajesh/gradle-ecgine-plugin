package org.ecgine.gradle;

import org.ecgine.gradle.extensions.Configuration;
import org.ecgine.gradle.extensions.Master;

@SuppressWarnings("unchecked")
public class EcgineServerStart extends AbstractStart {

	@Override
	protected void exec() {
		Master master = (Master) getProject().getExtensions().getByName("master");
		Configuration cfg = (Configuration) getProject().getExtensions().getByName("server");
		cfg.property("ecgine.vimukti.create.default", master.toProperty());
		prepareSetup("server");
	}

	@Override
	protected boolean filterDevBundle(EManifest manifest) {
		return manifest.isServer() || manifest.isShared();
	}

}
