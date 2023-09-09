module com.adamgent.petclinic.config {
	exports com.adamgent.petclinic.config;
	exports com.adamgent.petclinic.config.jooq;
	exports com.adamgent.petclinic.config.mapstruct;
	exports com.adamgent.petclinic.config.flyway;
	exports com.adamgent.petclinic.config.doma;
	exports com.adamgent.petclinic.config.sql;
	exports com.adamgent.petclinic.config.jooby;
	
	opens config;
	
	requires typesafe.config;
	requires static java.management;

	requires transitive java.sql;
	requires transitive org.slf4j;


	requires static io.avaje.inject;
	
	requires static org.eclipse.jdt.annotation;
	requires static org.jooq.codegen;
	requires static org.jooq;
	requires static org.seasar.doma.core;
	requires static jakarta.inject;
	
	requires static io.jooby;
	
	requires static io.jstach.jstache;
	requires static io.jstach.jstachio;
	requires static org.mapstruct.processor;
	requires static org.mapstruct;
	requires static java.compiler;
	requires static com.zaxxer.hikari;
	requires static org.flywaydb.core;
	
}