import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("jacoco")
    alias(libs.plugins.secrets.gradle)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}



android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    // Required for Maps on certain Android versions
    useLibrary("org.apache.http.legacy")

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 1. Load local.properties
        val properties = Properties()
        val propertiesFile = project.rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }

        // 2. Check Environment (CI) first, then local.properties (You), then DUMMY
        val myKey: String = System.getenv("MAPS_API_KEY")
            ?: properties.getProperty("MAPS_API_KEY")
            ?: "DUMMY_KEY"

        manifestPlaceholders["mapsApiKey"] = myKey

        buildConfigField("String", "MAPS_API_KEY", "\"$myKey\"")
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
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    // Maps dependency
    implementation("org.apache.httpcomponents:httpclient-android:4.3.5.1")
    implementation(libs.play.services.maps)


    testImplementation(libs.junit)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}

val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    // 1. Ensure the tests run first. If a test fails, the build stops here.
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*", "**/androidx/**/*.*",
        "**/*_MembersInjector.class", "**/Dagger*Component.class",
        "**/*_Factory.class", "**/Hilt_*.class", "**/*\$Composable*.*",
        "**/MapsActivity*.*", "**/ui/components/**", "**/ui/theme/**",
        "**/*ComposableSingletons*.*",
        "**/*\$lambda*.*",
        "**/*Kt$*.*"
    )

    sourceDirectories.setFrom(files(
        "${project.projectDir}/src/main/java",
        "${project.projectDir}/src/main/kotlin"
    ))

    // 2. Use the Provider API (dir) instead of .get() to prevent configuration crashes
    val buildDir = project.layout.buildDirectory

    
    val kotlinClassDirs = listOf(
        "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes",
        "tmp/kotlin-classes/debug",
        "kotlin/compileDebugKotlin/classes"
    )

    val kotlinTrees = kotlinClassDirs.map { dir ->
        fileTree(buildDir.dir(dir)) {
            exclude(fileFilter)
        }
    }

    val javaTree = fileTree(buildDir.dir("intermediates/javac/debug/classes")) {
        exclude(fileFilter)
    }

    // 3. This combines them only at execution time
    classDirectories.setFrom(files(*kotlinTrees.toTypedArray(), javaTree))

    
    executionData.setFrom(fileTree(buildDir) {
        include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
    })
}
