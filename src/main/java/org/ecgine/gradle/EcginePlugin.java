package org.ecgine.gradle;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.ecgine.gradle.extensions.EcgineExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.osgi.OsgiPlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xtend.gradle.XtendPlugin;
import org.xtend.gradle.tasks.XtendCompile;

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
		project.getPlugins().apply(XtendPlugin.class);

		// Add the install task to this project.
		// project.tasks.create(ReactInstallTask.NAME, ReactInstallTask.class )

		// replace the jar task to this project.
		if (project.getRootProject() == project) {
			project.getTasks().create("ecgineLogin", EcgineLoginTask.class);
			project.getTasks().create("ecgineDeploy", EcgineDeployTask.class);
			project.getTasks().create("ecginePrepare", EcgineBundlesTask.class);
			project.getTasks().create("ecgineClientStart", EcgineClientStart.class);
			project.getTasks().create("ecgineServerStart", EcgineServerStart.class);

			project.getExtensions().create(EcgineExtension.NAME, EcgineExtension.class, project);
			project.getTasks().remove(project.getTasks().getByName("build"));
		} else {
			project.getTasks().getByName("jar").doFirst(this::jar);
			project.getTasks().getByName("compileXtend").doFirst(this::compileXtend);
			project.getTasks().getByName("compileJava").doFirst(this::compileJava);
		}
	}

	private void compileXtend(Task t) {

		XtendCompile c = (XtendCompile) t;

		List<File> existed = new ArrayList<>();

		// Existing jars
		c.getXtendClasspath().forEach(existed::add);
		c.getClasspath().forEach(existed::add);

		List<File> collect = collectAllJars(t, existed);
		collect.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));
		FileCollection allJars = project.files(collect);

		c.setClasspath(allJars);
	}

	private void compileJava(Task t) {

		AbstractCompile c = (AbstractCompile) t;

		List<File> existed = new ArrayList<>();
		// Existing jars
		c.getClasspath().forEach(existed::add);

		List<File> collect = collectAllJars(t, existed);
		FileCollection allJars = project.files(collect);

		c.setClasspath(allJars);
	}

	private List<File> collectAllJars(Task c, List<File> existed) {
		Set<String> existedNames = new HashSet<>();
		List<File> all = new ArrayList<>();

		EcgineExtension root = (EcgineExtension) project.getRootProject().getExtensions().getByName("ecgine");
		File plugins = new File(project.getRootDir(), root.getPlugins());
		if (!plugins.exists()) {
			plugins.mkdirs();
		}
		Map<String, String> dependencies = root.getBundles();
		Map<String, JSONObject> jarDepends = EcgineUtils.readJarDependencies(project.getRootProject());

		Set<String> allJarNames = new HashSet<>();
		dependencies.forEach((k, v) -> {
			JSONObject json = jarDepends.get(k);
			if (json == null) {
				return;
			}
			if (!json.getString("specifiedVersion").equals(v)) {
				return;
			}
			if (!json.has("version")) {
				return;
			}
			addDependencies(jarDepends, allJarNames, existedNames, k, json);
		});
		allJarNames.stream().map(s -> new File(plugins, s + ".jar")).filter(f -> f.exists())
				.collect(Collectors.toCollection(() -> all));

		existed.stream().filter(f -> !existedNames.contains(f.getName().split("-")[0])).forEach(all::add);

		return all;
	}

	private void addDependencies(Map<String, JSONObject> jarDepends, Set<String> allJarNames, Set<String> existedNames,
			String k, JSONObject json) {
		if (!json.has("version")) {
			return;
		}

		// Exists, add all dependencies
		if (!existedNames.contains(k)) {
			allJarNames.add(k + "_" + json.getString("version"));
			existedNames.add(k);
		}

		JSONArray array = json.getJSONArray("dependents");
		array.forEach(a -> {
			JSONObject obj = jarDepends.get(a.toString());
			addDependencies(jarDepends, allJarNames, existedNames, a.toString(), obj);
		});
	}

	private final class Holder<T> {
		private T val;

		private Holder(T init) {
			val = init;
		}

		public T getVal() {
			return val;
		}

		public void setVal(T val) {
			this.val = val;
		}
	}

	private void jar(Task t) {
		File propsFile = new File(project.getProjectDir(), "build.properties");
		if (!propsFile.exists()) {
			throw new GradleException("Can not create jar, 'build.properties' file is missing in project folder.");
		}
		Jar jar = (Jar) t;
		Holder<Boolean> manifestExculde = new Holder<Boolean>(false);

		// We need to exclude existing manifest
		Iterator<CopySpecInternal> it = jar.getRootSpec().getChildren().iterator();
		it.hasNext();
		it.next().eachFile(f -> {
			// Need to skip first manifest(system)
			if (!manifestExculde.getVal()) {
				manifestExculde.setVal(true);
				f.exclude();
			}
		});

		jar.getMetaInf().from(new File("./META-INF/MANIFEST.MF"));
		try {
			Properties props = new Properties();
			props.load(new FileReader(propsFile));
			String includes = props.getProperty("bin.includes");
			String[] files = includes.split(",");
			File ecg = new File(project.getProjectDir(), "build/ecg");
			ecg.mkdirs();
			for (String s : files) {
				if (s.equals(".")) {
					continue;
				}
				File f = new File(project.getProjectDir(), s);
				File d = new File(ecg, s);
				if (f.isDirectory()) {
					FileUtils.copyDirectory(f, d);
				} else {
					FileUtils.copyFile(f, d);
				}
			}
			jar.from(ecg);
			// jar.from("./.").include(files);
		} catch (Exception e) {
			throw new GradleException("Unable to read properties file.", e);
		}
		jar.setArchiveName(project.getName() + ".jar");
		jar.setDestinationDir(new File(project.getRootDir(), "build"));
	}

}
