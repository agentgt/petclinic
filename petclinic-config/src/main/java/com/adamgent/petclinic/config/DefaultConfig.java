package com.adamgent.petclinic.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

class DefaultConfig implements Config {

	private final Map<String, ConfigEntry> keyValues;

	// We could probably use a blocking queue instead of doing the locks ourselves
	// But having explicit control is nice
	private final ConcurrentLinkedQueue<Event> queue = new ConcurrentLinkedQueue<>();

	private final CopyOnWriteArrayList<Consumer<? super Event>> listeners = new CopyOnWriteArrayList<>();

	private final Lock queueLock = new ReentrantLock();

	private final AtomicLong version = new AtomicLong(1);

	private final Executor eventExecutor;

	DefaultConfig(Iterable<? extends ConfigEntry> entries, Executor eventExecutor) {
		super();
		this.keyValues = fillMap(new ConcurrentHashMap<>(), entries);
		this.eventExecutor = eventExecutor;
	}

	DefaultConfig(Iterable<? extends ConfigEntry> entries) {
		this(entries, Runnable::run);
	}

	static DefaultConfig of(Map<String, String> m) {
		List<KeyValue> kvs = KeyValue.of(m.entrySet().iterator(), "");
		return new DefaultConfig(kvs);
	}

	static Map<String, ConfigEntry> fillMap(Map<String, ConfigEntry> keyValues,
			Iterable<? extends ConfigEntry> entries) {
		for (var e : entries) {
			keyValues.put(e.name(), e);
		}
		return keyValues;
	}

	protected @Nullable ConfigEntry get(String name) {
		return keyValues.get(name);
	}

	PropertyString _property(String name) {
		var ce = get(name);
		if (ce == null) {
			return PropertyString.missing(name);
		}
		return ce;
	}

	public PropertyString property(String name) {
		PropertyString current = _property(name);
		return PropertyString.of(current.key(), () -> _property(name));
	}

	Snapshot snapshot() {
		Map<String, ConfigEntry> state = new LinkedHashMap<>();
		// We wait for keyValues to be updated if that is in process
		// So that we get a clean snapshot
		queueLock.lock();
		try {
			state.putAll(keyValues);
			return new Snapshot(state, version.get());
		}
		finally {
			queueLock.unlock();
		}
	}

	record Snapshot(Map<String, ConfigEntry> snapshot, long version) {
	}

	@Override
	public void onEvent(Consumer<? super Event> consumer) {
		listeners.add(consumer);
	}

	@Override
	public void publish(Event event) {
		fire(event);
	}

	@Override
	public EventBuilder eventBuilder() {
		var snapshot = snapshot();
		var builder = new DefaultEventBuilder(snapshot.snapshot(), snapshot.version());
		return builder;
	}

	private static class DefaultEventBuilder implements EventBuilder {

		private final Map<String, ConfigEntry> snapshot;

		private String description = "";

		private boolean update = true;

		private final long version;

		public DefaultEventBuilder(Map<String, ConfigEntry> snapshot, long version) {
			super();
			this.snapshot = snapshot;
			this.version = version;
		}

		@Override
		public Map<String, ConfigEntry> snapshot() {
			return snapshot;
		}

		@Override
		public EventBuilder description(String description) {
			this.description = Objects.requireNonNull(description);
			return this;
		}

		@Override
		public EventBuilder update(boolean update) {
			this.update = update;
			return this;
		}

		@Override
		public Event build() {
			return new DefaultEvent(snapshot, description, update, version);
		}

	}

	private void fire(Event event) {
		queue.add(event);
		while (!queue.isEmpty()) {
			List<Event> events;
			// START LOCK
			if (queueLock.tryLock()) {
				try {
					events = new ArrayList<>();
					Event current = null;
					// We use the last event marked for update
					Event updateEvent = null;
					while ((current = queue.poll()) != null) {
						events.add(current);
						if (current.update()) {
							updateEvent = current;
						}
					}
					if (updateEvent != null) {
						keyValues.clear();
						keyValues.putAll(updateEvent.snapshot());
						version.incrementAndGet();
					}
				}
				finally {
					queueLock.unlock();
				}
				// END LOCK
			}
			else {
				events = List.of();
			}
			for (var e : events) {
				for (var listener : listeners) {
					eventExecutor.execute(() -> listener.accept(e));
				}
			}
		}
	}

	private record DefaultEvent(Map<String, ConfigEntry> snapshot, String description, boolean update,
			long version) implements Event {

	}

	private static final Comparator<Entry<String, ConfigEntry>> _COMPARATOR = (a, b) -> ConfigEntry.COMPARATOR
			.compare(a.getValue(), b.getValue());

	private static final Comparator<Entry<String, ConfigEntry>> COMPARATOR = _COMPARATOR.thenComparing(Entry::getKey);

	public Stream<Entry<String, ConfigEntry>> stream() {
		return keyValues.entrySet().stream().map(e -> Map.entry(e.getKey(), e.getValue())).sorted(COMPARATOR);
	}

	// private static final Comparator<ConfigEntry> COMPARATOR =
	// Comparator.comparingInt(ConfigEntry::ordinal);

}