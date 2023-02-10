package com.adamgent.petclinic.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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

public interface Config extends Iterable<PropertyString> {

	/*
	 * Static factory methods
	 */

	public static Config empty() {
		return new DefaultConfig(Map.of(), "");
	}

	public static Config of(Iterable<? extends PropertyString> keyValues) {
		final Map<String, PropertyString> kvs = new LinkedHashMap<>();
		for (var e : keyValues) {
			kvs.put(e.name(), e);
			;
		}
		return new DefaultConfig(kvs, "");
	}

	public static Config ofEntries(Iterable<? extends Entry<String, String>> entries, String sourceName) {
		return Config.of(KeyValue.of(entries.iterator(), sourceName));
	}

	public static Config ofEntries(Iterable<? extends Entry<String, String>> entries) {
		return Config.ofEntries(entries, "");
	}

	/*
	 * Required methods to implement
	 */

	public Config withPrefix(String prefix);

	public Stream<PropertyString> stream();

	public PropertyString property(String name);

	default Function<String, String> asFunction() {
		PropertyFunction<String, String> pf = this::property;
		return pf;
	}

	@Override
	default Iterator<PropertyString> iterator() {
		return stream().iterator();
	}

	default Map<String, String> toMap() {
		Map<String, String> r = new LinkedHashMap<>();
		for (var p : this) {
			r.put(p.name(), p.get());
		}
		return r;
	}

	public interface Key {

		String name();

		default String description() {
			return "\"" + name() + "\"";
		}

	}

	public interface Property<T> extends Key {

		default <R> R to(Function<? super T, ? extends R> f) {
			try {
				return f.apply(get());
			}
			catch (IllegalArgumentException e) {
				throw new PropertyConvertException(this, e);
			}
		}

		default int toInt(ToIntFunction<T> f) {
			try {
				return f.applyAsInt(get());
			}
			catch (IllegalArgumentException e) {
				throw new PropertyConvertException(this, e);
			}
		}

		default long toLong(ToLongFunction<T> f) {
			try {
				return f.applyAsLong(get());
			}
			catch (IllegalArgumentException e) {
				throw new PropertyConvertException(this, e);
			}
		}

		default boolean toBoolean(Predicate<T> f) {
			try {
				return f.test(get());
			}
			catch (IllegalArgumentException e) {
				throw new PropertyConvertException(this, e);
			}
		}

		default PropertyString toProperty(Function<? super T, String> f) {
			return new SupplierStringProperty(this, () -> to(f));
		}

		default <R> Property<R> map(Function<? super T, ? extends R> f) {
			return new SupplierProperty<>(this, () -> to(f));
		}

		/*
		 * TODO flatMap... not sure how to combine
		 */

		default <R> Property<R> flatMap(Function<? super T, ? extends Property<? extends R>> f) {
			throw new UnsupportedOperationException();
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

	interface PropertyFunction<T, R> extends Function<T, R> {

		@Override
		default R apply(T t) {
			return property(t).get();
		}

		public Property<? extends R> property(T t);

		@Override
		default <V> PropertyFunction<T, V> andThen(Function<? super R, ? extends V> after) {
			return t -> property(t).map(after);
		}

		@Override
		default <V> PropertyFunction<V, R> compose(Function<? super V, ? extends T> before) {
			return t -> property(before.apply(t));
		}

	}

	public interface PropertyString extends Property<String> {

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

		public PropertyConvertException(Key key, Exception cause) {
			super("For property: " + key.description(), cause);
			this.key = key;
		}

		public Key getKey() {
			return key;
		}

	}

	public record KeyValue(String name, //
			String value, //
			String rawValue, //
			String sourceName, //
			int sourceOrdinal) implements PropertyString {

		public static KeyValue of(Entry<String, String> e, String sourceName, int index) {
			return new KeyValue(e.getKey(), e.getValue(), e.getValue(), sourceName, index);
		}

		public static List<KeyValue> of(Iterator<? extends Entry<String, String>> entries, String sourceName) {
			List<KeyValue> kvs = new ArrayList<>();
			int i = 0;
			while (entries.hasNext()) {
				kvs.add(of(entries.next(), sourceName, i++));
			}
			return kvs;
		}

		@Override
		public @Nullable String orNull() {
			return value;
		}

		@Override
		public String name() {
			return name;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.getClass().getSimpleName()).append("(").append(description()).append(")");
			return sb.toString();
		}

		public StringBuilder description(StringBuilder sb) {
			sb.append(name());
			if (!sourceName.isBlank()) {
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

record SupplierProperty<T> (Config.Key key, Supplier<T> value) implements Config.Property<T> {
	@Override
	public String name() {
		return key.name();
	}

	@Override
	public @Nullable T orNull() {
		return value.get();
	}

	@Override
	public String description() {
		return key.description();
	}
}

record SupplierStringProperty(Config.Key key, Supplier<String> value) implements PropertyString {
	@Override
	public String name() {
		return key.name();
	}

	@Override
	public @Nullable String orNull() {
		return value.get();
	}

	@Override
	public String description() {
		return key.description();
	}
}
