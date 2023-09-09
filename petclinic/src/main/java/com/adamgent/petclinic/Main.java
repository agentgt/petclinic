package com.adamgent.petclinic;

import com.adamgent.petclinic.config.Config;
import com.adamgent.petclinic.config.ConfigBootstrap;

public class Main {

	public static void main(String[] args) {
		Config config = ConfigBootstrap.load("petclinic");
		Application app = Application.of(config);
		app.start();
	}

}
