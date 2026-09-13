plugins {
    id("com.android.application")
}

android {
    namespace = "de.autopolish.nativeapp"
    compileSdk = 35

    signingConfigs {
        getByName("debug") {
            System.getenv("AP_DEBUG_KEYSTORE")?.let { storeFile = file(it) }
        }
    }

    defaultConfig {
        applicationId = "de.autopolish.nativeapp"
        minSdk = 29
        targetSdk = 35
        versionCode = 1322
        versionName = "1.3.21"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(files("libs/zxing-core-3.5.3.jar"))
}
