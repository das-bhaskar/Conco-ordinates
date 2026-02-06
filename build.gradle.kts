// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.secrets.gradle) apply false
    id("org.sonarqube") version "7.1.0.6387"
}

sonar {
  properties {
    property("sonar.projectKey", "soen-390-the-irs_backend")
    property("sonar.organization", "soen-390-the-irs")
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.sources", "app/src/main")
    property("sonar.tests", "app/src/test")
    property("sonar.java.binaries", "app/build/intermediates/javac")
    property("sonar.android.lint.report", "app/build/reports/lint-results.xml")
  }
}
