package org.ecgine.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.ecgine.gradle.extensions.EcgineExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class EcgineUtils {
	public static Map<String, String> readJarDependencies(Logger logger, Project project) {
		File ecgine = getDependenciesFile(project);
		if (!ecgine.exists()) {
			logger.debug(".ecgine not found:" + ecgine.getAbsolutePath());
			return new HashMap<>();
		}

		Map<String, String> existing = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(ecgine))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split("=");
				existing.put(split[0], split[1]);
			}
		} catch (Exception e) {
			throw new GradleException("Unable to read .ecgine file", e);
		}
		return existing;
	}

	private static File getDependenciesFile(Project project) {
		EcgineExtension ext = (EcgineExtension) project.getExtensions().getByName(EcgineExtension.NAME);
		File ecgine = new File(ext.getPlugins(), ".ecgine");
		return ecgine;
	}

	public static void updateJarDependencies(Logger logger, Project project, Map<String, String> existing) {
		File ecgine = getDependenciesFile(project);
		writeProperties(logger, ecgine, existing);
	}

	public static void writeProperties(Logger logger, File file, Map<String, String> properties) {
		try (FileWriter fw = new FileWriter(file)) {
			properties.forEach((n, v) -> {
				try {
					fw.write(n + "=" + v + "\n");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Exception e) {
			throw new GradleException("Unable to write file " + file.getName(), e);
		}
	}

	public static Set<String> combineAllJars(Logger logger, Map<String, String> jarDepends,
			Map<String, String> dependencies) {
		Set<String> jars = new HashSet<>();
		dependencies.forEach((n, v) -> {
			// add self
			jars.add(n + "_" + v);

			// Get it's dependencies
			String dep = jarDepends.get(n + "_" + v);
			if (dep == null) {
				return;
			}
			String[] split = dep.split(",");
			for (String jar : split) {
				jars.add(jar);
			}
		});
		return jars;
	}

	public static boolean copy(File source, File dest) {
		if (!source.exists()) {
			return false;
		}
		try {
			Files.copy(source.toPath(), new FileOutputStream(dest));
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static void forEachProject(Project root, Consumer<Project> project) {
		root.getAllprojects().forEach(project);
	}

	public static EManifest readManifest(Project p) {
		File file = new File(p.getProjectDir(), "META-INF/MANIFEST.MF");
		if (file.exists()) {
			return new EManifest(p, file);
		}
		throw new GradleException("Invalid plugin project. Manifest file not found");
	}

	public static boolean copyProjectJar(EManifest mf, File plugins) {
		String name = mf.getSymbolicName();
		Project p = mf.getProject();
		File jar = new File(p.getBuildDir(), name + ".jar");
		return copy(jar, new File(plugins, jar.getName()));
	}

}
