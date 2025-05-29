plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}


android {
    namespace = "com.example.musicplayer"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.example.musicplayer"
        minSdk = 23
        targetSdk = 34
        versionCode = 11
        versionName = "2.0.1"

        // For showing build version name
        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures{
        // For viewBinding
        viewBinding = true

        // For showing build version name
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Pull to Refresh
    implementation(libs.legacy.support)

    // Glide for image loading
    implementation(libs.glide)

    // For storing objects in shared preferences
    implementation(libs.gson)

    // Notification
    implementation(libs.androidx.media)

    // Vertical Seekbar
    implementation(libs.verticalseekbar)

    // Palette for extracting vibrant colors from images
    implementation("androidx.palette:palette:1.0.0")

    implementation("com.google.android.material:material:1.11.0")
    implementation ("com.arthenica:ffmpeg-kit-full-gpl:6.0.LTS")

    implementation(libs.firebase.auth)

    //cloud to server
    implementation("com.cloudinary:cloudinary-android:3.0.2")
    implementation(libs.firebase.database)
}

