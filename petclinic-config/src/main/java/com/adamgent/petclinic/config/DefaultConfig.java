package com.adamgent.petclinic.config;

import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

class DefaultConfig implements Config {

	private final Map<String, PropertyString> keyValues;

	private final String prefix;

	private final ConfigInfo info;

	DefaultConfig(Map<String, PropertyString> keyValues, String prefix) {
		super();
		if (prefix == null) {
			throw new NullPointerException("prefix");
		}
		this.keyValues = keyValues;
		this.prefix = prefix;
		this.info = new ConfigInfo() {

			@Override
			public String description() {
				return prefix;
			}

			@Override
			public String toString() {
				return description();
			}
		};
	}

	interface ConfigInfo {

		String description();

	}

	public Stream<PropertyString> stream() {
		return keyValues.values().stream().map(this::toProperty).filter(p -> p != null);
	}

	public Config withPrefix(String prefix) {
		return new DefaultConfig(keyValues, prefix);
	}

	public PropertyString property(String name) {
		String path = prefix + name;
		var kv = get(path);
		var prop = toProperty(kv);
		if (prop == null) {
			return new MissingProperty<>(name, info);
		}
		return toProperty(kv);
	}

	private String minusPrefix(String path) {
		if (path.startsWith(prefix)) {
			return path.substring(prefix.length());
		}
		return path;
	}

	private boolean hasPrefix(PropertyString kv) {
		return kv.name().startsWith(prefix);
	}

	@Nullable
	private PropertyString get(String name) {
		return keyValues.get(name);
	}

	private @Nullable PropertyString toProperty(@Nullable PropertyString kv) {
		if (kv == null || !hasPrefix(kv)) {
			return null;
		}
		if ("".equals(prefix)) {
			return kv;
		}
		String name = minusPrefix(kv.name());
		return new PropertyKeyValue(kv, name, info);
	}

	private record MissingProperty<T> (String name, ConfigInfo info) implements PropertyString {
		@Override
		public @Nullable String orNull() {
			return null;
		}

		@Override
		public String description() {
			String desc = info.description();
			return PropertyString.super.description() + (desc.isEmpty() ? "" : " @ [" + info.description() + "] ");
		}
	}

	private record PropertyKeyValue(PropertyString key, String name, ConfigInfo info) implements PropertyString {

		@Override
		public @Nullable String orNull() {
			return key.orNull();
		}

		@Override
		public String description() {
			return PropertyString.super.description() + " @ [" + key.description() + "] " + name();
		}

	}

}