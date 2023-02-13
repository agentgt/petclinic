package com.adamgent.petclinic.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
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

	DefaultConfig(Map<String, ? extends ConfigEntry> keyValues) {
		super();
		this.keyValues = new ConcurrentHashMap<>(keyValues);

	}
	
	@Override
	public EventTransaction beginEvent() {
		Map<String,ConfigEntry> state = snapshot();
		return new DefaultEventTransaction(state);
	}
	
	Map<String, ConfigEntry> snapshot() {
		Map<String,ConfigEntry> state = new LinkedHashMap<>();
		// We wait for keyValues to be updated if that is in process
		// So that we get a clean snapshot
		queueLock.lock();
		try {
			state.putAll(keyValues);
			return state;
		}
		finally {
			queueLock.unlock();
		}
	}
	
	@Override
	public void onEvent(
			Consumer<? super Event> consumer) {
		listeners.add(consumer);
	}
	
	@Override
	public void publish(
			Consumer<? super EventBuilder> eventProducer) {
		var snapshot = snapshot();
		var builder = new DefaultEventBuilder(snapshot);
		eventProducer.accept(builder);
		fire(builder.build());
	}
	
	private static class DefaultEventBuilder implements EventBuilder {
		private final Map<String,ConfigEntry> snapshot;
		private String description = "";
		private boolean update = true;
		
		public DefaultEventBuilder(
				Map<String, ConfigEntry> snapshot) {
			super();
			this.snapshot = snapshot;
		}

		@Override
		public Map<String, ConfigEntry> snapshot() {
			return snapshot;
		}

		@Override
		public EventBuilder description(
				String description) {
			this.description = Objects.requireNonNull(description);
			return this;
		}

		@Override
		public EventBuilder update(
				boolean update) {
			this.update = update;
			return this;
		}

		@Override
		public Event build() {
			return new DefaultEvent(snapshot, description, update);
		}
		
	}
	
	private void fire(
			Event event) {
		queue.add(event);
		while (!queue.isEmpty()) {
			List<Event> events;
			if (queueLock.tryLock()) {
				events = new ArrayList<>();
				try {
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
					}
				}
				finally {
					queueLock.unlock();
				}
			}
			else {
				events = List.of();
			}
			for (var e : events) {
				for (var listener : listeners) {
					listener.accept(e);
				}
			}
		}
	}
	
	private record DefaultEvent(
			Map<String, ConfigEntry> snapshot, 
			String description, 
			boolean update) implements Event {

	}
	private class DefaultEventTransaction implements EventTransaction {
		
		private final Map<String,ConfigEntry> snapshot;
		private volatile DefaultEvent committed = null;
		
		private static final int INIT = 0;
		private static final int COMMIT = 2;
		private static final int ROLLBACK = 4;
		private static final int COMMITTED = 8;
		private static final int ROLLBACKED = 16;
		private static final int CLOSED = 32;
		private static final int ERROR = 64;

		private final AtomicInteger _state = new AtomicInteger(INIT);
		private final Lock lock = new ReentrantLock();

		public DefaultEventTransaction(
				Map<String, ConfigEntry> snapshot) {
			super();
			this.snapshot = snapshot;
		}

		@Override
		public Map<String, ConfigEntry> snapshot() {
			return this.snapshot;
		}
		
		@Override
		public State state() {
			int s = _state.get();
			return switch(s) {
				case INIT -> State.INIT;
				case ROLLBACK, ROLLBACKED -> State.ROLLBACKED;
				case COMMIT, COMMITTED -> State.COMMITTED;
				case CLOSED | ERROR -> State.CLOSED;
				default -> throw new IllegalStateException("bug: " + s);
			};
		}
		
		@Override
		public void commit(
				String description,
				boolean update) {

			lock.lock();
			boolean set = false;
			try {
				if (_state.compareAndSet(INIT, COMMIT)) {
					var copy = Map.copyOf(snapshot);
					committed = new DefaultEvent(copy, description, update);
					if (!_state.compareAndSet(COMMIT, COMMITTED)) {
						throw new IllegalStateException("bug");
					}
					set = true;
				}
				else {
					String stateName = switch (_state.get()) {
						case COMMIT -> "commited";
						case ROLLBACK -> "rollbacked";
						case CLOSED -> "closed";
						case ERROR -> "error";
						default -> throw new IllegalStateException("bug");
					};
					throw new IllegalStateException("Transaction is already: " + stateName);
				}
			}
			finally {
				if (! set) {
					_state.set(ERROR);
				}
				lock.unlock();
			}

		}

		@Override
		public void rollback() {
			lock.lock();
			boolean set = false;
			try {
				if (!_state.compareAndSet(INIT, ROLLBACK)) {
					String stateName = switch (_state.get()) {
						case COMMITTED, COMMIT -> "committed";
						case ROLLBACKED, ROLLBACK -> "rollbacked";
						case CLOSED, ERROR -> "closed";
						default -> throw new IllegalStateException("bug");
					};
					throw new IllegalStateException("Transaction is already: " + stateName);
				}
				else {
					committed = null;
					if (! _state.compareAndSet(ROLLBACK, ROLLBACKED)) {
						throw new IllegalStateException("bug");
					}
					set = true;

				}
			}
			finally {
				if (! set) {
					_state.set(ERROR);
				}
				lock.unlock();
			}

		}

		@Override
		public void close() {
			DefaultEvent event = null;
			lock.lock();
			try {
				int s = _state.get();
				var c = committed;
				switch (s) {
					case COMMITTED -> event = c;
					case ROLLBACKED, ERROR  -> {}
					case INIT -> {
						throw new IllegalStateException("Transaction was not committed or rollbacked");
					}
					case CLOSED -> {
						throw new IllegalStateException("Already closed");
					}
				}
				_state.set(CLOSED);
			}
			finally {
				lock.unlock();
			}
			if (event != null) {
				fire(event);
			}
			
		}
		
	}

	@SuppressWarnings("unchecked")
	public Stream<ConfigEntry> stream() {
		return (Stream<ConfigEntry>) keyValues.values().stream();
	}
	
	private @Nullable ConfigEntry get(String name) {
		var ce = keyValues.get(name);
		if (ce != null) {
			ce = ce.withSupplier(() -> keyValues.get(name));
		}
		return ce;
	}

	public PropertyString property(String name) {
		var ce = get(name);
		if (ce == null) {
			return new MissingProperty<>(new MissingKey(name));
		}
		return ce;
	}
	
	record MissingKey(String name) implements Key {
	}

}