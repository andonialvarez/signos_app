plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.andoni.signosdaily"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.andoni.signosdaily"
        minSdk = 26
        targetSdk = 36
        // CI passes the run number so every published build is a newer version.
        val build = (System.getenv("BUILD_NUMBER") ?: "1").toInt()
        versionCode = build
        versionName = "1.0.$build"
        // Sign videos and photos, converted by the "Vídeos de los signos" workflow.
        buildConfigField(
            "String",
            "MEDIA_BASE_URL",
            "\"https://github.com/andonialvarez/signos_app/releases/download/media/\"",
        )
    }

    // Google Play upload key, only present in CI when the repository secrets exist.
    val uploadKeystore = System.getenv("SIGNING_KEYSTORE_PATH")
    signingConfigs {
        // Shared debug key (standard public android/android credentials, like React Native's template),
        // so the GitHub builds always install over each other.
        getByName("debug") {
            storeFile = file("debug.keystore")
        }
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = file(uploadKeystore)
                storePassword = System.getenv("SIGNING_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("SIGNING_PASSWORD")
            }
        }
    }

    // Google's public AdMob test IDs; they show "Test Ad" banners and never pay out.
    val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
    val testBannerId = "ca-app-pub-3940256099942544/9214589741"
    fun prop(name: String) = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }

    flavorDimensions += "distribution"
    productFlavors {
        // Installed from the GitHub download link; updates itself from GitHub Releases.
        // Always shows test ads, so clicking them on your own phone is safe (and earns nothing).
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "SELF_UPDATE", "true")
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("boolean", "ADS_REQUIRE_CONSENT", "false")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            signingConfig = signingConfigs.getByName("debug")
        }
        // Published on Google Play: Play handles updates, so no self-update. Real ads stay off
        // (playAdsEnabled=false) while the videos are under DILSE's non-commercial licence.
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "SELF_UPDATE", "false")
            buildConfigField("boolean", "ADS_ENABLED", (prop("playAdsEnabled")?.toBoolean() ?: false).toString())
            buildConfigField("boolean", "ADS_REQUIRE_CONSENT", "true")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${prop("admobBannerId") ?: testBannerId}\"")
            manifestPlaceholders["admobAppId"] = prop("admobAppId") ?: testAdmobAppId
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
