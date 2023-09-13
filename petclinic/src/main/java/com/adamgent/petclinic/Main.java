package com.adamgent.petclinic;

import java.util.concurrent.Executors;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.ConfigBootstrap;

public class Main {

	public static void main(String[] args) {
		Config config = ConfigBootstrap.load("petclinic");
		Application app = Application.of(config);
		if (config.property("shutdown.endpoint").toBoolean()) {
			app.get("/shutdown", (ctx) -> {
				Executors.newSingleThreadExecutor().execute(Main::shutdown);
				return """
						SHUTTING DOWN
						""";
			});
		}
		app.start();
	}

	static synchronized void shutdown() {
		try {
			Thread.sleep(20);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		System.out.println("SHUTTING DOWN with exit code 2");
		System.exit(2);
	}

}
