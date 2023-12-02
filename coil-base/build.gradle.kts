import coil3.addAllMultiplatformTargets
import coil3.jvmCommon
import coil3.nonAndroidMain
import coil3.nonJsMain
import coil3.nonJvmCommon
import coil3.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("dev.drewhamilton.poko")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "coil3.base")

kotlin {
    jvmCommon()
    nonAndroidMain()
    nonJsMain()
    nonJvmCommon()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.stdlib)
                api(libs.okio)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.common)
            }
        }
        named("nonJvmCommon") {
            dependencies {
                implementation(libs.kotlinx.immutable.collections)
            }
        }
        named("nonAndroidMain") {
            dependencies {
                implementation(libs.skiko)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.appcompat.resources)
                implementation(libs.androidx.core)
                implementation(libs.androidx.exifinterface)
                implementation(libs.androidx.profileinstaller)
                api(libs.androidx.lifecycle.runtime)
                api(libs.coroutines.android)
            }
        }
        named("androidUnitTest") {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.jvm)
            }
        }
        named("androidInstrumentedTest") {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.android)
            }
        }
    }
}
