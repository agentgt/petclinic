import io.jstach.jstache.JStacheConfig;
import io.jstach.jstache.JStachePath;

@JStacheConfig(pathing = @JStachePath(prefix = "templates/", suffix=".mustache"))
open module com.adamgent.petclinic {
	requires static org.eclipse.jdt.annotation;

	requires io.jooby;
	requires com.adamgent.petclinic.repo;
	requires com.adamgent.petclinic.config;
	requires io.jstach.jstachio;
	requires io.jooby.jstachio;

	provides io.avaje.inject.spi.Module with com.adamgent.petclinic.web.WebModule;
}