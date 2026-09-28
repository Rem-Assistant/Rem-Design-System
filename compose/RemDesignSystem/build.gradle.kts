import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    from("rows")
    from("brand")
    from("icons")
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

// Fail before Android resource packaging if either static Material Symbols asset is missing,
// variable/GSUB-dependent, or lacks a registry codepoint. This is deliberately a preBuild gate,
// rather than only a unit test, so a consumer cannot assemble an AAR that renders tofu at runtime.
val verifyMaterialSymbolResources = tasks.register("verifyMaterialSymbolResources") {
    val fontDirectory = file("src/main/res/font")
    val outline = fontDirectory.resolve("material_symbols_outlined.ttf")
    val filled = fontDirectory.resolve("material_symbols_filled.ttf")
    inputs.files(outline, filled)

    doLast {
        fun u16(bytes: ByteArray, offset: Int): Int =
            ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
        fun i16(bytes: ByteArray, offset: Int): Int =
            ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.BIG_ENDIAN).short.toInt()
        fun u32(bytes: ByteArray, offset: Int): Int =
            (ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFF_FFFFL).toInt()

        fun tables(bytes: ByteArray): Map<String, Pair<Int, Int>> {
            check(bytes.size >= 12) { "invalid Material Symbols sfnt header" }
            return buildMap {
                repeat(u16(bytes, 4)) { index ->
                    val record = 12 + index * 16
                    check(record + 16 <= bytes.size) { "truncated Material Symbols table directory" }
                    val tag = String(bytes, record, 4, Charsets.US_ASCII)
                    val offset = u32(bytes, record + 8)
                    val length = u32(bytes, record + 12)
                    check(offset >= 0 && length >= 0 && offset + length <= bytes.size) {
                        "$tag table is outside the Material Symbols font"
                    }
                    put(tag, offset to length)
                }
            }
        }

        fun glyphIndex(bytes: ByteArray, fontTables: Map<String, Pair<Int, Int>>, codePoint: Int): Int {
            val cmap = requireNotNull(fontTables["cmap"]) { "Material Symbols font is missing cmap" }.first
            var format4 = -1
            repeat(u16(bytes, cmap + 2)) { index ->
                val record = cmap + 4 + index * 8
                val subtable = cmap + u32(bytes, record + 4)
                if (u16(bytes, subtable) == 4) format4 = subtable
            }
            check(format4 >= 0) { "Material Symbols font has no format-4 cmap" }
            val segmentCount = u16(bytes, format4 + 6) / 2
            val endCodes = format4 + 14
            val startCodes = endCodes + segmentCount * 2 + 2
            val deltas = startCodes + segmentCount * 2
            val rangeOffsets = deltas + segmentCount * 2
            repeat(segmentCount) { index ->
                val end = u16(bytes, endCodes + index * 2)
                val start = u16(bytes, startCodes + index * 2)
                if (codePoint !in start..end) return@repeat
                val delta = i16(bytes, deltas + index * 2)
                val rangeOffsetAddress = rangeOffsets + index * 2
                val rangeOffset = u16(bytes, rangeOffsetAddress)
                if (rangeOffset == 0) return (codePoint + delta) and 0xFFFF
                val glyph = u16(bytes, rangeOffsetAddress + rangeOffset + (codePoint - start) * 2)
                return if (glyph == 0) 0 else (glyph + delta) and 0xFFFF
            }
            return 0
        }

        val requiredCodepoints = listOf(0xE000, 0xE5CC, 0xE873, 0xE9E0, 0xF686)
        val verified = listOf("outline" to outline, "filled" to filled).associate { (label, resource) ->
            check(resource.isFile) { "missing packaged Material Symbols $label resource: $resource" }
            val bytes = resource.readBytes()
            val fontTables = tables(bytes)
            check("fvar" !in fontTables) { "$label Material Symbols resource must be static" }
            check("GSUB" !in fontTables) { "$label Material Symbols resource must not require substitution" }
            requiredCodepoints.forEach { codePoint ->
                check(glyphIndex(bytes, fontTables, codePoint) > 0) {
                    "$label Material Symbols resource maps U+${codePoint.toString(16).uppercase()} to .notdef"
                }
            }
            label to bytes
        }
        check(!verified.getValue("outline").contentEquals(verified.getValue("filled"))) {
            "outlined and filled Material Symbols resources must be distinct"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyMaterialSymbolResources)
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
