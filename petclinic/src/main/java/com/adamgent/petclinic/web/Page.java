package com.adamgent.petclinic.web;

import java.util.Collection;
import java.util.EnumSet;

import io.jstach.jstache.JStacheLambda;

public interface Page {

	default Collection<MenuItem> menuItems() {
		return EnumSet.allOf(MenuItem.class);
	}

	@JStacheLambda(template = "{{#.}}{{>@section}}{{/.}}")
	default boolean active(MenuItem item) {
		return item == menuItem();
	}

	public MenuItem menuItem();

}
