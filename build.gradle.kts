// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Android Gradle Plugin
        classpath "com.android.tools.build:gradle:8.1.0"
        
        // Google Services plugin (Required for Firebase)
        classpath "com.google.gms:google-services:4.4.1"
        
        // NEW: Crashlytics Plugin for App Monitoring
        classpath "com.google.firebase:firebase-crashlytics-gradle:2.9.9"
    }
}

plugins {
    id 'com.android.application' version '8.1.0' apply false
    id 'com.android.library' version '8.1.0' apply false
}
