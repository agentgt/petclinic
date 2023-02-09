package com.adamgent.petclinic.config;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import com.adamgent.petclinic.config.Config.Property;

public class Config implements Iterable<Property<String>> {

	private final Map<String, KeyValue> keyValues;
	private final String prefix;
	private final ConfigInfo info;
	
	Config(
			Map<String, KeyValue> keyValues, String prefix) {
		super();
		if (prefix == null) {
			throw new NullPointerException("prefix");
		}
		this.keyValues = keyValues;
		this.prefix = prefix;
		this.info = new ConfigInfo() {
			
			@Override
			public String description() {
				return "prefix=" + prefix;
			}
			
			@Override
			public String toString() {
				return description();
			}
		};
	}
	
	public static Config empty() {
		return new Config(Map.of(), "");
	}
	
	public static Config of(Iterable<? extends Entry<String,String>> keyValues) {
		final Map<String, KeyValue> kvs = new LinkedHashMap<>();

		int i = 0;
		for (var e: keyValues) {
			kvs.put(e.getKey(), KeyValue.of(e, "", i++));
		}
		return new Config(kvs, "");
	}
	
	public Config withPrefix(String prefix) {
		return new Config(keyValues, prefix);
	}
	
	
	@Override
	public Iterator<Property<String>> iterator() {
		return stream().iterator();
	}
	
	public Stream<Property<String>> stream() {
		return keyValues.values()
				.stream()
				.map(this::toProperty)
				.filter(p -> p != null);
	}
	
	String minusPrefix(String path) {
		if (path.startsWith(prefix)) {
			return path.substring(prefix.length());
		}
		return path;
	}
	
	boolean hasPrefix(KeyValue kv) {
		return kv.name().startsWith(prefix);
	}
	
	public Map<String,String> toMap() {
		Map<String, String> r = new LinkedHashMap<>();
		for (var e : keyValues.entrySet()) {
			r.put(e.getKey(), e.getValue().get());
		}
		return r;
	}

	@Nullable KeyValue get(String name) {
		return keyValues.get(name);
	}
	

	public interface Key {
		String name();
		
		default String description() {
			return name();
		}
		
		default String path() {
			return name();
		}
		
	}
	
	public Property<String> property(
			String name) {
		String path = prefix + name;
		KeyValue kv = get(path);
		var prop = toProperty(kv);
		if (prop == null) {
			return new MissingProperty<>(name, info);
		}
		return toProperty(kv);
	}

	private @Nullable Property<String> toProperty(
			@Nullable KeyValue kv) {
		if (kv == null || ! hasPrefix(kv)) {
			return null;
		}
		if ("".equals(prefix)) {
			return kv;
		}
		String name = minusPrefix(kv.name());
		return new PropertyKeyValue(kv, name, info);
	}
	
	public interface ConfigInfo {
		String description();
	}
	
	public sealed interface Property<T> extends Key {
		default <R> R map(Function<T,R> f) {
			try {
				return f.apply(get());
			}
			catch (IllegalArgumentException  e) {
				throw new PropertyConvertException("unable to convert: ", this, e);
			}
		}
		
		default int mapInt(ToIntFunction<T> f) {
			try {
				return f.applyAsInt(get());
			}
			catch (IllegalArgumentException  e) {
				throw new PropertyConvertException("unable to convert to int: ", this, e);
			}
		}
		
		default long mapLong(ToLongFunction<T> f) {
			try {
				return f.applyAsLong(get());
			}
			catch (IllegalArgumentException  e) {
				throw new PropertyConvertException("unable to convert to long: ", this, e);
			}
		}
		
		default boolean mapBoolean(Predicate<T> f) {
			try {
				return f.test(get());
			}
			catch (IllegalArgumentException  e) {
				throw new PropertyConvertException("unable to convert to boolean: ", this, e);
			}
		}
		
		default <R> Property<R> property(Function<T,R> f) {
			return new PropertyKey<>(this, () -> map(f));
		}
		
		default Optional<Property<T>> notMissing() {
			if (this.orNull() == null) {
				return Optional.empty();
			}

			return Optional.of(this);
		}
		
		default T get() {
			T v = orNull();
			if (v == null) {
				throw new PropertyMissingException(this);
			}
			return v;
		}
		
		public @Nullable T orNull();
		
		default void set(BiConsumer<String, T> consumer) {
			consumer.accept(name(), get());
		}
		
		default void set(Consumer<T> consumer) {
			consumer.accept(get());
		}
	}
	
	public static class PropertyMissingException extends NullPointerException {
		private static final long serialVersionUID = 1L;
		private final Key key;
		
		public PropertyMissingException(Key key) {
			super("Property missing for: " + key.description());
			this.key = key;
		}
		
		public Key getKey() {
			return key;
		}
		
	}
	
	public static class PropertyConvertException extends IllegalArgumentException {
		private static final long serialVersionUID = 1L;
		private final Key key;
		
		public PropertyConvertException(String message, Key key, Exception cause) {
			super(message + key.description(), cause);
			this.key = key;
		}
		
		public Key getKey() {
			return key;
		}
		
	}

	public record MissingProperty<T>(String name, ConfigInfo info) implements Property<T>{
		@Override
		public @Nullable T orNull() {
			return null;
		}
	}
	
	public record PropertyKey<T>(Key key, Supplier<T> value) implements Property<T> {
		@Override
		public String name() {
			return key.name();
		}

		@Override
		public @Nullable T orNull() {
			return value.get();
		}
	}
	
	public record PropertyKeyValue(KeyValue key, String name, ConfigInfo info) implements Property<String> {
		
		@Override
		public @Nullable String orNull() {
			return key.orNull();
		}
		
		@Override
		public String description() {
			return name() + " @ [" + info.description() + "]" + name();
		}
		
	}
	
	public record KeyValue(
			String name, //
			String value, //
			String rawValue, //
			String sourceName, //
			int sourceOrdinal) implements Property<String>, Entry<String, String> {
		
		static KeyValue of(Entry<String,String> e, 
				String sourceName,
				int index) {
			
			if (e instanceof KeyValue kv) {
				return kv;
			}
			return new KeyValue(e.getKey(), e.getValue(), e.getValue(), sourceName, index);
		}
		
		@Override
		public @Nullable String orNull() {
			return value;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String getKey() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String setValue(
				String value) {
			throw new UnsupportedOperationException();
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.getClass().getSimpleName())
			.append("(").append(description()).append(")");
			return sb.toString();
		}
		
		public StringBuilder description(StringBuilder sb) {
			sb.append(name());
			if (! sourceName.isBlank()) {
				sb.append(" <-- [").append(sourceName).append(":").append(sourceOrdinal).append("]");
			}
			return sb;
		}
		
		@Override
		public String description() {
			return description(new StringBuilder()).toString();
		}
		
		public Property<String> asProperty() {
			return this;
		}

	}
}
