package com.adamgent.petclinic.config;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

public class Suppliers {

	public static <T> Supplier<T> memoize(Supplier<T> delegate) {
		return (delegate instanceof MemoizingSupplier) ? delegate
				: new MemoizingSupplier<>(Objects.requireNonNull(delegate));
	}

	static class MemoizingSupplier<T> implements Supplier<T>, Serializable {

		final Supplier<T> delegate;

		transient volatile boolean initialized;

		// "value" does not need to be volatile; visibility piggy-backs
		// on volatile read of "initialized".
		transient T value;

		@SuppressWarnings("null")
		MemoizingSupplier(Supplier<T> delegate) {
			this.delegate = delegate;
		}

		@Override
		public T get() {
			// A 2-field variant of Double Checked Locking.
			if (!initialized) {
				synchronized (this) {
					if (!initialized) {
						T t = delegate.get();
						value = t;
						initialized = true;
						return t;
					}
				}
			}
			return value;
		}

		@Override
		public String toString() {
			return "Suppliers.memoize(" + delegate + ")";
		}

		private static final long serialVersionUID = 0;

	}

}