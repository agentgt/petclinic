package com.adamgent.petclinic.config.doma;

import javax.sql.DataSource;

import org.seasar.doma.jdbc.Naming;
import org.seasar.doma.jdbc.dialect.Dialect;
import org.seasar.doma.jdbc.dialect.PostgresDialect;

@SuppressWarnings("exports")
public class DomaConfig implements org.seasar.doma.jdbc.Config {

	private final DataSource dataSource;

	private final Dialect dialect;

	public static DomaConfig of(DataSource dataSource) {
		return new DomaConfig(dataSource, new PostgresDialect());
	}

	DomaConfig(DataSource dataSource, Dialect dialect) {
		super();
		this.dataSource = dataSource;
		this.dialect = dialect;
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public Naming getNaming() {
		return Naming.SNAKE_LOWER_CASE;
	}

}
