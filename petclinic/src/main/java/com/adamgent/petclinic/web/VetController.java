package com.adamgent.petclinic.web;

import com.adamgent.petclinic.repo.VetRepository;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@Path("/")
public class VetController {

	private final VetRepository vetRepository;

	@Inject
	public VetController(VetRepository vetRepository) {
		super();
		this.vetRepository = vetRepository;
	}

	@GET("/vets")
	public VetsHtml showVetList(@QueryParam Integer page) {
		int _page = page == null ? 0 : page;
		var vets = vetRepository.findAll();
		return new VetsHtml(vets);
	}

}
