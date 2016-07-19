package org.ecgine.gradle;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.ecgine.gradle.extensions.Configuration;
import org.ecgine.gradle.extensions.EcgineExtension;
import org.ecgine.gradle.extensions.Master;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.osgi.OsgiPlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;

/**
 *
 */
public class EcginePlugin implements Plugin<Project> {

	private Project project;

	@Override
	public void apply(final Project project) {
		this.project = project;

		// We need the node plugin to run and install the react-tools.
		project.getPlugins().apply(JavaPlugin.class);
		project.getPlugins().apply(EclipsePlugin.class);
		project.getPlugins().apply(OsgiPlugin.class);

		// Add the install task to this project.
		// project.tasks.create(ReactInstallTask.NAME, ReactInstallTask.class )

		// replace the jar task to this project.
		// project.getTasks().remove(project.getTasks().getByName("jar"));
		project.getTasks().getByName("jar").doFirst(this::jar);

		project.getTasks().getByName("compileJava").doFirst(this::compileJava);

		project.getTasks().create("bundles", EcgineBundlesTask.class);

		project.getTasks().create("ecgineClientStart", EcgineClientStart.class);

		project.getTasks().create("ecgineServerStart", EcgineServerStart.class);

		// adding the task to the extra properties makes it available as task
		// type in this project.
		// addGlobalTaskType(JSXTask.class)

		// create the extension to configure the tasks
		project.getExtensions().create(EcgineExtension.NAME, EcgineExtension.class);
		project.getExtensions().create("server", Configuration.class, 4000, 2502, "64m", "1g");
		project.getExtensions().create("client", Configuration.class, 8000, 2501, "64m", "1g");
		project.getExtensions().create("master", Master.class);
	}

	private void compileJava(Task t) {

		JavaCompile c = (JavaCompile) t;
		EcgineExtension ext = (EcgineExtension) project.getExtensions().getByName("ecgine");

		Map<String, String> dependencies = ext.getBundles();
		Map<String, String> jarDepends = EcgineUtils.readJarDependencies(c.getLogger(), project);
		Set<String> allJarNames = EcgineUtils.combineAllJars(c.getLogger(), jarDepends, dependencies);

		FileCollection allJars = project.files(allJarNames.stream().map(s -> new File(s + ".jar")).toArray());

		// Existing jars
		allJars.plus(c.getClasspath());

		c.setClasspath(allJars);
	}

	private void jar(Task t) {
		File propsFile = new File(project.getProjectDir(), "build.properties");
		if (!propsFile.exists()) {
			throw new GradleException("Can not create jar, 'build.properties' file is missing in project folder.");
		}
		Jar jar = (Jar) t;
		jar.getManifest().from("META-INF/MANIFEST.MF");

		Properties props = new Properties();
		try {
			props.load(new FileReader(propsFile));
		} catch (IOException e) {
			throw new GradleException("Unable to read properties file.", e);
		}
		jar.from("./.").setIncludes(Arrays.asList(props.getProperty("bin.includes").split(",")));
		jar.setDestinationDir(new File(project.getRootDir(), "build"));
	}
}
