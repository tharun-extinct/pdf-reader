plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
}

android {
    namespace = "com.pdfreader.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pdfreader.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    
    splits {
        abi {
            isEnable = false
        }
    }

    packaging {
        jniLibs {
            // Preserve extracted native libraries for PDFium without using the
            // deprecated AndroidManifest application attribute.
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Material Components (provides the Theme.Material3.* XML window themes
    // referenced by AndroidManifest -> @style/Theme.NoxReader). Required even for a
    // pure-Compose app because ComponentActivity needs a valid XML window theme.
    implementation("com.google.android.material:material:1.11.0")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // PDFium integration for fast rendering
    implementation("com.github.barteksc:pdfium-android:1.9.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    
    // Coroutines for concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Navigation component for Jetpack Compose (required for multi-screen architecture)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Typed, transactional local metadata with a versioned Protocol Buffer schema.
    implementation("androidx.datastore:datastore:1.1.7")
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")

    // Required by Macrobenchmark to reset compilation state and capture profiles.
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                maybeCreate("java").apply {
                    option("lite")
                }
            }
        }
    }
}
