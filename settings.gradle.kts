pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Zemberek is published to the maintainer's GitHub-hosted Maven repo, not
        // Maven Central. Scoped to the zemberek-nlp group so all other artifacts
        // keep resolving from Central/Google. Used only by :datapipeline.
        maven {
            url = uri("https://raw.githubusercontent.com/ahmetaa/maven-repo/master")
            content { includeGroup("zemberek-nlp") }
        }
    }
}

rootProject.name = "Turkish Suffix Practice"
include(":app")
include(":core-model")
include(":datapipeline")
