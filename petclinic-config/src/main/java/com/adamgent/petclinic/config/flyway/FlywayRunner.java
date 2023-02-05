package com.adamgent.petclinic.config.flyway;

import java.lang.System.Logger.Level;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

public class FlywayRunner {

	private final FlywayConfig config;

	public FlywayRunner(FlywayConfig config) {
		this.config = config;
	}

	public DataSource run(DataSource ds) {
		ClassicConfiguration c = new ClassicConfiguration();
		c.setBaselineOnMigrate(true);
		c.setDataSource(ds);
		c.setFailOnMissingLocations(true);
		Location[] locations = config.locations().stream().map(Location::new).toList().toArray(new Location[] {});
		c.setLocations(locations);
		// c.setLocations(new Location("filesystem:src/main/resources/db/migrate"));
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