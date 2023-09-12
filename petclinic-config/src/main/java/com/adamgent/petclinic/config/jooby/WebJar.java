package com.adamgent.petclinic.config.jooby;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import io.jooby.Router;
import io.jooby.handler.AssetSource;

public interface WebJar {

	String groupId();

	String artifactId();

	public static Builder builder() {
		return new Builder();
	}

	public class Builder {

		private List<WebJar> jars = new ArrayList<>();

		private Builder() {
			super();
		}

		public Builder add(String groupId, String artifactId) {
			jars.add(WebJar.of(groupId, artifactId));
			return this;
		}

		public <R extends Router> R install(R j, ClassLoader classLoader) {
			return install(j, classLoader, "/webjars");
		}

		<R extends Router> R install(R j, ClassLoader classLoader, String path) {
			for (var jar : jars) {
				jar.install(j, classLoader, path);
			}
			return j;
		}

	}

	public static WebJar of(String groupId, String artifactId) {
		return new DefaultWebJar(groupId, artifactId);
	}

	default <R extends Router> R install(R j, ClassLoader classLoader) {
		return install(j, classLoader, "");
	}

	default <R extends Router> R install(R j, ClassLoader classLoader, String path) {
		var source = webjar(classLoader);
		j.assets(path + path(), source);
		return j;
	}

	private AssetSource webjar(ClassLoader loader) {
		String version = version(loader);
		String source = sourcePath(version);
		return AssetSource.create(loader, source);
	}

	private String path() {
		return "/" + artifactId() + "/*";
	}

	private String mavenVersionPath() {
		return "META-INF/maven/" + groupId() + "/" + artifactId() + "/pom.properties";
	}

	private String sourcePath(String version) {
		return "META-INF/resources/webjars/" + artifactId() + "/" + version;
	}

	private String version(ClassLoader loader) {
		String versionPath = mavenVersionPath();
		try (InputStream in = loader.getResourceAsStream(versionPath)) {
			requireNonNull(in, versionPath);
			Properties properties = new Properties();
			properties.load(in);
			String version = properties.getProperty("version");
			requireNonNull(version);
			return version;
		}
		catch (IOException io) {
			throw new IllegalStateException(io);
		}
	}

}

record DefaultWebJar(String groupId, String artifactId) implements WebJar {
}
