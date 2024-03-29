package com.adamgent.petclinic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.adamgent.petclinic.config.Config.PropertyConvertException;

public class ConfigTest {

	@Test
	public void testProperty() {
		var config = Config.builder().add("foo.bar", "bar").build();

		String value = config.property("foo.bar").get();
		assertEquals("bar", value);
	}

	@Test
	public void testConvert() {
		Map<String, String> m = Map.of("foo.bar", "1");
		var config = Config.builder().add(m).build();

		int value = config.property("foo.bar").toInt(Integer::parseInt);

		assertEquals(1, value);
	}

	@Test
	public void testConvertFail() {
		Map<String, String> m = Map.of("foo.bar", "asdfsf");
		var config = Config.builder().add(m).build();

		assertThrows(IllegalArgumentException.class, () -> {
			config.property("foo.bar").toInt(Integer::parseInt);
		});
	}

	@Test
	public void testOr() throws Exception {
		Map<String, String> m = Map.of("foo.bar", "asdfsf");
		var c = Config.builder().add(m).build();
		URI stuff = c.property("junk").map(s -> URI.create(s)).orElse(URI.create("https://stuff"));
		System.out.println(stuff);
	}

	@Test
	public void testConfigFunction() throws Exception {
		var config = ConfigBootstrap //
				.load("petclinic");

		URI uri = null;

		// URI _uri = config.asOptional() //
		// .compose("database."::concat) //
		// .andThen( o -> o.map(URI::create))
		// .apply("url")
		// .orElse(null);
		//
		// assertNotNull(_uri);

		uri = config.asOptional() //
				// compose("database."::concat) //
				.andThen(o -> o.map(URI::create)).apply("database.url").orElse(null);

		assertNotNull(uri);

		uri = config.asOptional() //
				.compose("database."::concat) //
				.andThen(o -> o.map(URI::create)).apply("url").orElse(null);

		assertNotNull(uri);

		try {
			config.asOptional() //
					.compose("database."::concat) //
					.andThen(o -> o.map(Integer::parseInt)).apply("url").orElse(null);
			fail("should have thrown convert exception");
		}
		catch (PropertyConvertException e) {
		}

		// uri = config.asNullable() //
		// .compose("database."::concat) //
		// .andThen(URI::create).apply("asdasf");

		// assertNull(uri);

		uri = config.asFunction() //
				.compose("database."::concat) //
				.andThen(URI::create).apply("url");

		String scheme = uri.getScheme();

		assertEquals("jdbc", scheme);

		// int port = ConfigBootstrap //
		// .load("petclinic").withPrefix("database.").property("port").toInt();

	}

	@Test
	public void testUpdate() throws Exception {
		Map<String, String> m = Map.of("foo.bar", "1");

		List<String> events = new ArrayList<>();
		List<Long> versions = new ArrayList<>();

		Config c = DefaultConfig.of(m);

		c.onEvent(e -> {
			events.add(e.description());
		});

		c.onEvent(e -> {
			versions.add(e.version());
		});

		var property = c.property("foo.bar");
		System.out.println(property);

		String v = property.get();

		assertEquals("1", v);

		c.publish(b -> {
			b.description(b.property("foo.bar").toInt() + "->2");
			b.put("foo.bar", "2");
		});

		v = property.get();

		assertEquals("2", v);

		c.publish(b -> {
			b.description("remove");
			b.remove("foo.bar");
		});

		v = property.get();

		// after remove we go back to the initial value

		assertEquals("1", v);

		assertEquals(List.of(1L, 2L), versions);

		System.out.println(events);

	}

	@Test
	public void testPrefix() throws Exception {
		var config = Config.builder().add("foo.bar", "1").add("foo.foo", "2").add("foo.stuff", "stuff").build();

		var pc = PrefixConfig.of(config, "foo.");
		assertEquals(1, pc.property("bar").toInt());
		assertEquals("foo.bar", pc.property("bar").name());

		List<String> names = pc.stream().map(e -> e.getValue()).map(e -> e.name()).toList();
		assertEquals(List.of("foo.bar", "foo.foo", "foo.stuff"), names);
	}

	// @Test
	// public void testConfigEntrySupplier()
	// throws Exception {
	// KeyValue a = KeyValue.of("a", "A");
	// KeyValue b = KeyValue.of("b", "B");
	//
	// Map<String, ConfigEntry> m = new LinkedHashMap<>();
	// ConfigEntrySupplier ac = new ConfigEntrySupplier(() -> m.get("b"), a);
	// ConfigEntrySupplier bc = new ConfigEntrySupplier(() -> m.get("a"), b);
	// m.put("b", bc);
	// m.put("a", ac);
	// ac.get();
	// }
	//
	enum Blah {

		STUFF

	}

	@Test
	public void testFlatMap() throws Exception {

		Map<String, String> m = Map.of("foo.bar", "1", "foo.foo", "2");
		var config = Config.builder().add(m).build();
		int fooBar = config.property("foo.bar").map(Integer::parseInt)
				.flatMap(p -> config.property("foo.foo").map(Integer::parseInt).map(i -> i + p)).get();

		assertEquals(3, fooBar);
	}

}
