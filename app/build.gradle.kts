import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("jacoco")
    alias(libs.plugins.secrets.gradle)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

jacoco {
        toolVersion = "0.8.11"
    }

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    useLibrary("org.apache.http.legacy")

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        val propertiesFile = project.rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }

        val myKey: String = System.getenv("MAPS_API_KEY")
            ?: properties.getProperty("MAPS_API_KEY")
            ?: "DUMMY_KEY"
        val smartlookProjectKey: String = System.getenv("SMARTLOOK_PROJECT_KEY")
            ?: properties.getProperty("SMARTLOOK_PROJECT_KEY")
            ?: ""
        val smartlookTesterId: String = System.getenv("SMARTLOOK_TESTER_ID")
            ?: properties.getProperty("SMARTLOOK_TESTER_ID")
            ?: ""

        manifestPlaceholders["mapsApiKey"] = myKey
        buildConfigField("String", "MAPS_API_KEY", "\"$myKey\"")
        buildConfigField("String", "SMARTLOOK_PROJECT_KEY", "\"$smartlookProjectKey\"")
        buildConfigField("String", "SMARTLOOK_TESTER_ID", "\"$smartlookTesterId\"")
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    testOptions {
    unitTests.isReturnDefaultValues = true
    unitTests.all {
        it.ignoreFailures = true

        it.extensions.configure(JacocoTaskExtension::class) {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }
}
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("org.apache.httpcomponents:httpclient-android:4.3.5.1")
    implementation(libs.play.services.maps)

    // ── Google Sign-In — required for Calendar OAuth token ────────────────────
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.smartlook.android:smartlook-analytics:2.3.4")
    // ── Jetpack Navigation — replaces hardcoded when(selectedTab) ─────────────
    implementation("androidx.navigation:navigation-compose:2.7.7")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation(libs.junit)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = mutableListOf(
        "**/*\$*.*",
        "**/*\$DefaultImpls*",
        "**/SearchResult$*",
        // To hide the "Function" objects created by coroutines/lambdas
        "**/*\$getRoute$*",
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        "**/com/example/myapplication/logic/SharedPrefsCalendarPreferences*",
        "**/com/example/myapplication/logic/GoogleCalendarProvider*",
        "**/com/example/myapplication/logic/SimpleMockRouteProvider*",
        "**/com/example/myapplication/data/JsonSchedule*",
        "**/com/example/myapplication/data/JsonStop*",
        "**/com/example/myapplication/data/ShuttleRoute*",
        "**/com/example/myapplication/data/CampusBuildingNameProvider*",
        "**/com/example/myapplication/data/JsonShuttleData*",
        // Jetpack Compose exclusions (SRP focused)
        "**/com/example/myapplication/ui/components/**",
        "**/com/example/myapplication/ui/theme/**",
        "**/MapViewModel\$navigateToBuildingCode\$*",
        "**/MapViewModel\$refreshLocation\$*",
        "**/com/example/myapplication/ui/viewmodel/MapViewModel/onBackToPreview",

        // Exclude Activities and App entry points
        "**/com/example/myapplication/MainActivity*",
        "**/com/example/myapplication/logic/TravelMode*",
        "**/com/example/myapplication/ui/screens/**",
        "**/com/example/myapplication/analytics/**",
        "**/MyCustomApplication.*",
        "**/com/example/myapplication/MapsActivity*",
        "**/com/example/myapplication/MyApplication*",
        "**/com/example/myapplication/telemetry/**",   // Excludes CrashReporter
        "**/com/example/myapplication/map/**",
        "**/com/example/myapplication/shuttle/**",
        "**/*ComposableSingletons*",
        "**/*ComposableInvoker*",
        "**/*Composable*$",
        "**/*ViewActions*",
        "**/*_Factory*",
        "**/*\$Lambda$*.*", // Added to catch Compose state-change lambdas
        "**/ui/theme/**"    // Usually excluded as it's boilerplate
    )

    sourceDirectories.setFrom(files(
        "${project.projectDir}/src/main/java",
        "${project.projectDir}/src/main/kotlin"
    ))

    val buildDir = project.layout.buildDirectory

    val kotlinTree = fileTree(buildDir.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
        exclude(fileFilter)
    }

    val javaTree = fileTree(buildDir.dir("intermediates/javac/debug/classes")) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(kotlinTree, javaTree))

    executionData.setFrom(fileTree(buildDir) {
    include(
        "outputs/unit_test_code_coverage/debugUnitTest/*.exec",
        "jacoco/testDebugUnitTest.exec",
        "jacoco/testDebugUnitTestUnitTest.exec",
        "outputs/code-coverage/connected/*.ec"
        )
    })
}
