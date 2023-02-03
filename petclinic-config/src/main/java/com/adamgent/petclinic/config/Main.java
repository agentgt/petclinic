package com.adamgent.petclinic.config;

import java.util.EnumSet;
import java.util.Map;

public class Main {

	public static void main(
			String[] args) {

		String error = "Missing command arg. Commands (pick one) = " + EnumSet.allOf(Command.class);

		Command command;
		if (args.length == 0) {
			command = Command.VALIDATE;
		}
		else if (args.length != 1) {
			throw new RuntimeException(error);
		}
		else {
			try {
				command = Command.valueOf(args[1]);
			}
			catch (IllegalArgumentException e) {
				throw new RuntimeException(error, e);
			}
		}

		Map<String, String> properties = ConfigBootstrap.load("petclinic");

		var f = ConfigBootstrap.propertySupplier("database.", properties);

		DataSourceConfig dataSourceConfig = DataSourceConfig.of(f);

		switch (command) {
			case VALIDATE -> {
			}
			case MIGRATE -> {
			}
			case GENERATE -> {
			}
		}
		System.out.println(dataSourceConfig);

	}
	


	public enum Command {
		VALIDATE,
		MIGRATE,
		GENERATE
	}

}
