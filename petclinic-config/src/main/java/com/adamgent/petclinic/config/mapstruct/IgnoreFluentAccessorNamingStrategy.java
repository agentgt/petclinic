package com.adamgent.petclinic.config.mapstruct;

import javax.lang.model.element.ExecutableElement;

import org.eclipse.jdt.annotation.Nullable;
import org.mapstruct.ap.spi.DefaultAccessorNamingStrategy;

public class IgnoreFluentAccessorNamingStrategy extends DefaultAccessorNamingStrategy {

	@Override
	protected boolean isFluentSetter(
			@Nullable ExecutableElement method) {
		return false;
	}
}
