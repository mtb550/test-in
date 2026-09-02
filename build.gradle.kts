import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "org.testin"
version = "2.9.3-alpha"

/**
 * The newest IDE branch the plugin claims to support, verified alongside the
 * one it is built against.
 *
 * intellij.version in gradle.properties is what this compiles and runs against.
 * This is the other end of the range plugin.xml declares - sinceBuild 261 and no
 * untilBuild - and is the end the JetBrains Marketplace checks. Move it when a
 * new branch ships, or the sweep goes back to verifying only the past.
 */
val NEXT_BRANCH = "2026.2.1"

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

        // Everything that needs the IntelliJ Java plugin lives here rather than
        // in the core jar, so the verifier checks it only against IDEs that
        // have com.intellij.java (#144).
        //
        // Packaged, not compiled against: the core calls the extension points it
        // declares itself and never names a class in the module. Wrapping this
        // in implementation(..) would put the module on the core's compile
        // classpath and make the two projects depend on each other, since the
        // module compiles against the core.
        pluginModule(project(":testin-java"))
        pluginModule(project(":testin-testng"))
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
        // IntelliJ IDEA and PyCharm, because both verdicts are now news.
        //
        // PyCharm used to be behind the flag below with GoLand and WebStorm: the
        // verifier reported every PSI and TestNG reference as unresolved - 53
        // apiece, 159 in all - because those IDEs do not ship the Java and TestNG
        // plugins, and its static analysis cannot see that OptionalPlugin guards
        // the code at run time (#41). Moving the Java and TestNG code into
        // content modules the IDE loads only where they apply took that to zero,
        // which the Marketplace confirmed on 2.9.0-alpha, so PyCharm's answer is
        // a real answer again and belongs in the gate rather than behind a flag.
        //
        // GoLand and WebStorm stay behind it. Nothing targets them, PyCharm is
        // the IDE without Java support that Testin is actually built for
        // (#2, #148), and two more IDEs is two more downloads on every sweep.
        //
        // The full sweep is still one command away:
        //
        //     gradlew verifyPlugin -PverifyAllIdes
        //
        // It adds GoLand and WebStorm, which report what the verifier cannot
        // prove about an IDE nothing targets - the report is what the sweep is
        // for.
        ides {
            // Two branches, not one. sinceBuild is 261 with no untilBuild, so
            // the plugin claims every build from 261 onward - and that is what
            // the Marketplace verifies against. Pinning only the version this
            // compiles against hid a real defect: the Marketplace reported a
            // renderer scheduled for removal in 262 that our own sweep, running
            // against 261, could not see.
            listOf(IntelliJPlatformType.IntellijIdea, IntelliJPlatformType.PyCharm).forEach { ide ->
                create(ide, providers.gradleProperty("intellij.version"))
                create(ide, NEXT_BRANCH)
            }

            if (providers.gradleProperty("verifyAllIdes").isPresent) {
                listOf(IntelliJPlatformType.GoLand, IntelliJPlatformType.WebStorm).forEach { ide ->
                    create(ide, providers.gradleProperty("intellij.version"))
                    create(ide, NEXT_BRANCH)
                }
            }
        }

        // Ten of the twelve levels the verifier offers, so the gate fails on
        // anything new rather than on the two it was narrowed to. Both of the
        // levels that are off are named below, with what each would cost.
        //
        // It was down to COMPATIBILITY_PROBLEMS and INVALID_PLUGIN while one
        // internal-API call needed the exception - ExecutionManager
        // .getRunningDescriptors, how the stop found the process behind a run.
        // The stop now records the handler the execution topic hands it, so the
        // plugin makes no internal call and the level goes back on (#140).
        //
        // EXPERIMENTAL_API_USAGES is the one level deliberately left off, for
        // three usages that have no stable equivalent at all:
        //
        //   - EditorTabColorProvider.getEditorTabForegroundColor, overridden to
        //     color a Testin tab's title. The stable half of that interface
        //     colors the background, which is left to the user's File Colors.
        //   - WriteIntentReadAction.run, twice, taking the lock the action system
        //     itself takes before dispatching - a Swing click arrives without it.
        //
        // Each fails to compile if the platform drops it, which is the warning
        // that matters. Turning this level on would fail the build for three
        // decisions already made rather than for anything new.
        //
        // MISSING_DEPENDENCIES is the second, and it is off for the opposite
        // reason - not a decision to live with, but a report of the thing
        // working. Testin declares com.intellij.java, com.intellij.modules.java
        // and TestNG-J optional so the Java and TestNG code loads only in an IDE
        // that has them, which is what took PyCharm from 159 compatibility
        // problems to zero (#144). The verifier then lists all three as
        // Unavailable on every PyCharm target and raises this level while
        // reporting the plugin itself Compatible. Left on, the gate could never
        // be green for the IDE it had just been widened to cover: main was red
        // on every push from 2026-08-30 until this came off. A dependency that
        // is genuinely required and genuinely missing still fails the build,
        // through INVALID_PLUGIN and COMPATIBILITY_PROBLEMS, which are what a
        // plugin that will not load reports.
        failureLevel.set(
            listOf(
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_WARNINGS,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.DEPRECATED_API_USAGES,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
                org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.NOT_DYNAMIC,
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
