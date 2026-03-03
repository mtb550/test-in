plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "testGit"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        snapshots()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("253.28294.334") {
            useInstaller = false
        }
        bundledPlugins(listOf("com.intellij.java", "TestNG-J", "Git4Idea"))
        jetbrainsRuntime()
        pluginVerifier()
        zipSigner()
    }

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("com.jayway.jsonpath:json-path:3.0.0")
    implementation("commons-io:commons-io:2.21.0")
    testImplementation("org.testng:testng:7.12.0")
}

intellijPlatform {
    pluginConfiguration {
        id.set("testGit.demo")
        name.set("Test Case Manager")
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
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

}

//tasks.withType<JavaCompile> {
//    options.compilerArgs.add("-Xlint:deprecation")
//}