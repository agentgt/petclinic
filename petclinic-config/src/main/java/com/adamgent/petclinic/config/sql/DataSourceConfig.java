package com.adamgent.petclinic.config.sql;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.sql.DataSource;

import com.adamgent.petclinic.config.Component;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public record DataSourceConfig(String url, String username, String password, String driverClassName, int retryCount,
		int retrySleep, int initialSleep) {

	public static DataSourceConfig of(Function<String, String> f) {
		String url = f.apply("url");
		String username = f.apply("username");
		String password = f.apply("password");
		String driverClassName = f.apply("driverClassName");
		int retryCount = f.apply("retryCount").transform(Integer::valueOf);
		int retrySleep = f.apply("retrySleep").transform(Integer::valueOf);
		int initialSleep = f.apply("initialSleep").transform(Integer::valueOf);

		return new DataSourceConfig(url, username, password, driverClassName, retryCount, retrySleep, initialSleep);
	}

	public String toString() {
		/*
		 * This is to avoid toStringing the password. This should probably be put in some
		 * util class.
		 */
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getName());
		sb.append("[");
		boolean first = true;
		for (var rc : this.getClass().getRecordComponents()) {
			String name = rc.getName();
			if (name.equals("password")) {
				continue;
			}
			if (first) {
				first = false;
			}
			else {
				sb.append(", ");
			}
			try {
				Object o = rc.getAccessor().invoke(this);
				sb.append(name).append("=").append(o);
			}
			catch (IllegalAccessException e) {
			}
			catch (IllegalArgumentException e) {
			}
			catch (InvocationTargetException e) {
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public Component<DataSource> dataSource() {

		Logger logger = System.getLogger(DataSourceConfig.class.getName());

		for (int i = 1; i < retryCount; i++) {
			try {
				return Component.of(hikari(url, username, password, driverClassName));
			}
			catch (RuntimeException e) {
				int sleep = i == 1 ? initialSleep : retrySleep;
				logger.log(Level.WARNING, "Retrying to connect to db. " + "retryCount: " + retryCount + ", try: " + i
						+ ", sleep: " + sleep);
				try {
					TimeUnit.MILLISECONDS.sleep(sleep);
				}
				catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		return Component.of(hikari(url, username, password, driverClassName));
	}

	private static DataSource hikari(String url, String username, String password, String driverClassName) {
		HikariConfig hc = new HikariConfig();
		hc.setJdbcUrl(url);
		hc.setUsername(username);
		hc.setPassword(password);
		hc.setRegisterMbeans(false);
		hc.setDriverClassName(driverClassName);
		return new HikariDataSource(hc);
	}

}
