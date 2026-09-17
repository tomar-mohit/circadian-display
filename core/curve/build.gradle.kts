import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// Target Java 17 bytecode so this pure-Kotlin module stays consumable by the
// Android modules (app/system:*), which all compile with `jvmTarget = 17` and
// with a JDK 17 toolchain in CI. Previously this module pinned
// `jvmToolchain(21)`, which emitted Java 21 (class file v65) bytecode that a
// JDK 17 `javac` cannot read ("class file has wrong version 65.0, should be
// 61.0"). We deliberately do NOT pin a toolchain here: only the JDK running the
// build is available in some environments, so we let it compile while forcing
// the bytecode version to 17 via `jvmTarget`.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Coroutines (for Flow support in DisplayController if needed)
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
