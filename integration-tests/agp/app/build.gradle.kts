plugins {
  id("com.android.application")
}

android {
  namespace = "com.google.guava.agptest"

  compileSdk = 35

  defaultConfig {
    applicationId = "com.google.guava.agptest"
    minSdk = 21
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
  }
}

dependencies {
  implementation("com.google.guava:guava:999.0.0-HEAD-android-SNAPSHOT")
}
