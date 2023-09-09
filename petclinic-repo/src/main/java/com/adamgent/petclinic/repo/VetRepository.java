package com.adamgent.petclinic.repo;

import java.util.List;

import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.Sql;

import com.adamgent.petclinic.config.doma.DomaSupport;

@Dao
@DomaSupport
public interface VetRepository {

	@Sql("""
				select * from vets
			""")
	@Select
	List<Vet> findAll();

}

// @Singleton
// class DefaultVetRepository implements VetRepository {
//
// private final DSLContext jooq;
//
// @Inject
// public DefaultVetRepository(DSLContext jooq) {
// super();
// this.jooq = jooq;
// }
//
// @Override
// public List<Vet> findAll() {
//
// return List.of();
// }
//
// }
