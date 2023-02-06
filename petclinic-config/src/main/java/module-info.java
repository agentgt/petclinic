module com.adamgent.petclinic.config {
	exports com.adamgent.petclinic.config;
	exports com.adamgent.petclinic.config.jooq;
	exports com.adamgent.petclinic.config.mapstruct;
	exports com.adamgent.petclinic.config.flyway;
	
	opens config;
	
	requires transitive java.sql;
	requires transitive org.slf4j;

	
	requires static java.management;
	
	requires static org.eclipse.jdt.annotation;
	requires static info.picocli;
	requires static org.jooq.codegen;
	requires static io.jstach.jstache;
	requires static io.jstach.jstachio;
	requires static typesafe.config;
	requires static org.mapstruct.processor;
	requires static org.mapstruct;
	requires static java.compiler;
	requires static com.zaxxer.hikari;
	requires static org.flywaydb.core;
	
}