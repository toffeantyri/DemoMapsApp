import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

allprojects {
    extra.apply {
        set("mapkitApiKey", getMapKitApiKey())
    }
}

fun getMapKitApiKey(): String {
    val properties = Properties().apply {
        load(file("map.properties").inputStream())
    }

    return properties.getProperty("MAPKIT_API_KEY")


}