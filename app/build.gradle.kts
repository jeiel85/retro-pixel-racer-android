import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class ExportReleaseToDesktopTask : DefaultTask() {
  @get:Input
  abstract val versionName: Property<String>

  @get:Input
  abstract val versionCode: Property<Int>

  @get:InputFile
  abstract val aabFile: RegularFileProperty

  @get:InputFile
  abstract val releaseNotesFile: RegularFileProperty

  @TaskAction
  fun export() {
    val home = File(System.getProperty("user.home"))
    val candidates = listOf(
      File(home, "OneDrive/바탕 화면"),
      File(home, "OneDrive/Desktop"),
      File(home, "Desktop")
    )
    val desktop = candidates.firstOrNull { it.isDirectory }
      ?: throw GradleException(
        "Could not find a Desktop directory. Tried:\n" +
          candidates.joinToString("\n") { "  - ${it.absolutePath}" }
      )
    
    // Build Export Path Rule: Always save in C:\Users\jeiel\Desktop\Build
    val buildFolder = File(desktop, "Build")
    if (!buildFolder.exists()) {
      buildFolder.mkdirs()
    }

    val aab = aabFile.get().asFile
    if (!aab.isFile) {
      throw GradleException(
        "Release AAB not found at ${aab.absolutePath}. " +
          "bundleRelease should have produced it; check the build log."
      )
    }

    val releaseNotes = releaseNotesFile.get().asFile
    if (!releaseNotes.isFile) {
      throw GradleException(
        "Missing release notes at ${releaseNotes.absolutePath}. " +
          "Create the Play Console TXT before exporting."
      )
    }

    val releaseNotesText = releaseNotes.readText().trim()
    if (!releaseNotesText.contains("<ko-KR>") || !releaseNotesText.contains("<en-US>")) {
      throw GradleException(
        "Release notes must contain <ko-KR> and <en-US> blocks: ${releaseNotes.absolutePath}"
      )
    }

    val baseName = "RetroPixelRacer-v${versionName.get()}-vc${versionCode.get()}"
    val aabTarget = File(buildFolder, "$baseName.aab")
    val txtTarget = File(buildFolder, "$baseName-release-notes.txt")

    aab.copyTo(aabTarget, overwrite = true)
    txtTarget.writeText(releaseNotesText + System.lineSeparator())

    logger.lifecycle("Wrote ${aabTarget.absolutePath} (${aabTarget.length()} bytes)")
    logger.lifecycle("Wrote ${txtTarget.absolutePath} (${txtTarget.length()} bytes)")
  }
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.jeiel85.retropixelracer"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.jeiel85.retropixelracer"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD") ?: "android"
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

val exportVersionName = android.defaultConfig.versionName
  ?: throw GradleException("versionName is not set in defaultConfig")
val exportVersionCode = android.defaultConfig.versionCode
  ?: throw GradleException("versionCode is not set in defaultConfig")
val exportReleaseAab = layout.buildDirectory.file("outputs/bundle/release/app-release.aab")
val exportReleaseNotes = rootProject.layout.projectDirectory.file("store-graphics/play-console-current/release-notes.txt")

tasks.register<ExportReleaseToDesktopTask>("exportReleaseToDesktop") {
  group = "retropixelracer"
  description = "Copies the release AAB and Play Console release notes to the user's Desktop Build folder"

  dependsOn("bundleRelease")
  versionName.set(exportVersionName)
  versionCode.set(exportVersionCode)
  aabFile.set(exportReleaseAab)
  releaseNotesFile.set(exportReleaseNotes)
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
