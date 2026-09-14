plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.dena"
    compileSdk = 35
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.dena"
        minSdk = 24 // floor: LocaleHelper (LocaleList/setLocales) — do not raise (see AGENTS.md)
        targetSdk = 35
        versionCode = 8
        versionName = "0.6.1-alpha"
        resourceConfigurations += listOf("en", "bn") // prunes unused locales from deps — part of 1.3 MB win
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        create("release") {
            // Centralized keystore: ~/.keystores/central-release.keystore (shared across all your apps)
            // Each app uses its own alias (dena, talom, …) with same store password. Falls back to local file.
            val centralPath = (project.findProperty("CENTRAL_RELEASE_STORE_FILE") as String?)
                ?: System.getenv("CENTRAL_RELEASE_STORE_FILE")
                ?: "${System.getProperty("user.home")}/.keystores/central-release.keystore"
            val centralFile = file(centralPath)
            val localFile = file("dena-release.keystore")
            val storeFilePath = when {
                centralFile.exists() -> centralFile
                else -> localFile
            }
            if (storeFilePath.exists()) {
                storeFile = storeFilePath
                storePassword = (project.findProperty("DENA_RELEASE_STORE_PASSWORD") as String?)
                    ?: System.getenv("DENA_RELEASE_STORE_PASSWORD")
                keyAlias = (project.findProperty("DENA_RELEASE_KEY_ALIAS") as String?)
                    ?: System.getenv("DENA_RELEASE_KEY_ALIAS") ?: "dena"
                keyPassword = (project.findProperty("DENA_RELEASE_KEY_PASSWORD") as String?)
                    ?: System.getenv("DENA_RELEASE_KEY_PASSWORD")
                        ?: storePassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true // R8 full — required for 1.3 MB (see AGENTS.md). Don't disable.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro" // keeps: Room entities/DAOs + BuildConfig (see file)
            )
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null && releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }
        }
    }

    packaging {
        // Explicit compression — prevents silent size regression if minSdk >= 28 (AGP would store dex uncompressed)
        dex { useLegacyPackaging = true }
        jniLibs { useLegacyPackaging = true }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling-preview") // must stay debugImplementation (AGENTS.md)
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
