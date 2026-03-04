

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.secrets.gradle) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    id("org.sonarqube") version "5.0.0.4638"
}

sonar {
    properties {
        property("sonar.projectKey", "soen-390-the-irs_backend")
        property("sonar.organization", "soen-390-the-irs")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths",
            "${project.projectDir}/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}

