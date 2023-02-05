package com.adamgent.petclinic.config;

import static com.adamgent.petclinic.config.ConfigBootstrap.propertySupplier;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import com.adamgent.petclinic.config.flyway.FlywayConfig;
import com.adamgent.petclinic.config.flyway.FlywayRunner;
import com.adamgent.petclinic.config.jooq.JooqRunner;

public class Main {

	public static void main(String[] args) {

		String error = "Missing command arg. Commands (pick one) = " + EnumSet.allOf(Command.class);

		Command command;
		if (args.length == 0) {
			command = Command.SHOW;
		}
		else {
			try {
				command = Command.valueOf(args[0]);
			}
			catch (IllegalArgumentException e) {
				throw new RuntimeException(error, e);
			}
		}

		Path cwd = Paths.get("");

		if (args.length == 2) {
			String s = args[1];
			if (!s.isBlank()) {
				cwd = Path.of(s);
			}
		}

		ConfigBootstrap config = new ConfigBootstrap("petclinic");

		try {

			run(command, config, cwd);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static void run(Command command, ConfigBootstrap config, Path cwd) throws Exception {

		var properties = config.load();

		var f = propertySupplier("database.", properties);

		DataSourceConfig dataSourceConfig = DataSourceConfig.of(f);

		var out = System.out;

		switch (command) {
			case ENV -> {
			}
			default -> {
				config.log();
				System.out.println("Running: " + command);
			}
		}

		switch (command) {
			case SHOW -> {
				for (var e : properties.entrySet()) {
					out.println("" + e.getKey() + "=" + e.getValue());
				}
			}
			case PROPERTIES -> {
				var path = cwd.resolve("target/application.properties");
				path.getParent().toFile().mkdir();
				try (var w = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					ConfigBootstrap.writeProperties(properties, w);
				}
			}
			case ENV -> {
				try (var w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
					ConfigBootstrap.writeEnv(config.toEnvironmentVariables(), w);
				}
			}
			case VALIDATE -> {
				FlywayConfig fc = new FlywayConfig(false, true);
				FlywayRunner runner = new FlywayRunner(fc);
				try (var ds = dataSourceConfig.dataSource()) {
					runner.run(ds.get());
				}

			}
			case MIGRATE -> {
				FlywayConfig fc = new FlywayConfig(true, true);
				FlywayRunner runner = new FlywayRunner(fc);
				try (var ds = dataSourceConfig.dataSource()) {
					runner.run(ds.get());
				}
			}
			case JOOQ -> {
				out.println(JooqRunner.toXmlConfig(dataSourceConfig, cwd));
				JooqRunner.run(dataSourceConfig, cwd);
			}
		}
	}

	public enum Command {

		SHOW, PROPERTIES, ENV, VALIDATE, MIGRATE, JOOQ

	}

}
