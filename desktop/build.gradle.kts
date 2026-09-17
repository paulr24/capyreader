plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization") version libs.versions.kotlin
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":capy"))
    implementation(libs.sqldelight.sqlite.driver)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    implementation(libs.okhttp.client)
    implementation(libs.org.json)

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "com.capyreader.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "CapyReader"
            packageVersion = "1.0.1241"
            description = "Capy Reader - RSS Reader for Desktop"
            copyright = "© 2026 Capy Reader"
            vendor = "Capy Reader"

            modules("java.sql", "java.naming", "jdk.unsupported")

            windows {
                menuGroup = "Capy Reader"
                upgradeUuid = "a4c28f11-96d5-4a2e-8d2b-6c4a6b29e011"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}
