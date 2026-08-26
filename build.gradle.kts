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

    // SFTP for the server sync (#94). The maintained JSch fork, not
    // com.jcraft:jsch, which last shipped in 2018 and cannot negotiate
    // rsa-sha2 against a current sshd. No transitive dependencies, and it
    // refuses an unknown host key rather than accepting it - which is what
    // ruled out MINA SSHD, whose ClientBuilder defaults to
    // AcceptAllServerKeyVerifier.INSTANCE.
    implementation(libs.jsch)

    // An SFTP server inside the test JVM, so the transport is tested against a
    // real one on every build rather than against a machine somebody set up.
    // testImplementation only - it never reaches the distribution, which is why
    // its size does not count and why its accept-any-host-key default, the
    // thing that ruled it out as the client, does not matter on the server side.
    testImplementation(libs.sshd.core)
    testImplementation(libs.sshd.sftp)
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
        // IntelliJ IDEA alone by default, because it is the only one of the four
        // whose verdict is news. Against PyCharm, GoLand and WebStorm the
        // verifier reports every PSI and TestNG reference as unresolved - 53
        // apiece, 159 in all - because those IDEs do not ship the Java and
        // TestNG plugins. plugin.xml depends on both optionally, with a config
        // file each, and the code behind them is guarded by OptionalPlugin
        // (#41); the verifier's static analysis cannot see a runtime guard, so
        // it reports what it cannot prove. A reference that genuinely escaped
        // into core code looks identical here, which is why the runPyCharm
        // smoke test is what catches that, and not this.
        //
        // The full sweep is still one command away:
        //
        //     gradlew verifyPlugin -PverifyAllIdes
        //
        // It reports those 159 and fails on them, which is the honest exit code
        // for a question whose answer is "the verifier cannot tell" - the
        // report is what the sweep is for.
        ides {
            create(IntelliJPlatformType.IntellijIdea, providers.gradleProperty("intellij.version"))

            if (providers.gradleProperty("verifyAllIdes").isPresent) {
                create(IntelliJPlatformType.PyCharm, "2026.1.3")
                create(IntelliJPlatformType.GoLand, "2026.1.3")
                create(IntelliJPlatformType.WebStorm, "2026.1.3")
            }
        }

        // The default also fails on a call to an @ApiStatus.Internal method, and
        // there is exactly one: ExecutionManager.getRunningDescriptors, which is
        // how the stop finds the process behind a run (#140). Left out here so
        // the gate reports the thing it exists for - a plugin that will not load
        // in the IDE it ships for - rather than a known call that works.
        failureLevel.set(
            listOf(
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
            )
        )
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

// A real SFTP server on localhost, for trying the server sync by hand (#94).
// Windows will not install OpenSSH Server without an administrator, so this is
// how there is something to sync against. Test scope only - it never ships.
tasks.register<JavaExec>("sftpServer") {
    group = "verification"
    description = "Runs a local SFTP server on 127.0.0.1:22 to sync a test project against"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("org.testin.sftp.SftpServerRunner")
    standardOutput = System.out
}

// Makes the local SFTP server start when this user logs in, with no
// administrator involved. A per-user startup entry needs no elevation, unlike a
// Windows service or the OpenSSH Server capability - which is exactly why this
// route is the one available on this machine.
tasks.register("installSftpServer") {
    group = "verification"
    description = "Starts the local SFTP server automatically when you log in"
    dependsOn(tasks.named("testClasses"))

    doLast {
        val home = File(System.getProperty("user.home"), ".testin-sftp")
        home.mkdirs()

        // The classpath goes in an argument file rather than on the command
        // line: it names every jar the tests use, and would run past the 8,191
        // characters a Windows command line allows. Forward slashes, because a
        // backslash is an escape character inside an argument file.
        val classpath = sourceSets["test"].runtimeClasspath.asPath.replace('\\', '/')
        val argsFile = File(home, "sftp-server.args")
        argsFile.writeText("-cp\n\"" + classpath + "\"\norg.testin.sftp.SftpServerRunner\n")

        // javaw rather than java, so nothing opens a console window at logon.
        val javaw = File(System.getProperty("java.home"), "bin/javaw.exe")
        val startup = File(System.getenv("APPDATA"), "Microsoft/Windows/Start Menu/Programs/Startup")
        val launcher = File(startup, "Testin SFTP Server.cmd")

        launcher.writeText(
            "@echo off\r\n" +
                "start \"\" /b \"" + javaw.absolutePath + "\" @\"" + argsFile.absolutePath + "\"\r\n"
        )

        println("")
        println("  The SFTP server will start when you log in.")
        println("  ---------------------------------------------------------------")
        println("  starts from : " + launcher.absolutePath)
        println("  serves      : " + File(home, "srv/Testin").absolutePath)
        println("  its log     : " + File(home, "server.log").absolutePath)
        println("  address     : sftp://127.0.0.1/Testin    (tester / testin)")
        println("  ---------------------------------------------------------------")
        println("  Start it now, without logging out:")
        println("    \"" + launcher.absolutePath + "\"")
        println("")
        println("  To stop it starting: gradlew uninstallSftpServer")
        println("")
    }
}

tasks.register("uninstallSftpServer") {
    group = "verification"
    description = "Stops the local SFTP server starting when you log in"

    doLast {
        val startup = File(System.getenv("APPDATA"), "Microsoft/Windows/Start Menu/Programs/Startup")
        val launcher = File(startup, "Testin SFTP Server.cmd")

        println(if (launcher.delete()) "  Removed " + launcher.absolutePath
                else "  Nothing to remove at " + launcher.absolutePath)
        println("  What it served is kept in " + File(System.getProperty("user.home"), ".testin-sftp"))
    }
}
