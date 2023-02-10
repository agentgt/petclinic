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

import com.adamgent.petclinic.config.Config.PropertyString;

public class Config implements Iterable<PropertyString> {

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
				return prefix;
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
	
	public <R> Function<String,R> convert(Function<String, R> f) {
		Function<String,String> o = k -> property(k).get();
		return o.andThen(f);
	}
	
//	private static class ConfigFunction implements Function<String,String> {
//		private final Config config;
//
//		public ConfigFunction(
//				Config config) {
//			super();
//			this.config = config;
//		}
//		@Override
//		public String apply(
//				String t) {
//			return config.property(t).get();
//		}
//		
//		@Override
//		public <V> Function<String, V> andThen(
//				Function<? super String, ? extends V> after) {
//			PropertyFunction<String, String> pf = config::property;
//			return new PropertyFunction<>(config::property).andThen(after);
//		}
//		
//		@Override
//		public <V> Function<V, String> compose(
//				Function<? super V, ? extends String> before) {
//			return new PropertyFunction<>(config::property).compose(before);
//		}
//		
//	}
	

	
//	private record PropertyFunction<T, R> (
//			Function<? super T, ? extends Property<? extends R>> property) implements Function<T, R> {
//
//		@Override
//		public R apply(
//				T t) {
//			return property.apply(t)
//				.get();
//		}
//
//		@Override
//		public <V> PropertyFunction<T, V> andThen(
//				Function<? super R, ? extends V> after) {
//			Function<? super T, ? extends Property<? extends V>> f = t -> property.apply(t)
//				.map(after);
//			return new PropertyFunction<>(f);
//		}
//
//		@Override
//		public <V> PropertyFunction<V, R> compose(
//				Function<? super V, ? extends T> before) {
//			Function<? super V, ? extends Property<? extends R>> f = v -> {
//				T t = before.apply(v);
//				return property.apply(t);
//			};
//			return new PropertyFunction<>(f);
//		}
//	}
	
	public Function<String,String> asFunction() {
		PropertyFunction<String,String> pf = this::property;
		return pf;
	}
	
	public interface PropertyFunction<T, R> extends Function<T, R> {

		@Override
		default R apply(
				T t) {
			return property(t)
				.get();
		}
		
		public Property<? extends R> property(T t);

		@Override
		default <V> PropertyFunction<T, V> andThen(
				Function<? super R, ? extends V> after) {
			return t -> property(t).map(after);
		}

		@Override
		default <V> PropertyFunction<V, R> compose(
				Function<? super V, ? extends T> before) {
			return t -> property(before.apply(t));
		}
	}
	

		
	@Override
	public Iterator<PropertyString> iterator() {
		return stream().iterator();
	}
	
	public Stream<PropertyString> stream() {
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
			return "\"" + name() + "\"";
		}
		
		default String path() {
			return name();
		}
		
	}
	
	public PropertyString property(
			String name) {
		String path = prefix + name;
		KeyValue kv = get(path);
		var prop = toProperty(kv);
		if (prop == null) {
			return new MissingProperty<>(name, info);
		}
		return toProperty(kv);
	}

	private @Nullable PropertyString toProperty(
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
		default <R> R to(Function<? super T,R> f) {
			try {
				return f.apply(get());
			}
			catch (IllegalArgumentException  e) {
				throw new PropertyConvertException("unable to convert: ", this, e);
			}
		}
		
		default int toInt(ToIntFunction<T> f) {
			try {
				return f.applyAsInt(get());
			}
			catch (IllegalArgumentException  e) {
				throw new PropertyConvertException("unable to convert to int: ", this, e);
			}
		}
		
		default long toLong(ToLongFunction<T> f) {
			try {
				return f.applyAsLong(get());
			}
			catch (IllegalArgumentException  e) {
				throw new PropertyConvertException("unable to convert to long: ", this, e);
			}
		}
		
		default boolean toBoolean(Predicate<T> f) {
			try {
				return f.test(get());
			}
			catch (IllegalArgumentException  e) {
				throw new PropertyConvertException("unable to convert to boolean: ", this, e);
			}
		}
		
		default <R> Property<R> map(Function<? super T, ? extends R> f) {
			return new PropertyKey<>(this, () -> to(f));
		}
		
		default Optional<T> toOptional() {
			return Optional.ofNullable(orNull());
		}
		
		default T orElse(T fallback) {
			var e = orNull();
			if (e == null) {
				return fallback;
			}
			return e;
		}
		
		default boolean isMissing() {
			return orNull() == null;
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
	
	public sealed interface PropertyString extends Property<String> {
		default int toInt() {
			return toInt(Integer::parseInt);
		}
		
		default long toLong() {
			return toLong(Long::parseLong);
		}
		
		default boolean toBoolean() {
			return toBoolean(Boolean::parseBoolean);
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

	public record MissingProperty<T>(String name, ConfigInfo info) implements PropertyString{
		@Override
		public @Nullable String orNull() {
			return null;
		}
		
		@Override
		public String description() {
			return PropertyString.super.description() + " @ [" + info.description() + "]" + name();
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
	
	public record PropertyKeyValue(KeyValue key, String name, ConfigInfo info) implements PropertyString {
		
		@Override
		public @Nullable String orNull() {
			return key.orNull();
		}
		
		@Override
		public String description() {
			return PropertyString.super.description() + " @ [" + info.description() + "]" + name();
		}
		
	}
	
	public record KeyValue(
			String name, //
			String value, //
			String rawValue, //
			String sourceName, //
			int sourceOrdinal) implements PropertyString, Entry<String, String> {
		
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
