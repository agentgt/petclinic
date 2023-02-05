package com.adamgent.petclinic.config.flyway;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public record FlywayConfig(List<String> locations, boolean migrate, boolean validate) {

	public static FlywayConfig of(Function<String, String> properties) {

		List<String> locations = properties.andThen(s -> Stream.of(s.split(",")).toList()).apply("locations");
		boolean migrate = properties.andThen(Boolean::valueOf).apply("migrate");
		boolean validate = properties.andThen(Boolean::valueOf).apply("validate");
		return new FlywayConfig(locations, migrate, validate);

	}

}
