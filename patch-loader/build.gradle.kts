import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
   alias(libs.plugins.android.application)
}

val defaultManagerPackageName: String by rootProject.extra
val androidCompileSdkVersion: Int by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val androidJvmTarget: JvmTarget by rootProject.extra

android {
    namespace = "$defaultManagerPackageName.patch_loader"
    compileSdk = androidCompileSdkVersion

    defaultConfig {
        minSdk = androidMinSdkVersion
        multiDexEnabled = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility(androidSourceCompatibility)
        targetCompatibility(androidTargetCompatibility)
    }
}

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.replaceFirstChar { it.uppercase() }

    task<Copy>("copyDex${variantCapped}") {
        dependsOn("assemble${variantCapped}")
//        val outPath = "${rootProject.projectDir}/patch/src/main/resources/assets/yapatch"
        val outPath = "${rootProject.projectDir}/out/assets/${variant.name}/yapatch"
        val dexOutPath = "${buildDir}/intermediates/dex/${variant.name}/mergeDex${variantCapped}/classes.dex"

        mkdir(outPath)
        into(outPath)
        from(dexOutPath)
        rename("classes.dex", "loader.dex")
        println("loader dex has been copied to $outPath")
    }

    task<Copy>("copySo${variantCapped}") {
        dependsOn("assemble${variantCapped}")
//        val outPath = "${rootProject.projectDir}/manager/src/main/assets/yapatch"
        val outPath = "${rootProject.projectDir}/out/assets/${variant.name}/yapatch/so"
        val soOutPath = "${buildDir}/intermediates/merged_native_libs/${variant.name}/merge${variantCapped}NativeLibs/out/lib"
        val soIncluded = listOf("**/libpine.so")

        mkdir(outPath)
        into(outPath)
        from(
            fileTree(
                "dir" to soOutPath,
                "include" to soIncluded
            )
        )
        println("loader so has been copied to $outPath")
    }

    task("copy${variantCapped}Assets") {
        description = "Copy patch-loader's assets to out's $variantCapped floder"
        println(description)

        dependsOn("copySo${variantCapped}")
        dependsOn("copyDex${variantCapped}")

        doLast {
            println("Dex and so files has been copied to every project's assets folder")
        }
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.hiddenapibypass)
    implementation(libs.pine.core)
    implementation(libs.pine.xposed) {
        exclude("com.android.internal.util", "XmlUtils")
    }
}