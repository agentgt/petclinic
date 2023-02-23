package com.adamgent.petclinic.repo;

import java.util.List;

import org.jooq.DSLContext;

import com.adamgent.petclinic.db.internal.tables.VetsTable;
import com.adamgent.petclinic.db.internal.tables.records.VetsRecord;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class VetRepository {

	private final DSLContext jooq;

	@Inject
	VetRepository(DSLContext jooq) {
		super();
		this.jooq = jooq;
	}

	public List<Vet> findAll() {

		return List.of();
	}

}
