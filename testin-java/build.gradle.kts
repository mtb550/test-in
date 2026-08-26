// The half of Testin that needs the IntelliJ Java plugin: generating the
// automation class and its methods, navigating to them, and the gutter icon
// beside a generated @Test (#144).
//
// It is a content module rather than part of the main jar so the Plugin
// Verifier checks it only against IDEs that have com.intellij.java. In the core
// jar these classes produced 53 unresolved-class findings apiece against
// PyCharm, GoLand and WebStorm - which the JetBrains Marketplace publishes on
// the plugin page - even though OptionalPlugin.JAVA already stops any of them
// being reached there.
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
        languageVersion.set(JavaLanguageVersion.of(21))
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

    // Same rule as the root build: a compile-time tool, never packaged.
    listOf("compileOnly", "annotationProcessor", "testCompileOnly", "testAnnotationProcessor").forEach { configuration ->
        add(configuration, libs.lombok)
    }

    // The tests of the classes that live here live here too, and they need the
    // core on the classpath for the same reason the classes do.
    testImplementation(project(":"))
    testImplementation(libs.testng)
}

tasks.withType<Test> {
    useTestNG()
    jvmArgs("--sun-misc-unsafe-memory-access=allow")

    testLogging {
        events("passed", "skipped", "failed")
    }
}
