package com.adamgent.petclinic.config;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class ConfigTest {

	@Test
	public void testProperty() {
		Map<String,String> m = Map.of("foo.bar", "bar");
		var config = Config.of(m.entrySet());
		
		String value = config.property("foo.bar").get();
		assertEquals("bar", value);
	}

	@Test
	public void testConvert() {
		Map<String,String> m = Map.of("foo.bar", "1");
		var config = Config.of(m.entrySet());
		
		int value = config.property("foo.bar").mapInt(Integer::parseInt);
		
		assertEquals(1, value);
	}
}
