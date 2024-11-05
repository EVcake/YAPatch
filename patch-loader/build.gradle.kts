plugins {
   alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.duzhaokun123.patch_loader"
    compileSdk = 35

    defaultConfig {
        minSdk = 33
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("top.canyie.pine:core:0.3.0")
    implementation("top.canyie.pine:xposed:0.2.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}

task("copyDex") {
    dependsOn("assembleRelease")

    doLast {
        val dexOutPath = "$buildDir/intermediates/dex/release/mergeDexRelease/classes.dex"
        val outPath = "${rootProject.projectDir}/patch/src/main/resources/assets/yapatch"
        copy {
            from(dexOutPath)
            rename("classes.dex", "loader.dex")
            into(outPath)
        }
        println("Patch dex has been copied to $outPath")
    }
}

task("copySo") {
    dependsOn("assembleRelease")

    doLast {
        val soOutPath = "$buildDir/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib"
        val outPath = "${rootProject.projectDir}/manager/src/main/assets/yapatch"
        copy {
            from(soOutPath)
            into(outPath)
        }
    }
}

task("copyFiles") {
    dependsOn("copyDex")
    dependsOn("copySo")
}