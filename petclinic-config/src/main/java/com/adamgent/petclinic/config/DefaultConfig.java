package com.adamgent.petclinic.config;

import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

class DefaultConfig implements Config {

	private final Map<String, ? extends ConfigEntry> keyValues;


	DefaultConfig(Map<String, ? extends ConfigEntry> keyValues) {
		super();
		this.keyValues = keyValues;

	}

	@SuppressWarnings("unchecked")
	public Stream<ConfigEntry> stream() {
		return (Stream<ConfigEntry>) keyValues.values().stream();
	}
	
	private @Nullable ConfigEntry get(String name) {
		var ce = keyValues.get(name);
		if (ce != null) {
			ce = ce.withSupplier(() -> keyValues.get(name));
		}
		return ce;
	}

	public PropertyString property(String name) {
		var ce = get(name);
		if (ce == null) {
			return new MissingProperty<>(new MissingKey(name));
		}
		return ce;
	}
	
	record MissingKey(String name) implements Key {
	}

}