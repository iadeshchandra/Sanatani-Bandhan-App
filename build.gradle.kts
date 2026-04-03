plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    
    // Updated from 4.4.1 to 4.4.2 to fix the GitHub Actions crash
    id("com.google.gms.google-services") version "4.4.2" apply false
}
