package com.adamgent.petclinic.config.flyway;

import java.lang.System.Logger.Level;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

public class FlywayRunner {

	private final FlywayConfig config;

	public FlywayRunner(
			FlywayConfig config) {
		this.config = config;
	}

	public DataSource migrate(
			DataSource ds) {
		ClassicConfiguration c = new ClassicConfiguration();
		c.setBaselineOnMigrate(true);
		c.setDataSource(ds);
		System.Logger log = System.getLogger(FlywayRunner.class.getName());
		Flyway f = new Flyway(c);
		if (config.migrate()) {
			log.log(Level.INFO, "migrating database");
			f.migrate();
		}
		else if (config.validate()) {
			log.log(Level.INFO, "validating database");
			f.validate();
		}
		else {
			log.log(Level.WARNING, "Not checking database");
		}
		return ds;
	}



}