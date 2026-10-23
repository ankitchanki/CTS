import com.android.build.api.variant.impl.VariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.kotlin.compose)
}

apksign {
    storeFileProperty = "androidStoreFile"
    storePasswordProperty = "androidStorePassword"
    keyAliasProperty = "androidKeyAlias"
    keyPasswordProperty = "androidKeyPassword"
}

val repo = jgit.repo()
val commitCount = (repo?.commitCount("HEAD") ?: 1)
val latestTag = (findProperty("releaseVersion") as String?) ?: repo?.latestTag?.removePrefix("v") ?: "1.0"

android {
    namespace = "io.github.ankitchanki.circlet"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.ankitchanki.circlet"
        minSdk = 28
        targetSdk = 36
        versionCode = commitCount
        versionName = latestTag
        buildConfigField("String", "APP_NAME", "\"Circlet\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Without the apksign keystore properties (see CI secrets), fall back to the debug key so
            // the APK is still installable. Builds signed this way can't update each other across machines.
            if (!project.hasProperty("androidStoreFile")) {
                signingConfig = signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            require(output is VariantOutputImpl)

            val vName = output.versionName.get()
            val vCode = output.versionCode.get()

            output.outputFileName.set("Circlet_${vName}_${vCode}_${variant.name}.apk")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.animation.core.android)
    implementation(libs.animation.android)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)
    testImplementation(libs.junit)
}
