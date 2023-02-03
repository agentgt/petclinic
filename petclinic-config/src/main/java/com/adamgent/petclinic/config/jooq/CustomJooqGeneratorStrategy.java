package com.adamgent.petclinic.config.jooq;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.meta.Definition;

@NonNullByDefault({})
@SuppressWarnings("exports")
public class CustomJooqGeneratorStrategy extends DefaultGeneratorStrategy {

	@Override
	public String getJavaClassName(
			Definition definition,
			Mode mode) {
		/*
		 * Adds suffix of "Table" for table things. Otherwise there is annoying
		 * conflict with the POJOs.
		 */
		if (mode == Mode.DEFAULT || mode == null) {
			return super.getJavaClassName(definition, mode) + "Table";
		}
		else if (mode == Mode.POJO) {
			return "IgnorePojo" + super.getJavaClassName(definition, mode);
		}
		return super.getJavaClassName(definition, mode);

	}

	@Override
	public String getJavaPackageName(
			Definition definition,
			Mode mode) {

		/*
		 * This is to hide jooq things by adding an "internal" package
		 */
		if (mode == Mode.POJO || mode == Mode.INTERFACE) {
			StringBuilder sb = new StringBuilder();
			return sb.append(getTargetPackage())
				.append(".internal.data")
				.toString();
		}
		else {
			String packageName = super.getJavaPackageName(definition, mode);
			String target = Objects.requireNonNull(getTargetPackage());
			if (packageName.startsWith(target)) {
				String suffix = packageName.substring(target.length());
				return target + ".internal" + suffix;
			}
			return packageName;
		}
	}

}