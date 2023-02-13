package com.adamgent.petclinic.config;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

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

//	@Test
//	public void testIterator() {
//		Map<String, String> m = Map.of("foo.bar", "asdfsf");
//		var config = Config.ofEntries(m.entrySet());
//
//		for (var p : config.withPrefix("foo.")) {
//			System.out.println(p);
//		}
//
//	}

	@Test
	public void testConfigFunction() throws Exception {
		var config = ConfigBootstrap //
				.load("petclinic");

		URI uri = config.asFunction() //
				.compose("database."::concat) //
				.andThen(URI::create).apply("url");

		// int port = ConfigBootstrap //
		// .load("petclinic").withPrefix("database.").property("port").toInt();

	}
	
	@Test
	public void testUpdate()
			throws Exception {
		Map<String, String> m = Map.of("foo.bar", "1");

		List<KeyValue> kvs = KeyValue.of(m.entrySet().iterator(), "");
		
		Map<String, KeyValue> mkvs = new LinkedHashMap<>();
		
		for (var k : kvs) {
			mkvs.put(k.name(), k);
		}
		
		DefaultConfig c = new DefaultConfig(mkvs);
		
		var property = c.property("foo.bar");
		String v = property.get();
		
		assertEquals("1", v);
		
		mkvs.put("foo.bar", KeyValue.of("foo.bar", "2"));
		
		v = property.get();
		
		assertEquals("2", v);
		
		//mkvs.put("foo.bar", KeyValue.of("foo.bar", "2"));
		
		mkvs.remove("foo.bar");
		
		v = property.get();
		
		// after remove we go back to the initial value
		
		assertEquals("1", v);


	}
	
	enum Blah {
		STUFF
	}
	@Test
	public void testFlatMap()
			throws Exception {
		
		Map<String, String> m = Map.of("foo.bar", "1", "foo.foo", "2");
		var config = Config.ofEntries(m.entrySet());
		int fooBar = config.property("foo.bar")
			.map(Integer::parseInt)
			.flatMap(p -> config.property("foo.foo").map(Integer::parseInt).map(i -> i + p))
			.get();
		
		assertEquals(3, fooBar);
	}


}
