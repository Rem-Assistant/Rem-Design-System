import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi")
}

// The reference layout keeps component sources flat (onboarding/, primitives/) plus the generated
// token file at ../../tokens/generated/RemTokens.kt. Gather them into one compilable tree, dropping
// the Code Connect bindings (*.figma.kt import a Figma package that is intentionally NOT a build
// dependency — exactly as the SwiftUI target excludes *.figma.swift in ../../Package.swift).
val gatherSources = tasks.register<Copy>("gatherDesignSystemSources") {
    into(layout.buildDirectory.dir("designSystemSrc"))
    from("onboarding")
    from("primitives")
    from(file("../../tokens/generated")) { include("RemTokens.kt") }
    exclude("**/*.figma.kt")
}

android {
    namespace = "com.rem.designsystem"
    compileSdk = 34
    defaultConfig { minSdk = 24 }

    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf(layout.buildDirectory.dir("designSystemSrc")))
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(gatherSources)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
}
