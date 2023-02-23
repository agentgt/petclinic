package com.adamgent.petclinic.repo;

import java.io.Closeable;

import javax.sql.DataSource;

import org.jooq.DSLContext;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.flyway.FlywayConfig;
import com.adamgent.petclinic.config.flyway.FlywayRunner;
import com.adamgent.petclinic.config.jooq.JooqFactory;
import com.adamgent.petclinic.config.sql.DataSourceConfig;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.inject.InjectModule;

@InjectModule(requires = Config.class)
@Factory
class _Module {

	@Bean
	DbContainer dbContainer(Config config) {
		var runner = new FlywayRunner(FlywayConfig.of(config.asFunction().compose("flyway."::concat)));
		var dataSource = DataSourceConfig.of(config.asFunction().compose("database."::concat)).dataSource().get();
		runner.run(dataSource);
		return new DbContainer(dataSource);
	}

	@Bean
	DataSource dataSource(DbContainer container) {
		return container.dataSource();
	}

	record DbContainer(DataSource dataSource) implements AutoCloseable {
		@Override
		public void close() throws Exception {
			var ds = dataSource();
			if (ds instanceof Closeable c) {
				c.close();
			}

		}
	}

	@Bean
	DSLContext jooq(DataSource dataSource, Config config) {
		var conf = JooqFactory.provideJOOQConfig(config, dataSource);
		return JooqFactory.provideDSLContext(conf);
	}

}
