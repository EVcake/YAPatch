// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.cxx.configure.abiOf
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.ObjectId
import org.jetbrains.kotlin.gradle.dsl.JvmTarget



plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.org.eclipse.jgit)
    }
}

val git = Git(FileRepository(rootProject.file(".git")))
val refId: ObjectId = git.repository.resolve("refs/remotes/origin/main")
val commitCount = git.log().add(refId).call().count()
val commitId: String = git.log().add(refId).setMaxCount(1).call().iterator().next().name

val defaultManagerPackageName by extra("io.github.duzhaokun123.yapatch")
val androidCompileSdkVersion by extra(35)
val androidBuildToolsVersion by extra("35.0.0")
val androidMinSdkVersion by extra(28)
val androidTargetSdkVersion by extra(35)
val androidVersionCode by extra(commitCount)
val androidVersionName by extra("0.1.7-r${commitCount}.${commitId.substring(0, 7)}")

val androidCompileNdkVersion by extra("27.2.12479018")
val androidCmakeVersion by extra("3.22.1+")

val androidSourceCompatibility by extra(JavaVersion.VERSION_11)
val androidTargetCompatibility by extra(JavaVersion.VERSION_11)
val androidJvmTarget by extra(JvmTarget.JVM_11)

fun Project.configureBaseExtension() {
    extensions.findByType(BaseExtension::class)?.run {
        namespace = defaultManagerPackageName
        compileSdkVersion(androidCompileSdkVersion)
        buildToolsVersion(androidBuildToolsVersion)


        defaultConfig {
            applicationId = defaultManagerPackageName
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = androidVersionCode
            versionName = androidVersionName

            ndkVersion = androidCompileNdkVersion

            externalNativeBuild.cmake {
                version = androidCmakeVersion

                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                val flags = arrayOf(
                    "-Wall",
                    "-Qunused-arguments",
                    "-Wno-gnu-string-literal-operator-template",
                    "-fno-rtti",
                    "-fvisibility=hidden",
                    "-fvisibility-inlines-hidden",
                    "-fno-exceptions",
                    "-fno-stack-protector",
                    "-fomit-frame-pointer",
                    "-Wno-builtin-macro-redefined",
                    "-Wno-unused-value",
                    "-D__FILE__=__FILE_NAME__",
                )
                cppFlags("-std=c++20", *flags)
                cFlags("-std=c18", *flags)
                arguments(
                    "-DANDROID_STL=none",
                )
            }

            signingConfigs.create("config") {
                val androidStoreFile = project.findProperty("androidStoreFile") as String?
                if (!androidStoreFile.isNullOrEmpty()) {
                    storeFile = rootProject.file(androidStoreFile)
                    storePassword = project.property("androidStorePassword") as String
                    keyAlias = project.property("androidKeyAlias") as String
                    keyPassword = project.property("androidKeyPassword") as String
                }
            }
        }

        buildTypes {
            all {
                signingConfig = if (signingConfigs["config"].storeFile != null) signingConfigs["config"] else signingConfigs["debug"]
            }
        }

        compileOptions {
            sourceCompatibility(androidSourceCompatibility)
            targetCompatibility(androidTargetCompatibility)
        }

    }

    extensions.findByType(ApplicationExtension::class)?.lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    extensions.findByType(ApplicationAndroidComponentsExtension::class)?.let { androidComponents ->
        val optimizeReleaseRes = task("optimizeReleaseRes").doLast {
            val aapt2 = File(
                androidComponents.sdkComponents.sdkDirectory.get().asFile,
                "build-tools/${androidBuildToolsVersion}/aapt2"
            )
            val zip = java.nio.file.Paths.get(
                rootProject.layout.buildDirectory.toString(),
                "intermediates",
                "optimized_processed_res",
                "release",
                "optimizeReleaseResources",
                "resources-release-optimize.ap_"
            )
            val optimized = File("${zip}.opt")
            val cmd = exec {
                commandLine(
                    aapt2, "optimize",
                    "--collapse-resource-names",
                    "--enable-sparse-encoding",
                    "-o", optimized,
                    zip
                )
                isIgnoreExitValue = false
            }
            if (cmd.exitValue == 0) {
                delete(zip)
                optimized.renameTo(zip.toFile())
            }
        }

        tasks.configureEach {
            if (name == ":manager:optimizeReleaseResources") {
                finalizedBy(optimizeReleaseRes)
            }
        }
    }
}

listOf("Debug", "Release").forEach { variant ->
    tasks.register("build${variant}") {
        description = "Build YAPatch with $variant"
        println(description)

        dependsOn(projects.jar.dependencyProject.tasks["build${variant}"])
        dependsOn(projects.manager.dependencyProject.tasks["build${variant}"])
    }
}

tasks.register("buildAll") {
    dependsOn("buildDebug", "buildRelease")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }

    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
}