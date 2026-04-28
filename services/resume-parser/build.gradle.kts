plugins {
	java
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.talentsaurus"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.apache.pdfbox:pdfbox:3.0.3")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("com.fasterxml.jackson.core:jackson-databind")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Monorepo: private PDFs + golden JSON live under ../../examples/resumes-private (gitignored contents).
val privateResumesDir = layout.projectDirectory.asFile.toPath().resolve("../../examples/resumes-private").normalize()

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("talentsaurus.resumes-private", privateResumesDir.toAbsolutePath().toString())
}

tasks.register<JavaExec>("dumpResumeSamples") {
	group = "help"
	description = "Print per-page text from examples/resumes/resume-samples-1.pdf (for parser design)"
	classpath = sourceSets["test"].runtimeClasspath
	mainClass = "com.talentsaurus.resumeparser.support.ResumeSamplesPdfDump"
	workingDir = layout.projectDirectory.asFile
}

tasks.register<JavaExec>("dumpPrivateResumeText") {
	group = "help"
	description = "Print extracted text + parser output for a private PDF (pass path as first arg, or default Rupert sample)"
	classpath = sourceSets["test"].runtimeClasspath
	mainClass = "com.talentsaurus.resumeparser.support.PrivateResumeTextDump"
	workingDir = layout.projectDirectory.asFile
	val defaultPdf =
		layout.projectDirectory.asFile.toPath().resolve("../../examples/resumes-private/Rupert Resume1-15-26.pdf").normalize()
	if (project.hasProperty("dumpPdf")) {
		args(project.property("dumpPdf").toString())
	} else {
		args(defaultPdf.toString())
	}
}

tasks.register<Test>("privateGoldenTests") {
	group = "verification"
	description =
		"Validate PDF + JSON pairs under examples/resumes-private (run after goldens match parser output)"
	useJUnitPlatform()
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	systemProperty("talentsaurus.resumes-private", privateResumesDir.toAbsolutePath().toString())
	systemProperty("talentsaurus.run-private-goldens", "true")
	filter {
		includeTestsMatching("*PrivateResumeGoldenFilesTest*")
	}
}
