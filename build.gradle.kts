import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.changelog") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // IntelliJ 2024.2 bundles Kotlin 2.0 – don't use newer stdlib APIs.
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        // Use JVM interface default methods directly; avoids generated bridges for deprecated
        // platform defaults such as DynamicPluginListener.checkUnloadPlugin.
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
        // Notes of the released version from CHANGELOG.md, or the [Unreleased] section if not released yet.
        changeNotes = provider {
            with(changelog) {
                renderItem(
                    (getOrNull(project.version.toString()) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }
    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("platformVersion"))
        }
    }
    // Secrets are only needed for signPlugin/publishPlugin (release workflow); buildPlugin works without them.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

changelog {
    groups = listOf("Added", "Changed", "Fixed", "Removed")
    repositoryUrl = "https://github.com/fl0gru85/progress-tractor-plugin"
}

tasks.test {
    systemProperty("java.awt.headless", "true")
}

// Renders a preview PNG of the progress bar next to the plugin ZIP on every buildPlugin.
val renderPreview by tasks.registering(JavaExec::class) {
    description = "Renders a preview image of the tractor progress bar into build/distributions."
    group = "intellij platform"
    val output = layout.buildDirectory.file("distributions/${project.name}-${project.version}-preview.png")
    // Same classpath as the unit tests, which includes the IntelliJ Platform jars (JBColor, Kotlin stdlib, ...).
    classpath = files(tasks.test.map { it.classpath })
    mainClass = "com.progresstractor.PreviewRenderer"
    systemProperty("java.awt.headless", "true")
    argumentProviders.add(CommandLineArgumentProvider { listOf(output.get().asFile.absolutePath) })
    inputs.files(sourceSets.main.get().output, sourceSets.test.get().output)
    outputs.file(output)
}

tasks.buildPlugin {
    finalizedBy(renderPreview)
}
