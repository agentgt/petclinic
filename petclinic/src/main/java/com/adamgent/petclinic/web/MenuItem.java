package com.adamgent.petclinic.web;

// From petclinic layouts
public enum MenuItem {

	home("/", "home", "home page", "home", "Home"),
	find_owners("/owners/find", "owners", "find owners", "search", "Find owners"),
	vets("/vets", "vets", "veterinarians", "th-list", "Veterinarians"),
	error("/oups", "error", "trigger a RuntimeException to see how it is handled", "exclamation-triangle", "Error");

	// ink,active,title,glyph,text
	public final String link, _active, title, glyph, text;

	private MenuItem(String link, String _active, String title, String glyph, String text) {
		this.link = link;
		this._active = _active;
		this.title = title;
		this.glyph = glyph;
		this.text = text;
	}

}
