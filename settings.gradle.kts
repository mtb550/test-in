pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.13.1"
    id("com.gradle.develocity") version "3.17.2"
}

rootProject.name = "testin"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}