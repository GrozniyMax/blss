pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "blss"

include("user-service")
include("order-service")
include("status-service")
include("bitrix-jca-connector")
