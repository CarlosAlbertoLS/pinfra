import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "wgg.sice_pinfra"
    compileSdk = 35

    defaultConfig {
        applicationId = "wgg.sice_pinfra"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "armeabi", "x86", "x86_64"))
        }
    }

    buildTypes {
        debug {
            val keysPropertiesFile = rootProject.file("keys.properties")
            if (keysPropertiesFile.exists()) {
                val keysProperties = Properties()
                keysProperties.load(FileInputStream(keysPropertiesFile))
                buildConfigField("String", "ACCESS", "\"${keysProperties.getProperty("ACCESS")}\"")
            }
        }



        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )


            packagingOptions {
                jniLibs {
                    useLegacyPackaging = true
                }
            }

            val keysPropertiesFile = rootProject.file("keys.properties")
            if (keysPropertiesFile.exists()) {
                val keysProperties = Properties()
                keysProperties.load(FileInputStream(keysPropertiesFile))
                buildConfigField("String", "ACCESS", "\"${keysProperties.getProperty("ACCESS")}\"")

            }
        }


        buildFeatures {
            viewBinding = true
            buildConfig = true
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    /* QA */
    implementation (files("libs/QA/sdkVierge_DEV_01.15.20-release.aar"))
    implementation (files("libs/QA/MITMobileLibrary-2.7.3-DEV-QA-release.aar"))
    implementation (files("libs/QA/MITReaderLibrary-2.7.3-DEV-QA-release.aar"))

    /* PROD */
    /*implementation (files("libs/PROD/sdkVierge_PROD_01.15.20-release.aar"))
    implementation (files("libs/PROD/MITMobileLibrary-2.7.3-release.aar"))
    implementation (files("libs/PROD/MITReaderLibrary-2.7.3-release.aar"))*/

    /* CONF */
    implementation (files("libs/CONF/firebasedatamodule-release.aar"))


    implementation ("androidx.room:room-runtime:2.5.2")

    implementation ("com.starmicronics:stario10:1.8.0")

    implementation ("androidx.core:core-ktx:1.9.0")
    implementation ("com.google.code.gson:gson:2.9.1")
    implementation ("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation ("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation ("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation ("androidx.preference:preference-ktx:1.2.0")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.guava:guava:31.1-jre")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("com.airbnb.android:lottie:5.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("io.github.binaryfoo:emv-bertlv:0.1.7")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.jaredrummler:colorpicker:1.1.0")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.airbnb.android:lottie:5.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("io.github.binaryfoo:emv-bertlv:0.1.7")
}
