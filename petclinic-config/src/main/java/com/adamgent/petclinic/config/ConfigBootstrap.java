package com.adamgent.petclinic.config;

import java.io.File;
import java.lang.System.Logger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

public class ConfigBootstrap {
	
	public static Map<String,String> load(String applicationName) {
		
		ConcurrentLinkedQueue<ConfigEvent> events = new ConcurrentLinkedQueue<>();
		var config = ConfigBootstrap.readConfig(applicationName, events::add);
		Map<String,String> properties = toMap(config);
		Map<String,String> resolved = new LinkedHashMap<>();

		var systemProps = System.getProperties();
		synchronized (systemProps) {
			for (var e : properties.entrySet()) {
				if (! systemProps.containsKey(e.getKey())) {
					systemProps.put(e.getKey(), e.getValue());
					resolved.put(e.getKey(), e.getValue());
				}
				else {
					resolved.put(e.getKey(), systemProps.getProperty(e.getKey()));
				}
			}
		}
		var logger = System.getLogger(ConfigBootstrap.class.getCanonicalName());
		
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
		
		return resolved;
		
		
	}
	
	public static Function<String, String> propertySupplier(String prefix, Map<String,String> m) {
		return (s) -> {
			var k = prefix + s;
			String v = m.get(k);
			if (v == null) {
				throw new NullPointerException("missing property: " + k);
			}
			return v;
		};
	}
	
	static Config readConfig(String applicationName, Consumer<ConfigEvent> eventConsumer) {
		
		/*
		 * I think typesafe aka lightbend config is a piece of shit.. end of rant
		 */
		String confDirName = applicationName;
		String envName = applicationName.replace(".", "_").replace("-", "_").toUpperCase();
		String envNamePrefix = envName + "_";
		
		var envProperties = propertiesFromEnv(System.getenv(), envNamePrefix, "");
		
		new ConfigEvent.Env(Instant.now(), envNamePrefix, envProperties).publish(eventConsumer);
		
		Config envConfig = ConfigFactory.parseMap(envProperties, "environment");
		
		Config systemPropertiesConfig = ConfigFactory.systemProperties();
		
		ConfigEvent.load("config/default.conf", true).publish(eventConsumer);
		
		Config defaultConfig = ConfigFactory.parseResources(ConfigBootstrap.class.getClassLoader(), "config/default.conf" );
		
		File userFile = new File(System.getProperty("user.home"), ".config/" + confDirName + "/config.properties");
		
		ConfigEvent.load(userFile.toString()).publish(eventConsumer);
		
		Config userConfig = ConfigFactory.parseFile(userFile, ConfigParseOptions.defaults().setAllowMissing(true));
		
		List<Config> configs = List.of(userConfig, envConfig, defaultConfig);
		
		
 		Config resolved = ConfigFactory.empty();
 		
 		for (var c : configs) {
 			resolved = resolved.withFallback(c);
 		}
 		
 		resolved = resolved.resolveWith(systemPropertiesConfig.withFallback(resolved));

 		return resolved;
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
		
		public record Load(Instant time, String file, boolean required) implements ConfigEvent {
			@Override
			public void log(
					Logger logger) {
				logger.log(Logger.Level.INFO, "Load: \"" + file + "\" required=" + required);
				
			}
		}
		
		public record Env(Instant time, String prefix, Map<String,String> env) implements ConfigEvent {
			@Override
			public void log(
					Logger logger) {
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
				logger.log(Logger.Level.INFO,  sb.toString());
				
			}
		}
		
	}
	
	static Map<String, String> toMap(Config config) {
 		Map<String,String> m = new LinkedHashMap<>();
 		for (var e: config.entrySet()) {
 			String key = e.getKey();
 			String value = config.getString(key);
 			m.put(key, value);
 		}
 		return m;
	}
	
	public static String environmentVariableToPropertyName(
			String p) {
		String property = p.replaceAll("([^_])_([^_])", "$1.$2")
			.replaceAll("([^_])_(_+)([^_])", "$1$2$3");
		// If its all upper case we convert to lower case and its probably a
		// windows
		// environment.
		// If its mixed case or its all lower case its probably a unix
		// environment.
		if (property.toUpperCase()
			.equals(property)) {
			property = property.toLowerCase();
		}
		return property;
	}
	
	public static Map<String, String> propertiesFromEnv(
			Map<String, String> env,
			String prefix,
			String replacement) {
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
	
	private static String removeStart(
			final String str,
			final String remove) {
		if (str.startsWith(remove)) {
			return str.substring(remove.length());
		}
		return str;
	}

}
