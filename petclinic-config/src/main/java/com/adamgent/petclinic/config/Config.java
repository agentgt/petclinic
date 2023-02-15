package com.adamgent.petclinic.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedMap;
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
import com.adamgent.petclinic.config.Config.Key;
import com.adamgent.petclinic.config.Config.Key.MissingKey;
import com.adamgent.petclinic.config.Config.MissingProperty;
import com.adamgent.petclinic.config.Config.Property;
import com.adamgent.petclinic.config.Config.PropertyString;
import com.adamgent.petclinic.config.Config.ValueProperty;

public interface Config extends Iterable<Entry<String, ConfigEntry>> {

	/*
	 * Static factory methods
	 */

	public static Config empty() {
		return new DefaultConfig(List.of());
	}

	public static Config of(Iterable<? extends ConfigEntry> keyValues) {
		return new DefaultConfig(keyValues);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private List<ConfigEntry> entries = new ArrayList<>();

		private Map<String, Integer> counters = new HashMap<>();

		public Builder add(ConfigEntry e) {
			entries.add(e);
			return this;
		}

		public Builder add(String name, String value, String source) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(value);
			Objects.requireNonNull(source);
			int index = counters.computeIfAbsent(source, _s -> 0);
			return add(new KeyValue(name, value, value, index, source, 0));
		}

		public Builder add(String name, String value) {
			return add(name, value, "");
		}

		public Builder add(Entry<String, String> e, String source) {
			return add(e.getKey(), e.getValue(), source);
		}

		public Builder add(Entry<String, String> e) {
			return add(e.getKey(), e.getValue(), "");
		}

		public Builder add(Map<String, String> m, String sourceName) {
			if (m instanceof LinkedHashMap || m instanceof SortedMap) {
				for (var e : m.entrySet()) {
					add(e, sourceName);
				}
				return this;
			}
			List<String> keys = new ArrayList<>(m.keySet());
			keys.sort(Comparator.naturalOrder());
			for (var k : keys) {
				var v = m.get(k);
				add(k, v, sourceName);
			}
			return this;
		}

		public Builder add(Map<String, String> m) {
			return add(m, "");
		}

		public List<ConfigEntry> entries() {
			return entries;
		}

		public Config build() {
			return build(Config::of);
		}

		public Config build(Function<List<ConfigEntry>, Config> f) {
			return f.apply(entries);
		}

	}

	/*
	 * Required methods to implement
	 */

	public Stream<Entry<String, ConfigEntry>> stream();

	public PropertyString property(String name);

	default Function<String, String> asFunction() {
		/*
		 * This is to for force the function to be our function type
		 */
		PropertyFunction<String, String> pf = this::property;
		return pf;
	}

	@Override
	default Iterator<Entry<String, ConfigEntry>> iterator() {
		return stream().iterator();
	}

	default Map<String, String> toMap() {
		Map<String, String> r = new LinkedHashMap<>();
		for (var p : this) {
			r.put(p.getKey(), p.getValue().get());
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

		public record MissingKey(String name) implements Key {

		}

	}

	public interface Event {

		default String description() {
			return "";
		}

		default boolean update() {
			return true;
		}

		default Map<String, ConfigEntry> snapshot() {
			return Map.of();
		}

		default long version() {
			return 0;
		}

	}

	public interface EventBuilder {

		// This snapshot is mutable
		Map<String, ConfigEntry> snapshot();

		EventBuilder description(String description);

		EventBuilder update(boolean update);

		default PropertyString property(String name) {
			Objects.requireNonNull(name);
			var e = snapshot().get(name);
			if (e == null) {
				return PropertyString.missing(name);
			}
			return e;
		}

		default EventBuilder put(String key, ConfigEntry keyValue) {
			Objects.requireNonNull(key);
			Objects.requireNonNull(keyValue);
			snapshot().put(key, keyValue);
			return this;
		}

		default EventBuilder add(ConfigEntry keyValue) {
			snapshot().put(keyValue.name(), keyValue);
			return this;
		}

		default EventBuilder put(String key, String value) {
			Objects.requireNonNull(key);
			Objects.requireNonNull(value);
			return put(key, KeyValue.of(key, value));
		}

		default EventBuilder remove(String key) {
			Objects.requireNonNull(key);
			snapshot().remove(key);
			return this;
		}

		default EventBuilder clear() {
			snapshot().clear();
			return this;
		}

		Event build();

	}

	void onEvent(Consumer<? super Event> consumer);

	default void publish(Consumer<? super EventBuilder> eventProducer) {
		var builder = eventBuilder();
		eventProducer.accept(builder);
		publish(builder.build());
	}

	void publish(Event event);

	EventBuilder eventBuilder();

	public sealed interface Property<T> {

		@SuppressWarnings("exports")
		public static <T> Property<T> ofSupplier(Key key, Supplier<@Nullable T> supplier) {
			Objects.requireNonNull(key);
			var t = supplier.get();
			if (t == null) {
				return missing(key);
			}
			return new SupplierProperty<>(key, supplier, t);
		}

		public static <T> ValueProperty<T> of(Key key, T value) {
			Objects.requireNonNull(key);
			Objects.requireNonNull(value);
			return new EagerProperty<>(key, value);
		}

		public static <T> MissingProperty<T> missing(Key key) {
			return new TypedMissingProperty<>(key);
		}

		public Key key();

		public @Nullable T orNull();

		public Property<T> or(Supplier<? extends Property<? extends T>> supplier);

		public <R> Property<R> map(Function<? super T, ? extends R> f);

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
				throw new PropertyConvertException(key(), e);
			}
		}

		default int toInt(ToIntFunction<T> f) {
			try {
				return f.applyAsInt(get());
			}
			catch (IllegalArgumentException e) {
				throw new PropertyConvertException(key(), e);
			}
		}

		default long toLong(ToLongFunction<T> f) {
			try {
				return f.applyAsLong(get());
			}
			catch (IllegalArgumentException e) {
				throw new PropertyConvertException(key(), e);
			}
		}

		default boolean toBoolean(Predicate<T> f) {
			try {
				return f.test(get());
			}
			catch (IllegalArgumentException e) {
				throw new PropertyConvertException(key(), e);
			}
		}

		default String name() {
			return key().name();
		}

		default String description() {
			return key().description();
		}

	}

	public sealed interface ValueProperty<T> extends Property<T> {

		default Property<T> or(Supplier<? extends Property<? extends T>> supplier) {
			return this;
		}

		default <R> Property<R> map(Function<? super T, ? extends R> f) {
			R r = to(f);
			return new SupplierProperty<>(this.key(), () -> to(f), r);
		}

		@Override
		default <R> Property<R> flatMap(Function<? super T, ? extends Property<? extends R>> mapper) {
			var property = mapper.apply(get());
			Key key = key().combine(property.key());
			R initial = property.orNull();
			if (initial == null) {
				return Property.missing(key);
			}
			Supplier<R> supplier = () -> mapper.apply(get()).get();
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

		default void set(BiConsumer<String, T> consumer) {
			consumer.accept(name(), get());
		}

		default void set(Consumer<T> consumer) {
			consumer.accept(get());
		}

		@SuppressWarnings("exports")
		public static <T> ValueProperty<T> of(Config.Key key, Supplier<@Nullable T> supplier, T initialValue) {
			return new SupplierProperty<>(key, supplier, initialValue);
		}

	}

	public sealed interface SealedProperty<T> extends Property<T> {

	}

	public sealed interface SealedValueProperty<T> extends ValueProperty<T>, SealedProperty<T> {

	}

	public sealed interface MissingProperty<T> extends SealedProperty<T> {

		@Override
		default @Nullable T orNull() {
			return null;
		}

		@Override
		default T orElse(T fallback) {
			return fallback;
		}

		@Override
		default boolean isMissing() {
			return true;
		}

		@Override
		default T get() {
			throw new PropertyMissingException(key());
		}

		@Override
		default <R> MissingProperty<R> map(Function<? super T, ? extends R> f) {
			return new TypedMissingProperty<>(key());
		}

		@Override
		default <R> MissingProperty<R> flatMap(Function<? super T, ? extends Property<? extends R>> mapper) {
			return new TypedMissingProperty<>(key());
		}

		@SuppressWarnings("unchecked")
		@Override
		default Property<T> or(Supplier<? extends Property<? extends T>> supplier) {
			return (Property<T>) supplier.get();
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

		default PropertyString mapString(Function<? super String, ? extends String> f) {
			return PropertyString.of(this.map(f));
		}

		public static PropertyString missing(String name) {
			return new MissingPropertyString(new MissingKey(name));
		}

		public static PropertyString of(Property<String> p) {
			if (p instanceof PropertyString ps)
				return ps;
			if (p.isMissing()) {
				return new MissingPropertyString(p.key());
			}
			String initialValue = p.get();
			return new SupplierPropertyString(p.key(), () -> p.orNull(), initialValue);
		}

		@SuppressWarnings("exports")
		public static PropertyString of(Config.Key key, Supplier<@Nullable String> supplier, String initialValue) {
			return new SupplierPropertyString(key, supplier, initialValue);
		}

		public static PropertyString of(Config.Key key, Supplier<? extends Property<String>> supplier) {
			var prop = supplier.get();
			if (prop.isMissing()) {
				return of(prop);
			}
			return new SupplierPropertyString(key, () -> supplier.get().orNull(), prop.get());
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

	public sealed interface ConfigEntry extends PropertyString, SealedValueProperty<String> {

		public static Comparator<ConfigEntry> COMPARATOR = Comparator.comparing(ConfigEntry::sourceOrdinal).reversed()
				.thenComparing(ConfigEntry::sourceName).thenComparing(ConfigEntry::index)
				.thenComparing(ConfigEntry::name);

		default Map.Entry<String, String> toEntry() {
			return Map.entry(name(), get());
		}

		public String sourceName();

		public int index();

		public int sourceOrdinal();

	}

	public record KeyValue( //
			String name, //
			String value, //
			String rawValue, //
			int index, //
			String sourceName, //
			int sourceOrdinal) implements ConfigEntry, Key {

		public static KeyValue of(Entry<String, String> e, String sourceName, int index) {
			return new KeyValue(e.getKey(), e.getValue(), e.getValue(), index, sourceName, 0);
		}

		public static KeyValue of(String name, String value) {
			return new KeyValue(name, value, value, 0, name, -1);
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

	}

}

record SupplierPropertyString( //
		Config.Key key, //
		Supplier<@Nullable String> supplier, //
		String initialValue) implements PropertyString, ValueProperty<String> {

	@Override
	public String get() {
		String t = supplier.get();
		if (t == null) {
			return initialValue;
		}
		return t;
	}

	@Override
	public <R> Property<R> map(Function<? super String, ? extends R> f) {
		R r = to(f);
		return new SupplierProperty<>(this.key(), () -> to(f), r);
	}

}

record TypedMissingProperty<T> (Key key) implements MissingProperty<T> {
}

record MissingPropertyString(Key key) implements PropertyString, MissingProperty<String> {

	@Override
	public PropertyString mapString(Function<? super String, ? extends String> f) {
		return this;
	}
}

record UserEvent(Map<String, ConfigEntry> snapshot, String description, boolean update) implements Config.Event {
	UserEvent {
		Objects.requireNonNull(snapshot);
		Objects.requireNonNull(description);
		snapshot = Map.copyOf(snapshot);
	}
}

record CombinedKey(Config.Key left, Config.Key right) implements Config.Key {
	@Override
	public String name() {
		return left.name();
	}

	@Override
	public String description() {
		return left.description() + " -> " + right.description();
	}
}

record SupplierProperty<T> (Config.Key key, Supplier<@Nullable T> supplier,
		T initialValue) implements Config.ValueProperty<T> {

	@Override
	public T get() {
		T t = supplier.get();
		if (t == null || t == this) {
			return initialValue;
		}
		return t;
	}
}

record EagerProperty<T> (Config.Key key, T value) implements Config.ValueProperty<T> {
	EagerProperty {
		key = Objects.requireNonNull(key);
		value = Objects.requireNonNull(value);
	}

	@Override
	public Key key() {
		return key;
	}

	@Override
	public T get() {
		return value;
	}
}
