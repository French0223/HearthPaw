plugins {
    alias(libs.plugins.android.application)
}

// Load local.properties to get the Maps API Key
val localPropertiesFile = file("${rootDir}/local.properties")
val mapsApiKey: String = if (localPropertiesFile.exists()) {
    val content = localPropertiesFile.readText()
    val match = Regex("""mapApiKey\s*=\s*(.+)""").find(content)
    match?.groupValues?.get(1)?.trim() ?: ""
} else {
    ""
}

val geminiApiKey: String = if (localPropertiesFile.exists()) {
    val content = localPropertiesFile.readText()
    val match = Regex("""geminiApiKey\s*=\s*(.+)""").find(content)
    match?.groupValues?.get(1)?.trim() ?: ""
} else {
    ""
}

android {
    namespace = "com.example.hearthpaw"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    defaultConfig {
        applicationId = "com.example.hearthpaw"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add manifest placeholders for the API key
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    
    // Room components
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Lifecycle components (MVVM)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Google Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Google AI SDK
    implementation(libs.google.genai)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}