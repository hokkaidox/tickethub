pluginManagement {
    includeBuild("build-logic")
}

plugins {
    // Скачивает JDK 25 для toolchain, если его нет на машине (CI)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "tickethub"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
