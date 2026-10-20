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
rootProject.name = "CommercialCarApp"

// App Application
include(":app")

// Core Libraries (Middleware, Data, System Services)
include(":core:common")
include(":core:model")
include(":core:aidl")
include(":core:vehicle")
include(":core:vehicle-service")
include(":core:uxrestrictions")
include(":core:power")
include(":core:audio")
include(":core:digitalkey")
include(":core:telematics")
include(":core:adas")
include(":core:account")
include(":core:data")
include(":core:designsystem")

// Feature Modules (Multi-Screen Cockpit Apps)
include(":feature:dashboard")
include(":feature:cluster")
include(":feature:climate")
include(":feature:media")
include(":feature:connected")
include(":feature:settings")
include(":feature:voice")
