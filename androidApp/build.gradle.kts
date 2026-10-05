import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import javax.inject.Inject

abstract class SubsetMaterialSymbolsFontTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    @get:InputFile
    abstract val symbolsKt: RegularFileProperty

    @get:InputFile
    abstract val sourceFont: RegularFileProperty

    @get:InputFile
    abstract val subsetScript: RegularFileProperty

    @get:OutputFile
    abstract val outputFont: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val python3Executable: Property<String>

    @TaskAction
    fun run() {
        val out = outputFont.get().asFile
        out.parentFile.mkdirs()
        execOperations.exec {
            commandLine(
                python3(),
                subsetScript.get().asFile.absolutePath,
                symbolsKt.get().asFile.absolutePath,
                sourceFont.get().asFile.absolutePath,
                out.absolutePath,
            )
        }
    }

    private fun python3(): String =
        python3Executable.orNull?.takeIf { it.isNotBlank() } ?: "python3"
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val releaseKeystoreFile = providers.environmentVariable("MOBILE_KEYSTORE_FILE").orNull
val releaseKeystorePassword = providers.environmentVariable("MOBILE_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("MOBILE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("MOBILE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

val subsetMaterialSymbolsFont =
    tasks.register<SubsetMaterialSymbolsFontTask>("subsetMaterialSymbolsFont") {
        symbolsKt = file("src/main/kotlin/com/exodidio/tune/ui/components/MaterialSymbols.kt")
        sourceFont = rootProject.file("tools/fonts/material_symbols_rounded.ttf")
        subsetScript = rootProject.file("tools/font-subset/subset_font.py")
        outputFont = file("src/main/res/font/material_symbols_rounded.ttf")

        // Honour an explicit path set in local.properties:  python3=/path/to/python3
        localProperties.getProperty("python3")?.let { python3Executable = it }
    }

val romanizationAssets = tasks.register<Sync>("romanizationAssets") {
    from(rootProject.file("tools/romanization/ATTRIBUTION.md"))
    from(rootProject.file("tools/romanization/LICENSE-OpenCC.txt"))
    into(layout.buildDirectory.dir("generated/romanizationAssets/romanization"))
}

tasks.named("preBuild") { dependsOn(subsetMaterialSymbolsFont, romanizationAssets) }

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
dependencies {
    implementation(project(":sharedLogic"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewmodelKtx)

    implementation(libs.compose.material3)
    implementation(libs.compose.lucideIcons)
    debugImplementation(libs.compose.uiToolingPreview)
    implementation(libs.haze.core)
    implementation(libs.haze.blur)
    implementation(libs.androidx.palette)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.reorderable)
    implementation(libs.vico.compose)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.compose.uiTestJunit4)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.compose.uiTestManifest)
}

android {
    sourceSets.getByName("main").assets.directories.add(layout.buildDirectory.dir("generated/romanizationAssets").get().asFile.path)
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/amllAssets"))
    namespace = "com.exodidio.tune"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = "30.0.15729638"


    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 7
        versionName = "1.2.0-beta"

        val lastFmApiKey = localProperties.getProperty("LASTFM_API_KEY")
            ?: providers.environmentVariable("LASTFM_API_KEY").getOrElse("")
        val lastFmApiSecret = localProperties.getProperty("LASTFM_API_SECRET")
            ?: providers.environmentVariable("LASTFM_API_SECRET").getOrElse("")
        buildConfigField("String", "LASTFM_API_KEY", "\"${lastFmApiKey.replace("\"", "\\\"")}\"")
        buildConfigField(
            "String",
            "LASTFM_API_SECRET",
            "\"${lastFmApiSecret.replace("\"", "\\\"")}\""
        )

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-fexceptions", "-frtti")
            }
        }
        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationId = "com.exodidio.tune.dev"
        }
        create("prod") {
            dimension = "environment"
            applicationId = "com.exodidio.tune"
        }
    }
    splits {
        abi {
            // ABI splits only for release builds: keep debug as a single fast APK.
            val buildingRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
            val releaseAbiSplitsRequested = gradle.startParameter.projectProperties["enableReleaseAbiSplits"] != "false"
            isEnable = buildingRelease && releaseAbiSplitsRequested
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Per-ABI versionCode + APK naming for ABI splits (AGP 9 androidComponents API).
// Scheme: base versionCode (7) x 1000 + offset — universal +0 (=7000),
// armeabi-v7a +1 (=6001), x86_64 +2 (=6002), arm64-v8a +3 (=6003, highest).
// arm64-v8a gets the highest code so capable devices prefer it.
// Keep the literals 7 / "1.2.0-beta" below in sync with defaultConfig versionCode/versionName.
// Uses the current AGP VariantOutput Property API via set(...) calls, plus
// output.filters with FilterType.ABI to detect the per-split ABI.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }?.identifier
            val offset = when (abi) {
                null -> 0
                "armeabi-v7a" -> 1
                "x86_64" -> 2
                "arm64-v8a" -> 3
                else -> error("unknown ABI \"$abi\"")
            }
            output.versionCode.set(7 * 1000 + offset)
            val suffix = if (abi != null) "_$abi" else ""
            output.outputFileName.set("Tune-v1.2.0-beta$suffix.apk")
        }
    }
}
