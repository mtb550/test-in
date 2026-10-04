// Starting a TestNG execution (#144).
//
// Its own module rather than part of testin-java, because it needs both
// plugins: a TestNGConfiguration from TestNG-J and a PsiClass from
// com.intellij.java. Folded into testin-java it would have made that whole
// module require TestNG, so an IDE with Java and no TestNG would have lost code
// generation along with the ability to run.
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

        // Both, and that is the point of the separate module.
        bundledPlugin("com.intellij.java")
        bundledPlugin("TestNG-J")
    }

    // Ships beside this module, not inside it.
    compileOnly(project(":"))

    listOf("compileOnly", "annotationProcessor", "testCompileOnly", "testAnnotationProcessor").forEach { configuration ->
        add(configuration, libs.lombok)
    }

    // The tests of this module's classes live here, as testin-java's do.
    testImplementation(project(":"))
    testImplementation(libs.testng)
}

tasks.withType<Test> {
    useTestNG()
    jvmArgs("--sun-misc-unsafe-memory-access=allow")

    // A --tests filter reaches every module, and this one holds few tests.
    filter {
        isFailOnNoMatchingTests = false
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}
