package com.adamgent.petclinic.repo;

import static org.junit.Assert.assertNotNull;

import javax.sql.DataSource;

import org.junit.Test;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.ConfigProvider;

import io.avaje.inject.BeanScope;

public class RepositoriesTest {

	@Test
	public void test() {
		Config config = ConfigProvider.get();
		try (BeanScope di = BeanScope.builder().bean(Config.class, config).build()) {
			var vr = di.get(VetRepository.class);

			assertNotNull(vr);
		}
	}

}
