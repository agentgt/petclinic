package com.adamgent.petclinic.config.jooq;

import java.nio.file.Path;

import org.jooq.codegen.GenerationTool;

import com.adamgent.petclinic.config.sql.DataSourceConfig;

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

	@JStacheConfig(naming = @JStacheName(suffix = "Template"))
	@JStache(path = "config/jooq-config.xml")
	public record JooqGeneratorConfig(DataSourceConfig database, String targetDirectory, String generatorName,
			String packageName, String generatorStrategy) {
	}

}
