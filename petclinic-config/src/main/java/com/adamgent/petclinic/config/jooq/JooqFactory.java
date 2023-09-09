package com.adamgent.petclinic.config.jooq;

import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

import com.adamgent.petclinic.config.Config;

@SuppressWarnings("exports")
public class JooqFactory {

	public static DSLContext provideDSLContext(Configuration c) {
		DefaultDSLContext context = new DefaultDSLContext(c);
		return context;
	}

	public static Configuration provideJOOQConfig(Config config, DataSource dataSource) {
		MyJooqConfiguration jc = new MyJooqConfiguration();
		// Config c = PrefixConfig.of(config, "jooq.");

		Settings settings = new Settings();
		// settings.setRenderSchema(
		// s.getBoolean("renderSchema")
		// .orValue(Nullables.nullToFalse(settings.isRenderSchema())));
		// settings.setExecuteLogging(
		// s.getBoolean("executeLogging")
		// .orValue(Nullables.nullToFalse(settings.isExecuteLogging())));
		jc.setSettings(settings);
		jc.setSQLDialect(SQLDialect.POSTGRES);
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
