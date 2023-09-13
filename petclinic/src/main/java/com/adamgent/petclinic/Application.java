package com.adamgent.petclinic;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.jooby.AvajeInjectModule;
import com.adamgent.petclinic.config.jooby.WebJar;

import io.avaje.inject.BeanScope;
import io.jooby.Jooby;
import io.jooby.annotation.Path;
import io.jooby.handler.AssetSource;
import io.jooby.jstachio.JStachioModule;

public class Application extends Jooby {

	Application() {
	}

	public static Application of(Config config) {
		BeanScope beanScope = BeanScope.builder().bean(Config.class, config).build();
		Application app = new Application();
		app.install(AvajeInjectModule.of(beanScope));
		app.install(new JStachioModule());

		ClassLoader loader = Application.class.getClassLoader();

		WebJar.builder() //
				.add("org.webjars.npm", "font-awesome") //
				.add("org.webjars.npm", "bootstrap") //
				.install(app, loader);
		app.assets("/static/*", AssetSource.create(loader, "/static"));
		var controllers = beanScope.listByAnnotation(Path.class);
		controllers.forEach(app::mvc);
		app.onStop(beanScope);
		return app;
	}

}
