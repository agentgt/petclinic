package com.adamgent.petclinic.config;

public class Component<T> implements AutoCloseable {

	private final T object;

	public static <T> Component<T> of(T object) {
		return new Component<T>(object);
	}

	public Component(T object) {
		super();
		this.object = object;
	}

	@Override
	public void close() throws Exception {
		if (object instanceof AutoCloseable a) {
			a.close();
		}
	}

	public T get() {
		return object;
	}

}
