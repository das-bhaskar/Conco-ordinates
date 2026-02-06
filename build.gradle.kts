// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.secrets.gradle) apply false
    id("org.sonarqube") version "7.1.0.6387" apply false
}

project(":app") {
    apply(plugin = "org.sonarqube")

    extensions.configure<org.sonarqube.gradle.SonarExtension> {
        properties {
            val projectKey = System.getenv("SONAR_PROJECT_KEY") ?: "soen-390-the-irs_backend"
            val org = System.getenv("SONAR_ORGANIZATION") ?: "soen-390-the-irs"
            property("sonar.projectKey", projectKey)
            property("sonar.organization", org)
            property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "https://sonarcloud.io")
            property("sonar.gradle.skipCompile", "true")

            property("sonar.sources", "app/src/main/java,app/src/main/kotlin")
            property("sonar.tests", "app/src/test/java,app/src/test/kotlin,app/src/androidTest/java,app/src/androidTest/kotlin")
            property("sonar.androidLint.reportPaths", "app/build/reports/lint-results-debug.xml")
            property("sonar.junit.reportPaths", "app/build/test-results/testDebugUnitTest")
            property("sonar.coverage.jacoco.xmlReportPaths", "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")

            val prKeyEnv = System.getenv("SONAR_PULLREQUEST_KEY")
            val prBranchEnv = System.getenv("SONAR_PULLREQUEST_BRANCH")
            val prBaseEnv = System.getenv("SONAR_PULLREQUEST_BASE")
            val ghRef = System.getenv("GITHUB_REF")
            val ghHead = System.getenv("GITHUB_HEAD_REF")
            val ghBase = System.getenv("GITHUB_BASE_REF")
            val prKey = prKeyEnv
                ?: ghRef?.let { Regex("refs/pull/(\\d+)/.*").find(it)?.groupValues?.get(1) }
            val prBranch = prBranchEnv ?: ghHead
            val prBase = prBaseEnv ?: ghBase

            if (!prKey.isNullOrBlank() && !prBranch.isNullOrBlank() && !prBase.isNullOrBlank()) {
                property("sonar.pullrequest.key", prKey)
                property("sonar.pullrequest.branch", prBranch)
                property("sonar.pullrequest.base", prBase)
            }
        }
    }
}
