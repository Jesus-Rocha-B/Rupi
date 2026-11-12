plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "pe.rupi.mobile"
 compileSdk = 36
 defaultConfig { applicationId = "pe.rupi.mobile"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "0.1.0" }
 buildFeatures { buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
 implementation("androidx.activity:activity-ktx:1.13.0")
 implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

 testImplementation("junit:junit:4.13.2")
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
