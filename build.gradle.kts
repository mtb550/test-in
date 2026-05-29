import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "org.testin"
version = "2.3.1-SNAPSHOT"

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
        testFramework(TestFrameworkType.Platform)
    }

    // Source: https://mvnrepository.com/artifact/org.projectlombok/lombok
    val lombokVersion = "1.18.44"
    implementation("org.projectlombok:lombok:$lombokVersion")
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Source: https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    // Source: https://mvnrepository.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-jsr310
    val jacksonVersion = "2.21.2"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Source: https://mvnrepository.com/artifact/io.rest-assured/json-path
    implementation("io.rest-assured:json-path:6.0.0")

    // Source: https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.21.0")

    // Source: https://mvnrepository.com/artifact/org.testng/testng
    testImplementation("org.testng:testng:7.12.0")

    // Source: https://mvnrepository.com/artifact/com.itextpdf/kernel
    // Source: https://mvnrepository.com/artifact/com.itextpdf/layout
    val iTextVersion = "9.6.0"
    implementation("com.itextpdf:kernel:$iTextVersion")
    implementation("com.itextpdf:layout:$iTextVersion")

    // Source: https://mvnrepository.com/artifact/org.dhatim/fastexcel
    implementation("org.dhatim:fastexcel:0.20.0")

    // Source: https://mvnrepository.com/artifact/org.apache.poi/poi
    // Source: https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml
    val poiVersion = "5.5.1"
    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")
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
            sinceBuild.set("251")
            untilBuild.set(null as String?)
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("JETBRAINS_TOKEN"))
        channels.set(listOf("alpha"))
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