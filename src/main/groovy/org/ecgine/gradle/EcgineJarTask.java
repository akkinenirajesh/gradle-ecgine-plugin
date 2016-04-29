package org.ecgine.gradle;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

public class EcgineJarTask extends Jar {

	public EcgineJarTask() {
		super();
	}

	@TaskAction
	@Override
	public void copy() {
		System.out.println("in Ecgine Jar Task");
		File propsFile = new File(getProject().getProjectDir(), "build.properties");
		if (!propsFile.exists()) {
			throw new GradleException("Can not create jar, 'build.properties' file is missing in project folder.");
		}
		// getManifest().from("META-INF/MANIFEST.MF");

		Properties props = new Properties();
		try {
			props.load(new FileReader(propsFile));
		} catch (IOException e) {
			throw new GradleException("Unable to read properties file.", e);
		}
		from("/.").setIncludes(Arrays.asList(props.getProperty("bin.includes").split(",")));
		from("bin/");
		from("resources/").into("resources");
		setDestinationDir(new File(getProject().getRootDir(), "build"));
		super.copy();
	}
}
