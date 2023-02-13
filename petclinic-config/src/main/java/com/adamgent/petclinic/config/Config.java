package com.adamgent.petclinic.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import com.adamgent.petclinic.config.Config.ConfigEntry;
import com.adamgent.petclinic.config.Config.Property;
import com.adamgent.petclinic.config.Config.PropertyString;

public interface Config extends Iterable<ConfigEntry> {

	/*
	 * Static factory methods
	 */

	public static Config empty() {
		return new DefaultConfig(Map.of());
	}

	public static Config of(Iterable<? extends ConfigEntry> keyValues) {
		final Map<String, ConfigEntry> kvs = new LinkedHashMap<>();
		for (var e : keyValues) {
			kvs.put(e.name(), e);
			;
		}
		return new DefaultConfig(kvs);
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

	public Stream<ConfigEntry> stream();

	public PropertyString property(String name);

	default Function<String, String> asFunction() {
		/*
		 * This is to for force the function to be our function type
		 */
		PropertyFunction<String, String> pf = this::property;
		return pf;
	}

	@Override
	default Iterator<ConfigEntry> iterator() {
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
		
		default Key combine(Key key) {
			return new CombinedKey(this, key);
		}

	}

	public sealed interface Property<T> extends Key {

		public Key key();
		
		public @Nullable T orNull();
		
		public Property<T> or(
				Supplier<? extends Property<? extends T>> supplier);
		
		public <R> Property<R> map(
				Function<? super T, ? extends R> f);
		
		public <R> Property<R> flatMap(Function<? super T, ? extends Property<? extends R>> mapper);
		
		public T orElse(T fallback);

		public boolean isMissing();

		public T get();
		
		default <R> R to(Function<? super T, ? extends R> f) {
			try {
				return Objects.requireNonNull(f.apply(get()), 
						"conversion function returned nulled for key: " + key().name());
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
		
		default String name() {
			return key().name();
		}
		
		default String description() {
			return key().description();
		}

		default void set(BiConsumer<String, T> consumer) {
			consumer.accept(name(), get());
		}

		default void set(Consumer<T> consumer) {
			consumer.accept(get());
		}

	}
	
	public non-sealed interface ValueProperty<T> extends Property<T> {
		
		default Property<T> or(
				Supplier<? extends Property<? extends T>> supplier) {
			return this;
		}
		
		default <R> Property<R> map(
				Function<? super T, ? extends R> f) {
			R r = to(f);
			return new SupplierProperty<>(this, () -> to(f), r);
		}
		
		@Override
		default <R> Property<R> flatMap(
				Function<? super T, ? extends Property<? extends R>> mapper) {
			var property = mapper.apply(get());
			R initial = property.get();
			Supplier<R> supplier = () -> mapper.apply(get()).get();
			Key key = key().combine(property);
			return new SupplierProperty<>(key, supplier, initial);
		}

		default T orElse(T fallback) {
			return get();
		}

		default boolean isMissing() {
			return false;
		}

		public T get();
		
		@Override
		default @Nullable T orNull() {
			return get();
		}
		
	}
	
	public record MissingProperty<T> (Key key) implements PropertyString {
		@Override
		public @Nullable String orNull() {
			return null;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <R> Property<R> map(
				Function<? super String, ? extends R> f) {
			//Missing can be everything kind of like null;
			return (Property<R>) this;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <R> Property<R> flatMap(
				Function<? super String, ? extends Property<? extends R>> mapper) {
			return (Property<R>) this;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Property<String> or(
				Supplier<? extends Property<? extends String>> supplier) {
			return (Property<String>) supplier.get();
		}

		@Override
		public String orElse(
				String fallback) {
			return fallback;
		}

		@Override
		public boolean isMissing() {
			return true;
		}

		@Override
		public String get() {
			throw new PropertyMissingException(this);
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

	public static class PropertyMissingException extends NoSuchElementException {

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
	
	public sealed interface ConfigEntry extends PropertyString, ValueProperty<String> {
		
		@SuppressWarnings("exports")
		default ConfigEntry withSupplier(Supplier<@Nullable ConfigEntry> supplier) {
			return new ConfigEntrySupplier(supplier, this);
		}
	}

	
	@SuppressWarnings("exports")
	public record ConfigEntrySupplier( //
			Supplier<@Nullable ConfigEntry> supplier, //
			ConfigEntry initialValue) implements ConfigEntry {
		
		@Override
		public String get() {
			return resolve().get();
		}
		
		private ConfigEntry resolve() {
			ConfigEntry v = supplier.get();
			if (v != null) return v;
			return initialValue;
		}
		
		@Override
		public Key key() {
			return resolve().key();
		}
		
	}
	
	
	public record KeyValue( //
			String name, //
			String value, //
			String rawValue, //
			String sourceName, //
			int sourceOrdinal) implements ConfigEntry {

		public static KeyValue of(Entry<String, String> e, String sourceName, int index) {
			return new KeyValue(e.getKey(), e.getValue(), e.getValue(), sourceName, index);
		}
		
		
		public static KeyValue of(String name, String value) {
			return new KeyValue(name, value, value, name, -1);
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
		public String get() {
			return value;
		}

		@Override
		public String name() {
			return name;
		}
		
		@Override
		public Key key() {
			return this;
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

record CombinedKey(Config.Key left, Config.Key right) implements Config.Key{
	@Override
	public String name() {
		return left.name();
	}
	@Override
	public String description() {
		return left.description() + " -> " + right.description();
	}
}

record SupplierProperty<T> (Config.Key key, Supplier<@Nullable T> supplier, T initialValue) implements Config.ValueProperty<T> {

	@Override
	public T get() {
		T t = supplier.get();
		if (t == null) {
			return initialValue;
		}
		return t;
	}
}

record SupplierStringProperty(Config.Key key, Supplier<@Nullable String> supplier, String initialValue) implements PropertyString, Config.ValueProperty<String> {

	@Override
	public String get() {
		String t = supplier.get();
		if (t == null) {
			return initialValue;
		}
		return t;
	}
	
	@Override
	public <R> Property<R> map(
			Function<? super String, ? extends R> f) {
		R r = to(f);
		return new SupplierProperty<>(this, () -> to(f), r);
	}

}
