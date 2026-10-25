pluginManagement {
    includeBuild("build-logic")
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

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "nextbill"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":core:model", ":core:domain", ":core:testing", ":core:database")
include(":core:datastore", ":core:data", ":core:notifications")
include(":core:designsystem", ":core:ui", ":core:navigation")
include(":widget")
include(
    ":feature:home:api",
    ":feature:calendar:api",
    ":feature:editor:api",
    ":feature:detail:api",
    ":feature:archive:api",
    ":feature:settings:api"
)
