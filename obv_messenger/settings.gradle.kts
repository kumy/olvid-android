
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Olvid for Android"

include(":app", ":sardine-android")

// The Olvid engine is consumed as the published io.olvid.messenger:olvid-kotlin-engine artifact from
// Maven Central (the default). Engine development happens in the GitLab repo Olvid/kotlin-engine; the
// GitHub mirror (olvid-io/olvid-kotlin-engine) and Maven Central only receive releases.
// Local engine co-development: set olvid.engineDir to a local checkout of the GitLab engine repo to
// build the engine from source — Gradle substitutes the artifact with the included build. Omit it (or
// leave it blank) to consume the published artifact.
// Precedence: -Polvid.engineDir on the command line, then this project's gradle.properties, then
// ~/.gradle/gradle.properties, then the OLVID_ENGINE_DIR environment variable. The project file is
// read explicitly because Gradle normally lets the user-home file shadow it.
val projectEngineDir = java.util.Properties().run {
    settingsDir.resolve("gradle.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
    getProperty("olvid.engineDir")
}
(gradle.startParameter.projectProperties["olvid.engineDir"]
    ?: projectEngineDir
    ?: providers.gradleProperty("olvid.engineDir").orNull
    ?: System.getenv("OLVID_ENGINE_DIR"))
    ?.takeIf { it.isNotBlank() }
    ?.let { includeBuild(it) }
