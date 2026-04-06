plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
    
    // ✨ NEW: Crashlytics Plugin
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
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Firebase Enterprise Stack
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-database'
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-messaging'
    
    // ✨ NEW: Crashlytics Dependency
    implementation 'com.google.firebase:firebase-crashlytics'
    
    // Visual Analytics & PDF Engine
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    implementation 'com.itextpdf:itext7-core:7.1.15'
}
