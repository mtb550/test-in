pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
    id("com.gradle.develocity") version "4.5.0"
}

rootProject.name = "Testin"

// The content module that carries everything needing the IntelliJ Java plugin,
// so the Plugin Verifier stops reporting it against IDEs that have no Java
// support (#144).
include(":testin-java")
include(":testin-testng")

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}