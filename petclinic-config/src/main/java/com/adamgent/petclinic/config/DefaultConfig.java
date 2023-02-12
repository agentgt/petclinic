package com.adamgent.petclinic.config;

import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

class DefaultConfig implements Config {

	private final Map<String, ConfigEntry> keyValues;


	DefaultConfig(Map<String, ConfigEntry> keyValues) {
		super();
		this.keyValues = keyValues;

	}

	public Stream<ConfigEntry> stream() {
		
		return keyValues.values().stream();
	}
	
	private @Nullable ConfigEntry get(String name) {
		return keyValues.get(name);
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