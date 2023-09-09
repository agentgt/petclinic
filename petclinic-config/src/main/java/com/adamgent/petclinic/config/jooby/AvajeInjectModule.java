package com.adamgent.petclinic.config.jooby;

import io.avaje.inject.BeanScope;
import io.jooby.Extension;
import io.jooby.Jooby;

@SuppressWarnings("exports")
public class AvajeInjectModule implements Extension {

	private final BeanScope beanScope;

	public static AvajeInjectModule of() {
		BeanScope beanScope = BeanScope.builder().build();
		return new AvajeInjectModule(beanScope);
	}

	public static AvajeInjectModule of(BeanScope beanScope) {
		return new AvajeInjectModule(beanScope);
	}

	AvajeInjectModule(BeanScope beanScope) {
		super();
		this.beanScope = beanScope;
	}

	@Override
	public void install(Jooby application) throws Exception {
		application.registry(new AvajeInjectRegistry(beanScope));

	}

}
