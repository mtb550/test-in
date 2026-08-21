import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "org.testin"
version = "2.8.0-alpha"

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
    // YAML for testin.yml, the file that binds an automation repository to the
    // test project it exercises (#6). SnakeYAML comes with it and the platform
    // ships its own copy, so the verifier run is what proves they do not clash.
    implementation(libs.jackson.dataformat.yaml)
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
        // Against PyCharm, GoLand and WebStorm the verifier reports the PSI
        // and TestNG references as unresolved. Expected: those classes load
        // only behind OptionalPlugin availability guards (issue #41), which
        // its static analysis cannot see. A reference that genuinely escaped
        // into core code looks identical here, so the runPyCharm smoke test
        // is what catches that.
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
        // Two channels only: alpha while a release is being used, and default
        // for production. A release goes out to alpha and is promoted to
        // default from the Marketplace page rather than uploaded again - a
        // version string can only be published once, whatever channel it
        // goes to.
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
