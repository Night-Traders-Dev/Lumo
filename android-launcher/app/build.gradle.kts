plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val generatedKeyboardAssetsDir = layout.buildDirectory.dir("generated/assets/keyboard/main")

val generateImeDictionary by tasks.registering {
    val dictionaryOutput = generatedKeyboardAssetsDir.map { it.file("ime-words.txt") }

    outputs.file(dictionaryOutput)

    doLast {
        val sources = listOf(
            file("/usr/share/dict/american-english"),
            file("/usr/share/dict/words"),
            file("/usr/share/dict/british-english"),
        ).filter { it.exists() }

        val outputFile = dictionaryOutput.get().asFile
        outputFile.parentFile.mkdirs()

        if (sources.isEmpty()) {
            outputFile.writeText("")
            return@doLast
        }

        val seen = linkedSetOf<String>()
        outputFile.printWriter().use { writer ->
            sources.forEach { source ->
                source.forEachLine { rawLine ->
                    val normalized = rawLine
                        .trim()
                        .lowercase()
                        .filter { character ->
                            character.isLetter() || character == '\''
                        }

                    if (normalized.length in 2..20 &&
                        normalized.any(Char::isLetter) &&
                        seen.add(normalized)
                    ) {
                        writer.println(normalized)
                    }
                }
            }
        }
    }
}

android {
    namespace = "dev.nighttraders.lumo.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.nighttraders.lumo.launcher"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("localRelease") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "AndroidDebugKey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("localRelease")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets.getByName("main").assets.srcDir(generatedKeyboardAssetsDir)
}

kotlin {
    jvmToolchain(17)
}

tasks.named("preBuild") {
    dependsOn(generateImeDictionary)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.datastore.preferences)
}
