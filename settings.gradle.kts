pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // REQUIRED FOR MPAndroidChart
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "Sanatani Bandhan"
include(":app")
