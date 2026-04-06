buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0") 
        classpath("com.google.gms:google-services:4.4.0") 
        
        // ✨ FIREBASE CRASHLYTICS (Kotlin Syntax)
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9") 
    }
}

plugins {
    id("com.android.application") version "8.1.0" apply false
}
