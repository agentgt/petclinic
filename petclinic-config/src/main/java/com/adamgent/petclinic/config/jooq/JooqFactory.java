package com.adamgent.petclinic.config.jooq;

import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.PrefixConfig;

@SuppressWarnings("exports")
public class JooqFactory {

	public static DSLContext provideDSLContext(Configuration c) {
		DefaultDSLContext context = new DefaultDSLContext(c);
		return context;
	}

	public static Configuration provideJOOQConfig(Config config, DataSource dataSource) {
		MyJooqConfiguration jc = new MyJooqConfiguration();

		var pc = PrefixConfig.of(config, "jooq.");

		Settings settings = new Settings();
		boolean renderSchema = pc.property("settings.renderSchema").toBoolean();
		boolean executeLogging = pc.property("settings.executeLogging").toBoolean();
		SQLDialect sqlDialect = SQLDialect.POSTGRES;

		settings.setRenderSchema(renderSchema);
		settings.setExecuteLogging(executeLogging);

		jc.setSettings(settings);
		jc.setSQLDialect(sqlDialect);
		jc.setDataSource(dataSource);
		return jc;
	}

	private static class MyJooqConfiguration extends DefaultConfiguration {

		/**
		 *
		 */
		private static final long serialVersionUID = 3021792821817910544L;

		public MyJooqConfiguration() {
		}

	}

}