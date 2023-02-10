package com.adamgent.petclinic.config;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class ConfigTest {

	@Test
	public void testProperty() {
		Map<String, String> m = Map.of("foo.bar", "bar");
		var config = Config.of(m.entrySet());

		String value = config.property("foo.bar")
			.get();
		assertEquals("bar", value);
	}

	@Test
	public void testConvert() {
		Map<String, String> m = Map.of("foo.bar", "1");
		var config = Config.of(m.entrySet());

		int value = config.property("foo.bar")
			.toInt(Integer::parseInt);

		assertEquals(1, value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertFail() {
		Map<String, String> m = Map.of("foo.bar", "asdfsf");
		var config = Config.of(m.entrySet());

		config.withPrefix("foo.")
			.property("bar")
			.toInt(Integer::parseInt);

	}

	@Test
	public void testIterator() {
		Map<String, String> m = Map.of("foo.bar", "asdfsf");
		var config = Config.of(m.entrySet());

		for (var p : config.withPrefix("foo.")) {
			System.out.println(p);
		}

	}

	@Test
	public void testConfig()
			throws Exception {
		int port = ConfigBootstrap //
			.load("petclinic")
			.withPrefix("database.")
			.property("port")
			.toInt();
		
//		var uri = ConfigBootstrap //
//				.load("petclinic")
//				.withPrefix("database.")
//				.property("port")
//				.map(UUID::fromString)
//				.orElse(UUID.randomUUID());
		
		System.out.println(uri);
		
//		int port = ConfigBootstrap //
//				.load("petclinic")
//				.convert(Integer::parseInt)
		}

}
