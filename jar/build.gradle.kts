import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.java.library)
}

val defaultManagerPackageName: String by rootProject.extra
val androidVersionCode: Int by rootProject.extra
val androidVersionName: String by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val androidJvmTarget: JvmTarget by rootProject.extra


fun Jar.configure(variant: String) {
    archiveBaseName.set("jar-${androidVersionName}-${variant}")
    destinationDirectory.set(file("${rootProject.projectDir}/out/jar/${variant}"))
    manifest {
        attributes("Main-Class" to "${defaultManagerPackageName}.patch.Main")
    }
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    into("assets") {
        from("src/main/assets")
        from("${rootProject.projectDir}/out/assets/${variant}")
    }

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.MF", "META-INF/*.txt", "META-INF/versions/**")
}

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
}

tasks.register<Jar>("buildDebug") {
    dependsOn(":patch-loader:copyDebugAssets")
    configure("debug")
}

tasks.register<Jar>("buildRelease") {
    dependsOn(":patch-loader:copyReleaseAssets")
    configure("release")
}

dependencies {
    implementation(projects.patch)
}