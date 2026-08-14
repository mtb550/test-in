import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "org.testin"
version = "2.7.1"

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

        bundledPlugins(
            listOf(
                "com.intellij.java",
                "TestNG-J",
                "Git4Idea"
            )
        )

        jetbrainsRuntime()
        pluginVerifier()
        zipSigner()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // Lombok is a compile-time tool: annotations generate plain Java, nothing
    // references it at runtime - so it must never be packaged into the plugin
    // distribution ("implementation" would ship its 2MB jar to the Marketplace).
    listOf(
        "compileOnly", "annotationProcessor", "testCompileOnly", "testAnnotationProcessor"
    ).forEach { configName ->
        add(configName, libs.lombok)
    }

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.testng)
    implementation(libs.iText.kernel)
    implementation(libs.iText.layout)
    implementation(libs.fastexcel)
    implementation(libs.poi)
    implementation(libs.poi.ooxml)
}

intellijPlatform {
    pluginConfiguration {
        id.set("org.testin")
        name.set("Testin")
        version.set(project.version.toString())

        vendor {
            name.set("Muteb Almughyiri")
            email.set("mtb550@gmail.com")
            url.set("https://github.com/mtb550/test-in")
        }

        ideaVersion {
            sinceBuild.set("261")
            untilBuild.set(null as String?)
        }
    }

    pluginVerification {
        // Expected findings only: all PSI/TestNG references live in classes
        // that are class-loaded exclusively behind OptionalPlugin availability
        // guards (issue #41); the verifier's static analysis cannot see
        // runtime gating. Patterns match the problem's short description, so
        // they cannot be scoped to packages — a PSI reference sneaking into
        // core code must be caught by the runPyCharm smoke test instead.
        // Update the version in the file on each release.
        ignoredProblemsFile = file("verifier-ignored-problems.txt")

        ides {
            create(IntelliJPlatformType.IntellijIdea, providers.gradleProperty("intellij.version"))
            create(IntelliJPlatformType.PyCharm, "2026.1.3")
            create(IntelliJPlatformType.GoLand, "2026.1.3")
            create(IntelliJPlatformType.WebStorm, "2026.1.3")
        }
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("JETBRAINS_TOKEN"))
        // Alpha channel while validating multi-IDE support (issues #38/#41);
        // switch back to "default" for the stable release.
        channels.set(listOf("alpha"))
        //channels.set(listOf("default"))
    }

    sandboxContainer.set(layout.projectDirectory.dir(".sandbox"))
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<Test> {
        useTestNG()

        jvmArgs("--sun-misc-unsafe-memory-access=allow")

        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runPyCharm") {
            type = IntelliJPlatformType.PyCharm
            version = "2026.1.3"
            task {
                jvmArgs("--sun-misc-unsafe-memory-access=allow")
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(7, TimeUnit.DAYS)
        cacheChangingModulesFor(7, TimeUnit.DAYS)
    }
}

tasks.named<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runIde") {
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
}
