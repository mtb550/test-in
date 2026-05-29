pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
    id("com.gradle.develocity") version "4.4.2"
}

rootProject.name = "Testin"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}