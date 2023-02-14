package com.adamgent.petclinic.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PrefixConfig implements Config {

	private final String prefix;
	private final Map<String, ConfigEntry> entries = new ConcurrentHashMap<>();
	
	private PrefixConfig(
			String prefix) {
		super();
		this.prefix = prefix;
	}
	
	public static PrefixConfig of(String prefix, Config config) {
		PrefixConfig pc = new PrefixConfig(prefix);
		config.onEvent(ce -> {
			if (ce.description().equals("prefix") || ce.update()) {
				pc.entries.clear();
				for (var e : ce.snapshot().entrySet()) {
					if (e.getKey().startsWith(prefix)) {
						pc.entries.put(e.getKey(), e.getValue());
					}
				}
			}
		});
		config.publish(eb -> eb.description("prefix"));
		return pc;
	}

	@Override
	public Stream<ConfigEntry> stream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PropertyString property(
			String name) {
		var ce = entries.get(prefix + name);
		if (ce != null) {
			return ce;
		}
		return PropertyString.missing(name);
//		Key key = new Key() {
//			@Override
//			public String name() {
//				return name;
//			}
//			@Override
//			public String description() {
//				return "[" + prefix + "]" + name();
//			}
//		};
//		var property = config.property(prefix + name);
//		return PropertyString.of(property.flatMap(p -> Property.ofSupplier(key, () -> property.orNull())));
	}
	
	

	@Override
	public void onEvent(
			Consumer<? super Event> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publish(
			Consumer<? super EventBuilder> eventProducer) {
		// TODO Auto-generated method stub
		
	}

}
