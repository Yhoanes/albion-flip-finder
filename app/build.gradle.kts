plugins {

    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.android)

    id("kotlin-parcelize")

}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}



android {

    namespace = "com.shadr.miflipperapp"

    compileSdk = 36



    buildFeatures {

        viewBinding = true

    }



    defaultConfig {

        applicationId = "com.shadr.miflipperapp"

        minSdk = 24

        targetSdk = 36

        versionCode = 3

        versionName = "1.0"



        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }



    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(

                getDefaultProguardFile("proguard-android-optimize.txt"),

                "proguard-rules.pro"

            )

        }

        debug {

            isMinifyEnabled = false

        }

    }

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_11

        targetCompatibility = JavaVersion.VERSION_11

    }

    kotlinOptions {

        jvmTarget = "11"

    }

    packagingOptions {

        resources.excludes.add("META-INF/AL2.0")

        resources.excludes.add("META-INF/LGPL2.1")

    }

}



dependencies {



// Core Android libraries

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.appcompat)

    implementation(libs.material)

    implementation(libs.androidx.activity)

    implementation(libs.androidx.constraintlayout)



// Testing libraries

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(libs.androidx.espresso.core)



// NATS client

    implementation("io.nats:jnats:2.17.0")



// Coroutines

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")



// Moshi for JSON parsing

    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")



// ViewModel and LiveData

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")



// Firebase (using BoM - Bill of Materials)

    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))



// DEPENDENCIAS DE AUTENTICACIÓN (CORRECCIÓN)

    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("com.google.firebase:firebase-firestore-ktx")



// DEPENDENCIAS DE GOOGLE SIGN-IN (LA CLAVE DE LA CORRECCIÓN)

    implementation("com.google.android.gms:play-services-auth:20.7.0")



// Navigation Component

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")

    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

}