

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.secrets.gradle) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.sonarqube)
}

sonar {
    properties {
        property("sonar.projectKey", "soen-390-the-irs_backend")
        property("sonar.organization", "soen-390-the-irs")
        property("sonar.host.url", "https://sonarcloud.io")

        property("sonar.sources", "app/src/main/java")
        property("sonar.tests", "app/src/test/java")
        property("sonar.sourceEncoding", "UTF-8")

        property("sonar.java.binaries",
            "app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
        )

        property("sonar.coverage.jacoco.xmlReportPaths",
            "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        )

        property("sonar.kotlin.source.version", "2.0")
    }
}


