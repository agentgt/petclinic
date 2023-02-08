package com.adamgent.petclinic.config;

import static com.adamgent.petclinic.config.ConfigBootstrap.propertySupplier;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.adamgent.petclinic.config.MainConfig.Command;
import com.adamgent.petclinic.config.flyway.FlywayConfig;
import com.adamgent.petclinic.config.flyway.FlywayRunner;
import com.adamgent.petclinic.config.jooq.JooqRunner;

public class Main {

	private static final String APPLICATION_NAME = "petclinic";

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
			if (!s.isBlank() && !s.startsWith("-")) {
				cwd = Path.of(s);
			}
		}

		ConfigBootstrap config = new ConfigBootstrap(APPLICATION_NAME, args);

		try {

			run(command, config, cwd);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static void run(Command command, ConfigBootstrap config, Path cwd) throws Exception {

		Map<String, String> properties = Map.of();
		Exception t = null;
		try {
			properties = config.load().toMap();
		}
		catch (Exception e1) {
			t = e1;
		}

		switch (command) {
			case ENV -> {
			}
			default -> {
				config.log(t);
				System.getLogger(Main.class.getName()).log(System.Logger.Level.INFO, "Running: " + command);
			}
		}

		if (t != null) {
			throw t;
		}

		var f = propertySupplier("database.", properties);

		DataSourceConfig dataSourceConfig = DataSourceConfig.of(f);

		var out = System.out;

		Path migration = cwd.resolve("src/main/resources/db/migration");

		List<String> flywayLocations = List.of("filesystem:" + migration.toString());

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
				FlywayConfig fc = new FlywayConfig(flywayLocations, false, true);
				FlywayRunner runner = new FlywayRunner(fc);
				try (var ds = dataSourceConfig.dataSource()) {
					runner.run(ds.get());
				}

			}
			case MIGRATE -> {
				FlywayConfig fc = new FlywayConfig(flywayLocations, true, true);
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

}
