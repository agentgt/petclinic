package com.adamgent.petclinic.config.doma;

import org.seasar.doma.AnnotateWith;
import org.seasar.doma.Annotation;
import org.seasar.doma.AnnotationTarget;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@AnnotateWith(annotations = { @Annotation(target = AnnotationTarget.CLASS, type = Singleton.class),
		@Annotation(target = AnnotationTarget.CONSTRUCTOR, type = Inject.class) })
public @interface DomaSupport {

}
