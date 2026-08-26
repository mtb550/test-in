// Starting a TestNG run (#144).
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
        languageVersion.set(JavaLanguageVersion.of(21))
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

    listOf("compileOnly", "annotationProcessor").forEach { configuration ->
        add(configuration, libs.lombok)
    }
}
