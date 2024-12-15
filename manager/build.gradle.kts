import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
}

val defaultManagerPackageName: String by rootProject.extra
val androidCompileSdkVersion: Int by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidVersionCode: Int by rootProject.extra
val androidVersionName: String by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val androidJvmTarget: JvmTarget by rootProject.extra

android {
    namespace = defaultManagerPackageName
    compileSdk = androidCompileSdkVersion

    defaultConfig {
        applicationId = defaultManagerPackageName
        minSdk = androidMinSdkVersion
    }

    dataBinding {
        enable = true
    }
    viewBinding {
        enable = true
    }

    androidResources {
        generateLocaleConfig = true
        noCompress.add(".so")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
//            proguardFiles("proguard-rules.pro")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
            )
        }
        all {
            sourceSets[name].assets.srcDirs(rootProject.projectDir.resolve("out/assets/${name}"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility(androidSourceCompatibility)
        targetCompatibility(androidTargetCompatibility)
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.get()
    }
    kotlinOptions {
        jvmTarget = androidJvmTarget.target
    }

    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/${name}/kotlin")
            }
        }
    }
}

afterEvaluate {
    android.applicationVariants.forEach { variant ->
        val variantCapped = variant.name.replaceFirstChar { it.uppercase() }
        val variantLowered = variant.name.lowercase()

        task<Copy>("copy${variantCapped}Assets") {
//            dependsOn(":jar:copy${variantCapped}Assets")
            dependsOn(":patch-loader:copy${variantCapped}Assets")

            description = "Copy out's assets to manager's $variantCapped floder"
            println(description)

            tasks["merge${variantCapped}Assets"].dependsOn(this)

            from("${rootProject.projectDir}/out/assets/${variant.name}")
            into("${rootProject.layout.buildDirectory.get()}/intermediates/assets/${variantLowered}/merge${variantCapped}Assets")
        }

        task<Copy>("build${variantCapped}") {
            dependsOn(tasks["assemble${variantCapped}"])

            from(variant.outputs.map { it.outputFile })
            into("${rootProject.projectDir}/out/android/${variantLowered}")
            rename(".*.apk", "manager-${androidVersionName}-${variantLowered}.apk")
        }
    }
}

dependencies {
    annotationProcessor(libs.auto.value)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.auto.value.annotations)
    implementation(libs.gson)
    implementation(libs.matsudamper.viewbindingutil)
    implementation(projects.patch)
}