package com.adamgent.petclinic.config.flyway;

import java.util.function.Function;

public record FlywayConfig(boolean migrate, boolean validate) {

	public static FlywayConfig of(Function<String, String> properties) {
		boolean migrate = properties.andThen(Boolean::valueOf).apply("migrate");
		boolean validate = properties.andThen(Boolean::valueOf).apply("validate");
		return new FlywayConfig(migrate, validate);

	}

}
