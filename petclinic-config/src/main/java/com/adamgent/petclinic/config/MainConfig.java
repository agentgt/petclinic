package com.adamgent.petclinic.config;

import java.nio.file.Path;

public record MainConfig(Command command, Path cwd) {

	public enum Command {

		SHOW, PROPERTIES, ENV, VALIDATE, MIGRATE, JOOQ

	}
}
