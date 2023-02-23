package com.adamgent.petclinic.repo;

import static org.junit.Assert.assertNotNull;

import org.jooq.DSLContext;
import org.junit.Test;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.ConfigBootstrap;

import io.avaje.inject.BeanScope;

public class RepositoriesTest {

	@Test
	public void test() {
		Config config = ConfigBootstrap.load("petclinic");
		try (
				BeanScope di = BeanScope.builder()
					.bean(Config.class, config)
					.build()) {
			var vr = di.get(VetRepository.class);
			
			assertNotNull(vr);
		}
	}

}
