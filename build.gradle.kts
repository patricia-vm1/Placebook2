// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
        extra["kotlin_version"] = "1.9.0"
        extra["maps_services_version"] = "18.1.0"
        extra["location_services_version"] = "21.0.1"
        extra["lifecycle_version"] = "2.7.0"
        extra["room_ version"] = "2.6.0"
        extra["coroutines_verson"] = ""
}

plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}