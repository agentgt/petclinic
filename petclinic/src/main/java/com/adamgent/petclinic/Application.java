package com.adamgent.petclinic;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.jooby.AvajeInjectModule;

import io.avaje.inject.BeanScope;
import io.jooby.Jooby;
import io.jooby.annotation.Path;

public class Application extends Jooby {

	Application() {
	}

	public static Application of(Config config) {
		BeanScope beanScope = BeanScope.builder().bean(Config.class, config).build();
		Application app = new Application();
		app.install(AvajeInjectModule.of(beanScope));
		var controllers = beanScope.listByAnnotation(Path.class);
		controllers.forEach(app::mvc);
		app.onStop(beanScope);
		return app;
	}

}
