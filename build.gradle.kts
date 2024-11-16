import java.net.URI
import kotlin.io.path.toPath

plugins {
    kotlin("jvm") version "2.0.0"
}

dependencies {

}

tasks.register("nh2PublishKotlinStdlib") {
    group = "publishing"
    doLast {
        configurations.runtimeClasspath.get().files.forEach { file ->
            file.copyTo(URI("file:///NH2Publish/lib/${file.name}").toPath().toFile(), true)
        }
    }
}
