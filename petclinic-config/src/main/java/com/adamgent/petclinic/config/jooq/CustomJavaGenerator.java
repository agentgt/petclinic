package com.adamgent.petclinic.config.jooq;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jooq.codegen.GeneratorStrategy.Mode;
import org.jooq.codegen.JavaGenerator;
import org.jooq.codegen.JavaWriter;
import org.jooq.meta.TableDefinition;
import org.jooq.tools.JooqLogger;

@NonNullByDefault({})
public class CustomJavaGenerator extends JavaGenerator {

	private final ThreadLocal<@Nullable JavaWriter> currentJavaWriter = new ThreadLocal<>();

	private final ConcurrentHashMap<String, String> packageMap = new ConcurrentHashMap<>();

	@Override
	protected void generateRecord(TableDefinition table) {
		String packageName = getStrategy().getJavaPackageName(table, Mode.RECORD);
		File recordFile = getFile(table, Mode.RECORD);
		if (packageName != null && packageMap.putIfAbsent(packageName, packageName) == null) {
			File packageDirectory = recordFile.getParentFile();
			File packageInfoFile = new File(packageDirectory, "package-info.java");
			JavaWriter packageInfo = newJavaWriter(packageInfoFile);
			packageInfo.println("@" + "org.eclipse.jdt.annotation.NonNullByDefault");
			packageInfo.println("package " + packageName + ";");
			closeJavaWriter(packageInfo);
		}
		JavaWriter out = newJavaWriter(getFile(table, Mode.RECORD));

		if (currentJavaWriter.get() != null) {
			log.error("Java writer wasn't cleared from thread local");
		}
		currentJavaWriter.set(out);
		try {
			log.info("Generating record", out.file().getName());
			generateRecord(table, out);
		}
		finally {
			currentJavaWriter.remove();
		}
		closeJavaWriter(out);
	}

	@Override
	public boolean generateConstructorPropertiesAnnotationOnRecords() {
		JavaWriter out = currentJavaWriter.get();
		// if out is not null then we know we are generating a record
		if (out != null) {
			out.println("@%s", Default.class);
		}

		return super.generateConstructorPropertiesAnnotationOnRecords();
	}

	private static final JooqLogger log = JooqLogger.getLogger(CustomJavaGenerator.class);

}
