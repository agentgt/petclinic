open module com.adamgent.petclinic {
	requires static org.eclipse.jdt.annotation;

	requires io.jooby;
	requires com.adamgent.petclinic.repo;
	requires com.adamgent.petclinic.config;

	provides io.avaje.inject.spi.Module with com.adamgent.petclinic.web.WebModule;
}