plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
kotlin { jvmToolchain(17) }
android { compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 namespace = "com.hercules.hitpredictor"; compileSdk = 35
 defaultConfig { applicationId = "com.hercules.hitpredictor"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1" } }
