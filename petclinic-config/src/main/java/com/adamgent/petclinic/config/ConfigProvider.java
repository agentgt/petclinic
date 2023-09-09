package com.adamgent.petclinic.config;

import java.util.Objects;
import java.util.function.Supplier;

public class ConfigProvider {

	public static Config get() {
		return config.get();
	}

	public void set(Supplier<Config> supplier) {
		config = Objects.requireNonNull(supplier);
	}

	private static Supplier<Config> config = Suppliers.memoize(() -> ConfigBootstrap.load("petclinic"));

}
