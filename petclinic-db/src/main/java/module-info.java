module com.adamgent.petclinic.db {
	requires static org.eclipse.jdt.annotation;
	requires com.zaxxer.hikari;
	requires java.compiler;
	requires org.jooq;
	/*
	 * This is for @ConstructorProperties
	 */
	requires static java.desktop;
	
	requires transitive com.adamgent.petclinic.config;
	requires transitive jakarta.validation;
}