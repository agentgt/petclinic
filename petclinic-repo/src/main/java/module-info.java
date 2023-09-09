module com.adamgent.petclinic.repo {
	exports com.adamgent.petclinic.repo;
	
	requires static org.eclipse.jdt.annotation;
	requires com.adamgent.petclinic.config;
	requires transitive io.avaje.inject;
	
	requires org.seasar.doma.core;
	
	requires org.jooq;
	
	provides io.avaje.inject.spi.Module with com.adamgent.petclinic.repo.RepoModule;
}