package com.adamgent.petclinic.repo;

import java.util.List;

import org.jooq.DSLContext;

import com.adamgent.petclinic.db.internal.tables.VetsTable;
import com.adamgent.petclinic.db.internal.tables.records.VetsRecord;

public class VetRepository {

	private final DSLContext jooq;

	public VetRepository(DSLContext jooq) {
		super();
		this.jooq = jooq;
	}

	public List<Vet> findAll() {

		return List.of();
	}

}
