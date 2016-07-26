package org.ecgine.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.ecgine.gradle.extensions.EcgineExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class EcgineUtils {
	public static Map<String, JSONObject> readJarDependencies(Project project) {
		File ecgine = getDependenciesFile(project);
		if (!ecgine.exists()) {
			project.getLogger().debug(".ecgine not found:" + ecgine.getAbsolutePath());
			return new HashMap<>();
		}

		try {
			String string = new String(Files.readAllBytes(ecgine.toPath()));
			JSONObject json = new JSONObject(string);
			Map<String, JSONObject> all = new HashMap<>();
			json.keySet().forEach(k -> {
				all.put(k, json.getJSONObject(k));
			});
			return all;
		} catch (Exception e) {
			throw new GradleException("Unable to read .ecgine file", e);
		}
	}

	private static File getDependenciesFile(Project project) {
		EcgineExtension ext = (EcgineExtension) project.getExtensions().getByName(EcgineExtension.NAME);
		File plugins = new File(ext.getPlugins());
		if (!plugins.exists()) {
			plugins.mkdirs();
		}
		File ecgine = new File(plugins, ".ecgine");
		return ecgine;
	}

	public static void updateJarDependencies(Logger logger, Project project, Map<String, JSONObject> existing) {
		File ecgine = getDependenciesFile(project);
		JSONObject json = new JSONObject();
		existing.forEach(json::put);
		try (FileWriter fw = new FileWriter(ecgine)) {
			fw.write(json.toString());
		} catch (Exception e) {
			throw new GradleException("Unable to write file " + ecgine.getName(), e);
		}
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
				if (jar.trim().isEmpty()) {
					continue;
				}
				jars.add(jar.trim());
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

	public static EManifest readManifest(Project p) {
		File file = new File(p.getProjectDir(), "META-INF/MANIFEST.MF");
		if (file.exists()) {
			return new EManifest(p, file);
		}
		return null;
	}

	public static Set<EManifest> getAllProjects(Project project, Predicate<EManifest> test) {
		return project.getAllprojects().stream().map(EcgineUtils::readManifest).filter(Objects::nonNull).filter(test)
				.collect(Collectors.toSet());
	}

	public static Map<String, String> getDependencies(Set<EManifest> devBundles, Project project) {
		Map<String, String> result = new HashMap<>();
		Map<String, JSONObject> dependencies = readJarDependencies(project);
		devBundles.forEach(m -> m.foreachRequiredBundle((n, v) -> addDependencies(n, v, result, dependencies)));
		return result;
	}

	private static void addDependencies(String n, String v, Map<String, String> result,
			Map<String, JSONObject> dependencies) {
		JSONObject json = dependencies.get(n);
		if (json == null) {
			return;
		}

		if (!json.has("version")) {
			return;
		}

		String version = json.getString("specifiedVersion");
		if (v != null && !version.equals(v)) {
			return;
		}

		JSONArray array = json.getJSONArray("dependents");
		array.forEach(a -> {
			addDependencies(a.toString(), null, result, dependencies);
		});
	}

}
