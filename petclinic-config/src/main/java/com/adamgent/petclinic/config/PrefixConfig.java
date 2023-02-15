package com.adamgent.petclinic.config;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PrefixConfig implements Config {

	private final String prefix;

	private final Config config;

	private PrefixConfig(String prefix, Config config) {
		super();
		this.prefix = prefix;
		this.config = config;
	}

	public static PrefixConfig of(Config config, String prefix) {
		return new PrefixConfig(prefix, config);
	}

	@Override
	public Stream<Entry<String, ConfigEntry>> stream() {
		return config.stream().filter(e -> e.getKey().startsWith(prefix))
				.map(e -> Map.entry(removeStart(e.getKey(), prefix), e.getValue()));
	}

	@Override
	public PropertyString property(String name) {
		String realName = prefix + name;
		return config.property(realName);
	}

	private static String removeStart(final String str, final String remove) {
		if (str.startsWith(remove)) {
			return str.substring(remove.length());
		}
		return str;
	}

	@Override
	public void onEvent(Consumer<? super Event> consumer) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void publish(Consumer<? super EventBuilder> eventProducer) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void publish(Event event) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EventBuilder eventBuilder() {
		throw new UnsupportedOperationException();
	}

}
