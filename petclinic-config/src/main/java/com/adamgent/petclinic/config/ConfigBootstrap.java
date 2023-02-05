package com.adamgent.petclinic.config;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.System.Logger;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

public class ConfigBootstrap {

	private static final String DEFAULT_PROFILE = "default";

	private final String applicationName;

	private final Consumer<ConfigEvent> eventConsumer;

	private final ConcurrentLinkedQueue<ConfigEvent> events = new ConcurrentLinkedQueue<>();

	private final File userConfigDir;

	private final String profilesProperty = "profile";

	private final String envName;

	private final String envNamePrefix;

	private @Nullable Map<String, String> properties = null;

	public ConfigBootstrap(String applicationName) {
		super();
		this.applicationName = applicationName;
		this.eventConsumer = events::add;
		userConfigDir = new File(System.getProperty("user.home"), ".config" + File.separator + applicationName);
		envName = applicationName.replace(".", "_").replace("-", "_").toUpperCase();
		envNamePrefix = envName + "_";

	}

	public static Map<String, String> load(String applicationName) {

		var bootstrap = new ConfigBootstrap(applicationName);
		var properties = bootstrap.load();
		bootstrap.log();
		return properties;
	}

	public void log() {

		var events = events();
		if (events.isEmpty()) {
			return;
		}

		var properties = getProperties();

		/*
		 * We initialize slf4j before
		 */
		LoggerFactory.getLogger(ConfigBootstrap.class).info("SLF4J Loaded");

		var logger = System.getLogger(ConfigBootstrap.class.getCanonicalName());

		/*
		 * Replay log events
		 */
		for (var e : events) {
			e.log(logger);
		}

		logger.log(Logger.Level.INFO, "Configuration and Logging loaded");

		StringBuilder sb = new StringBuilder();
		sb.append("Loaded following properties:");
		for (String k : properties.keySet()) {
			sb.append("\n\t").append(k);
		}

		logger.log(Logger.Level.INFO, sb.toString());
		// var args = ManagementFactory.getRuntimeMXBean().getInputArguments();
		// logger.log(Logger.Level.INFO, args);
		// logger.log(Logger.Level.INFO, System.getProperty("sun.java.command"));

	}

	public Map<String, String> toEnvironmentVariables() {
		return toEnvironmentVariables(envNamePrefix, getProperties());
	}

	public static Map<String, String> toEnvironmentVariables(String prefix, Map<String, String> properties) {
		Map<String, String> env = new LinkedHashMap<>();
		for (var e : properties.entrySet()) {
			env.put(prefix + propertyNameToEnvironmentVariable(e.getKey()), e.getValue());
		}
		return env;
	}

	public Map<String, String> getProperties() {
		var p = properties;
		return Objects.requireNonNull(p, "Properties have not been loaded yet");
	}

	public ConcurrentLinkedQueue<ConfigEvent> events() {
		return this.events;
	}

	protected Properties systemProperties() {
		return System.getProperties();
	}

	protected Map<String, String> systemEnv() {
		return System.getenv();
	}

	public Map<String, String> load() {

		/*
		 * We have this whole event system because we want to load config before we load
		 * the logging sytem
		 */
		events().clear();

		var config = readConfig();
		var systemProps = systemProperties();

		Map<String, String> properties = toMap(config);
		Map<String, String> resolved = new LinkedHashMap<>();

		synchronized (systemProps) {
			for (var e : properties.entrySet()) {
				if (!systemProps.containsKey(e.getKey())) {
					systemProps.put(e.getKey(), e.getValue());
					resolved.put(e.getKey(), e.getValue());
				}
				else {
					resolved.put(e.getKey(), systemProps.getProperty(e.getKey()));
				}
			}
		}
		this.properties = resolved;
		return resolved;

	}

	public static Function<String, String> propertySupplier(String prefix, Map<String, String> m) {
		return (s) -> {
			var k = prefix + s;
			String v = m.get(k);
			if (v == null) {
				throw new NullPointerException("missing property: " + k);
			}
			return v;
		};
	}

	Config readConfig() {

		/*
		 * I think typesafe aka lightbend config... I am not a fan but jooby uses it. Like
		 * why does it not interpolate properties files?
		 *
		 * We load up all the config and convert it to Map<String,String> to feed to other
		 * configuration systems.
		 */

		/*
		 * We parse environment variables beginning with a prefix converting them to
		 * properties by making them lower case and use "." for "_". We also add the
		 * unaltered as a property.
		 *
		 * Thus an environment variable of PREFIX_DATABASE_URL would get two properties
		 *
		 * database.url DATABASE_URL
		 */
		new ConfigEvent.Init(applicationName).publish(eventConsumer);

		Config envConfig = parseEnv(envNamePrefix);

		Config defaultConfig = parseBuiltin(DEFAULT_PROFILE);

		Config userConfig = parseUserFile(DEFAULT_PROFILE);

		/*
		 * n.b. how we do not add system properties. we are only interested in our
		 * explicit config right now otherwise the final Map<String,String> will have all
		 * the system properties in it.
		 */
		List<Config> configs = List.of(userConfig, envConfig, defaultConfig);

		Config resolved = ConfigFactory.empty();

		for (var c : configs) {
			resolved = resolved.withFallback(c);
		}

		Config systemPropertiesConfig = ConfigFactory.systemProperties();
		resolved = resolved.resolveWith(systemPropertiesConfig.withFallback(resolved));

		/*
		 * Now we resolve the profile string.
		 */
		String profilesString;

		if (systemPropertiesConfig.hasPath(profilesProperty)) {
			profilesString = systemPropertiesConfig.getString(profilesProperty);
		}
		else {
			profilesString = resolved.getString(profilesProperty);
		}

		if (profilesString.isBlank()) {
			throw new RuntimeException("profiles property \"" + profilesProperty + "\" is required and not blank");
		}

		/*
		 * Now we resolve profiles
		 */
		List<String> profiles = Stream.of(profilesString.split(",")).map(s -> s.trim()).toList();

		new ConfigEvent.Profiles(profiles).publish(eventConsumer);

		/*
		 * Now we load profile specific configuration ignoring the already loaded default
		 */
		for (var p : profiles) {
			if (p.equals(DEFAULT_PROFILE)) {
				continue;
			}
			var c = parseBuiltin(p);
			resolved = resolved.withFallback(c);
		}

		for (var p : profiles) {
			if (p.equals(DEFAULT_PROFILE)) {
				continue;
			}
			var c = parseUserFile(p);
			resolved = resolved.withFallback(c);
		}

		return resolved;
	}

	private Config parseEnv(String envNamePrefix) {
		var envProperties = propertiesFromEnv(systemEnv(), envNamePrefix, "");

		new ConfigEvent.Env(Instant.now(), envNamePrefix, envProperties).publish(eventConsumer);

		Config envConfig = ConfigFactory.parseMap(envProperties, "environment");
		return envConfig;
	}

	private Config parseUserFile(String file) {
		File userFile = new File(userConfigDir, file + ".conf");
		ConfigEvent.load(userFile.toURI().toString()).publish(eventConsumer);
		Config userConfig = ConfigFactory.parseFile(userFile, ConfigParseOptions.defaults().setAllowMissing(true));
		return userConfig;
	}

	private Config parseBuiltin(String conf) {
		String configPath = "config/" + conf + ".conf";
		ConfigEvent.load("classpath://" + configPath, true).publish(eventConsumer);
		return ConfigFactory.parseResources(ConfigBootstrap.class.getClassLoader(), configPath,
				ConfigParseOptions.defaults().setAllowMissing(false));
	}

	public interface ConfigEvent {

		default void publish(Consumer<ConfigEvent> consumer) {
			consumer.accept(this);
		}

		public void log(Logger logger);

		public static Load load(String file) {
			return new Load(Instant.now(), file, false);
		}

		public static Load load(String file, boolean required) {
			return new Load(Instant.now(), file, required);
		}

		public record Init(String applicationName) implements ConfigEvent {
			public void log(Logger logger) {
				logger.log(Logger.Level.INFO, "Starting application \"" + applicationName + "\"");
			};
		}

		public record Load(Instant time, String file, boolean required) implements ConfigEvent {
			@Override
			public void log(Logger logger) {
				logger.log(Logger.Level.INFO, "Load: \"" + file + "\" required=" + required);

			}
		}

		public record Profiles(List<String> profiles) implements ConfigEvent {

			@Override
			public void log(Logger logger) {
				logger.log(Logger.Level.INFO, "Profiles: " + profiles.toString());
			}
		}

		public record Env(Instant time, String prefix, Map<String, String> env) implements ConfigEvent {
			@Override
			public void log(Logger logger) {
				StringBuilder sb = new StringBuilder();
				sb.append("Converting ENV variables with prefix=\"").append(prefix).append("\"");
				sb.append(" to properties");
				if (env.isEmpty()) {
					sb.append(".");
				}
				else {
					sb.append(":");
				}
				for (var e : env.entrySet()) {
					sb.append("\n\t").append(e.getKey());
				}
				logger.log(Logger.Level.INFO, sb.toString());

			}
		}

	}

	static Map<String, String> toMap(Config config) {
		Map<String, String> m = new LinkedHashMap<>();
		for (var e : config.entrySet()) {
			String key = e.getKey();
			String value = config.getString(key);
			m.put(key, value);
		}
		return m;
	}

	public static String environmentVariableToPropertyName(String p) {
		String property = p.replaceAll("([^_])_([^_])", "$1.$2").replaceAll("([^_])_(_+)([^_])", "$1$2$3");
		// If its all upper case we convert to lower case and its probably a
		// windows
		// environment.
		// If its mixed case or its all lower case its probably a unix
		// environment.
		if (property.toUpperCase().equals(property)) {
			property = property.toLowerCase();
		}
		return property;
	}

	public static String propertyNameToEnvironmentVariable(String n) {
		if (n.toLowerCase().equals(n)) {
			n = n.toUpperCase();
		}
		return n.replace(".", "_");
	}

	public static Map<String, String> propertiesFromEnv(Map<String, String> env, String prefix, String replacement) {
		Map<String, String> props = new LinkedHashMap<>();
		for (Entry<String, String> e : env.entrySet()) {
			String key = e.getKey();
			if (key.startsWith(prefix)) {
				String propertyKey = removeStart(key, prefix);
				String property = environmentVariableToPropertyName(replacement + propertyKey);
				props.put(property, e.getValue());
				props.put(replacement + propertyKey, e.getValue());
			}
		}
		return props;
	}

	private static String removeStart(final String str, final String remove) {
		if (str.startsWith(remove)) {
			return str.substring(remove.length());
		}
		return str;
	}

	public static void writeEnv(Map<String, String> map, Appendable sb) throws IOException {
		for (var e : map.entrySet()) {
			sb.append(e.getKey()).append("=").append("\"").append(e.getValue()).append("\"");
			sb.append("\n");
		}
	}

	public static void writeProperties(Map<String, String> map, Appendable sb) throws IOException {
		StringWriter sw = new StringWriter();
		new Properties() {
			private static final long serialVersionUID = 1L;

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public java.util.Enumeration keys() {
				return Collections.enumeration(map.keySet());
			}

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public java.util.Set entrySet() {
				return map.entrySet();
			}

			@Override
			public Object get(Object key) {
				return map.get(key);
			}

			public String getProperty(String key) {
				return map.get(key);
			}
		}.store(sw, null);
		sw.flush();
		sw.close();

		var list = sw.toString().lines().filter(line -> !line.startsWith("#")).toList();

		for (String line : list) {
			sb.append(line).append(System.lineSeparator());
		}

	}

}
