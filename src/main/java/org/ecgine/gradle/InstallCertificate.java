package org.ecgine.gradle;

import java.io.File;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.ecgine.gradle.extensions.EcgineExtension;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Exec;

public class InstallCertificate extends Exec {

	@Override
	protected void exec() {

		EcgineExtension ext = (EcgineExtension) getProject().getExtensions().getByName(EcgineExtension.NAME);

		HttpGet request = new HttpGet(ext.getUrl());
		try {
			HttpResponse response = ext.getHttpClient().execute(request);
			EntityUtils.consume(response.getEntity());
		} catch (Exception e) {
			if (!(e instanceof SSLHandshakeException)) {
				return;
			}
		} finally {
			request.completed();
		}

		System.out.println("Adding certificate to jre");
		// sudo keytool -import -alias ecgine.com -keystore
		// Java.runtime/Contents/Home/jre/lib/security/cacerts -file $2;
		File jdk = new File(System.getenv("JAVA_HOME"));
		if (!jdk.exists()) {
			throw new GradleException("JAVA_HOME not found");
		}
		File cert = new File(ext.getPlugins(), "certificate");

		// http://s1.infra.ecgine.com/certificate/vimukti_codegen_bundle.crt
		if (!cert.exists()) {
			AbstractStart.downloadConfigFile(ext.getHttpClient(), cert, ext.getCertificateUrl());
		}

		setCommandLine("keytool", "-import", "-alias", "ecgine.com", "-keystore", "../jre/lib/security/cacerts",
				"-file", cert.getAbsolutePath());
		setWorkingDir(new File(jdk, "bin"));
		super.exec();
	}
}
