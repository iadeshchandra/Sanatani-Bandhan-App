plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "org.shda"
    compileSdk = 34
    defaultConfig {
        applicationId = "org.shda"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Firebase Core
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")

    // Startup Features
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") 
    implementation("com.itextpdf:itext7-core:7.1.15")
}
