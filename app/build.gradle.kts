plugins {
    id 'com.android.application'
    // Required for Firebase connecting to Google
    id 'com.google.gms.google-services'
    // NEW: Required for Crashlytics monitoring
    id 'com.google.firebase.crashlytics'
}

android {
    namespace 'org.shda'
    compileSdk 34

    defaultConfig {
        applicationId "org.shda"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Standard Android UI Libraries
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Firebase Bill of Materials (BoM) - Keeps all Firebase versions perfectly matched
    implementation platform('com.google.firebase:firebase-bom:32.7.0')

    // Existing Core Firebase Modules
    implementation 'com.google.firebase:firebase-database'
    implementation 'com.google.firebase:firebase-auth'

    // NEW: App Monitoring & Analytics (The CTO Dashboard)
    implementation 'com.google.firebase:firebase-analytics'
    implementation 'com.google.firebase:firebase-crashlytics'

    // PDF Generation Engine
    implementation 'com.itextpdf:itext7-core:7.2.5'
}
