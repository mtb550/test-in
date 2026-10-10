// Testin API Model from JSON: New -> Testin API Model from JSON on a package
// turns a pasted API call's request and response into a Lombok class and a
// record (UC-CODEGEN-022). A content module of its own, loaded only where the
// Java plugin is, as testin-java is, so the Plugin Verifier checks it only
// against IDEs that have com.intellij.java.
plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))

        // The one this module exists to depend on, and the one the core cannot.
        bundledPlugin("com.intellij.java")
    }

    // The core, which ships beside this module rather than inside it - so
    // compileOnly. The dependency runs one way only: this module calls the
    // core's Logger, Notifier and Services, and the core calls back through the
    // extension points it declares.
    compileOnly(project(":"))

    // The core ships Jackson in its own jar's libraries, and this module's
    // class loader has the core's as its parent, so it is compiled against here
    // and never packaged twice.
    compileOnly(libs.jackson.databind)

    // Same rule as the root build: a compile-time tool, never packaged.
    listOf(
        "compileOnly",
        "annotationProcessor",
        "testAnnotationProcessor"
    ).forEach { configuration ->
        add(configuration, libs.lombok)
    }

    testImplementation(libs.testng)
}

// The tests of this module's classes live here too, and they need everything
// the classes compile against - the core above all - on their classpath, where
// it runs as well. One declaration for both, so the two cannot drift apart.
configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

tasks.withType<Test> {
    useTestNG()
    jvmArgs("--sun-misc-unsafe-memory-access=allow")

    // A --tests filter is applied to every module, and a module with no matching
    // test fails rather than being skipped - so `./gradlew test --tests
    // "*DocumentClaimsTest*"` was a red build naming the filter rather than the
    // reason, which reads like the test does not exist (#66, finding 62). The
    // root module keeps the default: nearly every test lives there, so a filter
    // that matches nothing at all is still a misspelling worth failing on.
    filter {
        isFailOnNoMatchingTests = false
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}
