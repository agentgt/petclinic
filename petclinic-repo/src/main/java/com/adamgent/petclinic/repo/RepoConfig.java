package com.adamgent.petclinic.repo;

import javax.sql.DataSource;

import org.jooq.DSLContext;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.ConfigBootstrap;
import com.adamgent.petclinic.config.doma.DomaConfig;
import com.adamgent.petclinic.config.jooq.JooqFactory;
import com.adamgent.petclinic.config.sql.DataSourceConfig;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;

@Factory
class RepoConfig {

	@Bean
	Config config() {
		return ConfigBootstrap.load("petclinic");
	}

	@Bean(autoCloseable = true)
	DataSource dataSource(Config config) {
		return DataSourceConfig.of(config.asFunction().compose("database."::concat)).dataSource().get();
	}

	@Bean
	DSLContext jooq(DataSource dataSource, Config config) {
		var c = JooqFactory.provideJOOQConfig(config, dataSource);
		return JooqFactory.provideDSLContext(c);
	}

	@Bean
	DomaConfig domaConfig(DataSource dataSource) {
		return DomaConfig.of(dataSource);
	}

}
