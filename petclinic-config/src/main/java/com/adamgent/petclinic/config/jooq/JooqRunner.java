package com.adamgent.petclinic.config.jooq;

import java.nio.file.Path;

import org.jooq.codegen.GenerationTool;

import com.adamgent.petclinic.config.DataSourceConfig;

import io.jstach.jstache.JStache;
import io.jstach.jstache.JStacheConfig;
import io.jstach.jstache.JStacheName;
import io.jstach.jstachio.JStachio;

public class JooqRunner {

	public static void run(DataSourceConfig dataSourceConfig, Path cwd) throws Exception {
		String xml = toXmlConfig(dataSourceConfig, cwd);
		GenerationTool.generate(xml);

	}

	public static String toXmlConfig(DataSourceConfig dataSourceConfig, Path cwd) {
		String generatorName = CustomJavaGenerator.class.getName();
		String packageName = "com.adamgent.petclinic.db";
		String relativeTargetDirectory = "target/generated-sources/jooq";

		String targetDirectory = cwd.resolve(relativeTargetDirectory).toString();

		String generatorStrategy = CustomJooqGeneratorStrategy.class.getName();
		JooqGeneratorConfig config = new JooqGeneratorConfig(dataSourceConfig, targetDirectory, generatorName,
				packageName, generatorStrategy);
		String xml = JStachio.render(config);
		return xml;
	}

	// If you are wondering why this is not a record... eclipse has issues
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=575499
	// Edit apparently it still happens even on the below class.
	@JStacheConfig(naming = @JStacheName(suffix = "Template"))
	@JStache(path = "config/jooq-config.xml")
	public static class JooqGeneratorConfig {

		public final DataSourceConfig database;

		public final String targetDirectory;

		public final String generatorName;

		public final String packageName;

		public final String generatorStrategy;

		public JooqGeneratorConfig(DataSourceConfig database, String targetDirectory, String generatorName,
				String packageName, String generatorStrategy) {
			super();
			this.database = database;
			this.generatorName = generatorName;
			this.targetDirectory = targetDirectory;
			this.packageName = packageName;
			this.generatorStrategy = generatorStrategy;
		}

	}

}
