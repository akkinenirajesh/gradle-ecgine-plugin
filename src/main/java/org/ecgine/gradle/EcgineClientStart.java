package org.ecgine.gradle;

@SuppressWarnings("unchecked")
public class EcgineClientStart extends AbstractStart {

	@Override
	protected void exec() {
		prepareSetup("client");
	}

	@Override
	protected boolean filterDevBundle(EManifest manifest) {
		return manifest.isClient() || manifest.isShared();
	}
}
