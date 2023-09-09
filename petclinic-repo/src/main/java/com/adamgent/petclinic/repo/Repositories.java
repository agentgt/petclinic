package com.adamgent.petclinic.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

public interface Repositories extends AutoCloseable {

	VetRepository vetRepository();

	@Override
	default void close() {
		Logger log = LoggerFactory.getLogger(Repositories.class);
		log.info("Closing Repositories");
	}

}

@Singleton
record DefaultRepositories(VetRepository vetRepository) implements Repositories {
}

// @Singleton
// class DefaultRepositories implements Repositories {
//
// private final VetRepository vetRepository;
//
// @Inject
// public DefaultRepositories(VetRepository vetRepository) {
// super();
// this.vetRepository = vetRepository;
// }
//
// @Override
// public VetRepository vetRepository() {
// return vetRepository;
// }
//
// }
