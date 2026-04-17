import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "testGit"
version = "1.3.1-SNAPSHOT"

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
        intellijIdea("2025.3.4")

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
        testFramework(TestFrameworkType.Platform)
    }

    // Source: https://mvnrepository.com/artifact/org.projectlombok/lombok
    implementation("org.projectlombok:lombok:1.18.44")
    compileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor("org.projectlombok:lombok:1.18.44")
    testCompileOnly("org.projectlombok:lombok:1.18.44")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.44")

    // Source: https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")

    // Source: https://mvnrepository.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jsr310
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.2")

    // Source: https://mvnrepository.com/artifact/io.rest-assured/json-path
    implementation("io.rest-assured:json-path:6.0.0")

    // Source: https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.21.0")

    // Source: https://mvnrepository.com/artifact/com.codoid.products/fillo
    implementation("com.codoid.products:fillo:1.24")

    // Source: https://mvnrepository.com/artifact/org.testng/testng
    testImplementation("org.testng:testng:7.12.0")

    // Source: https://mvnrepository.com/artifact/com.itextpdf/kernel
    implementation("com.itextpdf:kernel:9.6.0")

    // Source: https://mvnrepository.com/artifact/com.itextpdf/layout
    implementation("com.itextpdf:layout:9.6.0")

    // Source: https://mvnrepository.com/artifact/org.dhatim/fastexcel
    implementation("org.dhatim:fastexcel:0.20.0")
}

intellijPlatform {
    pluginConfiguration {
        id.set("testGit.demo")
        name.set("Test Git")
        version.set(project.version.toString())

        vendor {
            name.set("Muteb Almughyiri")
            email.set("mtb550@gmail.com")
            url.set("https://mtb.com")
        }

        ideaVersion {
            sinceBuild.set("253")
            untilBuild.set("253.*")
        }
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    sandboxContainer.set(layout.projectDirectory.dir(".sandbox"))
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<Test> {
        useTestNG()

        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

intellijPlatformTesting {
    runIde {
        parallelStream()
    }
}

configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(7, TimeUnit.DAYS)
        cacheChangingModulesFor(7, TimeUnit.DAYS)
    }
}