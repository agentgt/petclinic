package com.adamgent.petclinic.config;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.adamgent.petclinic.config.Config.KeyValue;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;

public class ConfigBootstrap {

	private static final String DEFAULT_PROFILE = "default";

	private final String applicationName;

	private final Consumer<ConfigEvent> eventConsumer;

	private final ConcurrentLinkedQueue<ConfigEvent> events = new ConcurrentLinkedQueue<>();

	private final File userConfigDir;

	private final String profilesProperty = "profile";

	private final String envName;

	private final String envNamePrefix;

	private com.adamgent.petclinic.config.@Nullable Config config;

	private final String[] commandLineArgs;

	public ConfigBootstrap(String applicationName, String[] commandLineArgs) {
		super();
		this.applicationName = applicationName;
		this.eventConsumer = events::add;
		userConfigDir = new File(System.getProperty("user.home"), ".config" + File.separator + applicationName);
		envName = applicationName.replace(".", "_").replace("-", "_").toUpperCase();
		envNamePrefix = envName + "_";
		this.commandLineArgs = commandLineArgs;
	}

	public ConfigBootstrap(String applicationName) {
		this(applicationName, commandLineArgsFromSystemProperties(System.getProperties()));
	}

	public static com.adamgent.petclinic.config.Config load(String applicationName) {

		var bootstrap = new ConfigBootstrap(applicationName);
		com.adamgent.petclinic.config.Config c = com.adamgent.petclinic.config.Config.empty();
		@Nullable
		Throwable error = null;
		try {
			c = bootstrap.load();
		}
		catch (Throwable e) {
			error = e;
		}
		bootstrap.log(error);
		return c;
	}

	public void log(@Nullable Throwable error) {

		var events = events();
		if (events.isEmpty()) {
			return;
		}

		if (isLoaded() && error == null) {
			var config = getConfig();

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
			for (var p : config) {
				sb.append("\n\t");
				sb.append(p.getValue().description());
			}

			logger.log(Logger.Level.INFO, sb.toString());
		}
		else {
			FailsafeLogger logger = new FailsafeLogger();
			/*
			 * Replay log events
			 */
			for (var e : events) {
				e.log(logger);
			}
			String errorMessage = "Configuration was not fully loaded. ";
			if (error != null) {
				logger.log(Level.ERROR, errorMessage, error);
			}
			else {
				logger.log(Level.ERROR, errorMessage);
			}

		}

	}

	private static class FailsafeLogger implements System.Logger {

		PrintStream out = System.err;

		@Override
		public String getName() {
			return ConfigBootstrap.class.getName();
		}

		@Override
		public boolean isLoggable(Level level) {
			return Level.DEBUG.getSeverity() <= level.getSeverity();
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
			_log(level, msg, thrown);
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String format, Object... params) {
			_log(level, format, null);

		}

		protected void _log(Level level, String msg, @Nullable Throwable thrown) {
			out.append("[").append(level.toString()).append("]");
			out.append(" ").append(msg);
			if (thrown != null) {
				out.append(" exception: ").append(System.lineSeparator());
				thrown.printStackTrace(out);
			}
			out.append(System.lineSeparator());
		}

	}

	public Map<String, String> toEnvironmentVariables() {
		return toEnvironmentVariables(envNamePrefix, getConfig().stream().map(e -> e.getValue().toEntry()).toList());
	}

	public static Map<String, String> toEnvironmentVariables(String prefix,
			Iterable<Map.Entry<String, String>> properties) {
		Map<String, String> env = new LinkedHashMap<>();
		for (var e : properties) {
			env.put(prefix + propertyNameToEnvironmentVariable(e.getKey()), e.getValue());
		}
		return env;
	}

	public boolean isLoaded() {
		return this.config != null;
	}

	public com.adamgent.petclinic.config.Config getConfig() {
		var p = config;
		return Objects.requireNonNull(p, "Config has not been loaded yet");
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

	public enum LoadFlag {

		SYSTEM_PROPERTIES

	}

	public com.adamgent.petclinic.config.Config load(LoadFlag... flags) {

		/*
		 * We have this whole event system because we want to load config before we load
		 * the logging sytem
		 */
		events().clear();

		var config = readConfig();
		var systemProps = systemProperties();

		List<KeyValue> kvs = toKeyValues(config);
		var c = com.adamgent.petclinic.config.Config.of(kvs);

		Set<LoadFlag> fs;

		if (flags != null && flags.length > 0) {
			fs = EnumSet.of(flags[0], flags);
		}
		else {
			fs = Set.of();
		}

		if (fs.contains(LoadFlag.SYSTEM_PROPERTIES)) {
			synchronized (systemProps) {
				for (var kv : c) {
					kv.getValue().set(systemProps::setProperty);
				}
			}
		}

		this.config = c;

		return c;

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

		Config commandLineConfig = parseCommandLine("-D");

		Config envConfig = parseEnv(envNamePrefix);

		Config defaultConfig = parseBuiltin(DEFAULT_PROFILE);

		Config userConfig = parseUserFile(DEFAULT_PROFILE);

		/*
		 * n.b. how we do not add system properties. we are only interested in our
		 * explicit config right now otherwise the final Map<String,String> will have all
		 * the system properties in it.
		 */
		List<Config> configs = List.of( //
				commandLineConfig, envConfig, userConfig, defaultConfig);

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
		// TODO do we need to reresolve here?
		return resolved;
	}

	static String[] commandLineArgsFromSystemProperties(Properties properties) {
		final String rawArgs = properties.getProperty("sun.java.command");
		if (rawArgs == null) {
			return new String[] {};
		}
		return rawArgs.split(" ");
	}

	static Map<String, String> propertiesFromCommandLine(Iterable<String> args, String prefix, String addPrefix) {
		Map<String, String> props = new LinkedHashMap<>();
		for (String arg : args) {
			@NonNull
			String[] kv = arg.split("=", 2);
			if (kv.length < 2)
				continue;
			String key = kv[0];
			if (key.startsWith(prefix)) {
				String propertyKey = removeStart(key, prefix);
				String value = kv[1];
				props.put(addPrefix + propertyKey, value);
			}
		}
		return props;
	}

	public String[] commandLineArgs() {
		return commandLineArgs;
	}

	private Config parseCommandLine(String prefix) {
		List<String> args = List.of(commandLineArgs());
		var m = propertiesFromCommandLine(args, prefix, "");
		new ConfigEvent.CommandLine(prefix, m).publish(eventConsumer);
		return ConfigFactory.parseMap(m, "commandLine");
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

		public record CommandLine(String prefix, Map<String, String> args) implements ConfigEvent {
			@Override
			public void log(Logger logger) {
				logger.log(Logger.Level.INFO, "Loaded CMD line args with prefix: " + prefix);
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

	static List<KeyValue> toKeyValues(Config config) {
		List<KeyValue> kvs = new ArrayList<>();
		for (var e : config.entrySet()) {
			String name = e.getKey();
			String value = config.getString(name);
			var info = e.getValue();
			String rawValue = info.render(ConfigRenderOptions.concise());
			var origin = e.getValue().origin();
			String filename = origin.resource();
			if (filename != null) {
				filename = "classpath:/" + filename;
			}
			else {
				filename = origin.filename();
			}
			if (filename == null) {
				filename = origin.description().substring(0, 30);
			}
			if (filename == null) {
				filename = "";
			}
			int line = origin.lineNumber();
			KeyValue keyValue = new KeyValue(name, value, rawValue, line, filename, 0);
			kvs.add(keyValue);
		}
		return kvs;
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
