package com.adamgent.petclinic.config;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.adamgent.petclinic.config.Config.ConfigEntry;
import com.adamgent.petclinic.config.Config.ConfigEntrySupplier;
import com.adamgent.petclinic.config.Config.KeyValue;

public class ConfigTest {

	@Test
	public void testProperty() {
		Map<String, String> m = Map.of("foo.bar", "bar");
		var config = Config.ofEntries(m.entrySet());

		String value = config.property("foo.bar").get();
		assertEquals("bar", value);
	}

	@Test
	public void testConvert() {
		Map<String, String> m = Map.of("foo.bar", "1");
		var config = Config.ofEntries(m.entrySet());

		int value = config.property("foo.bar").toInt(Integer::parseInt);

		assertEquals(1, value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertFail() {
		Map<String, String> m = Map.of("foo.bar", "asdfsf");
		var config = Config.ofEntries(m.entrySet());

		config.property("foo.bar").toInt(Integer::parseInt);
	}

	@Test
	public void testOr() throws Exception {
		Map<String, String> m = Map.of("foo.bar", "asdfsf");
		var c = Config.ofEntries(m.entrySet());
		URI stuff = c.property("junk").map(s -> URI.create(s)).orElse(URI.create("https://stuff"));
		System.out.println(stuff);
	}

	@Test
	public void testConfigFunction() throws Exception {
		var config = ConfigBootstrap //
				.load("petclinic");

		URI uri = config.asFunction() //
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
		Map<String, String> m = Map.of("foo.bar", "1", "foo.foo", "2");
		var config = Config.ofEntries(m.entrySet());
		var pc = PrefixConfig.of("foo.", config);
		assertEquals(1, pc.property("bar").toInt());
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
		var config = Config.ofEntries(m.entrySet());
		int fooBar = config.property("foo.bar").map(Integer::parseInt)
				.flatMap(p -> config.property("foo.foo").map(Integer::parseInt).map(i -> i + p)).get();

		assertEquals(3, fooBar);
	}

}
