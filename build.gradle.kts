import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()
val platformVersion = providers.gradleProperty("platformVersion").get()
val targetIde = providers.gradleProperty("targetIde").orNull ?: "rider"
val localIdePath = providers.gradleProperty("localIdePath").orNull

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        when {
            !localIdePath.isNullOrBlank() -> local(localIdePath)
            targetIde.equals("rider", ignoreCase = true) -> rider(platformVersion)
            targetIde.equals("webstorm", ignoreCase = true) -> webstorm(platformVersion)
            targetIde.equals("intellij", ignoreCase = true) -> intellijIdea(platformVersion)
            else -> error("Unsupported targetIde: $targetIde. Use rider, webstorm, or intellij.")
        }
        pluginVerifier()
        zipSigner()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.add("-jvm-default=enable")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("sinceBuild")
        }

        vendor {
            name = "zefir"
        }
    }

    publishing {
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    wrapper {
        gradleVersion = "9.4.1"
    }
}
