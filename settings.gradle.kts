rootProject.name = "NH2"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":swdb:dbmgr")
include(":swdb:gateway")
include(":swdb:swtpl")

include(":ufi")
