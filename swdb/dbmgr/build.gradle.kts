import g.bs.BuildCount
import g.bs.Dependencies
import g.bs.NH2Publish
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.util.jar.Attributes
import kotlin.io.path.toPath

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(Dependencies.KOTLIN_REFLECT)
    implementation(Dependencies.SQLITE_JDBC)

    implementation(project(":ufi"))
}

// <editor-fold desc="Build Count">
val run = BuildCount(project, "run")

val runCount = tasks.register("runCount") {
    group = "buildCount"
    doLast {
        run.inc()
    }
}

val jar = BuildCount(project, "jar")

val jarCount = tasks.register("jarCount") {
    group = "buildCount"
    doLast {
        jar.inc()
    }
}
// </editor-fold>

tasks.withType<KotlinCompile> {
    dependsOn(runCount)
}

tasks.withType<Jar> {
    version = "1.0.${jar.read()}.${run.read()}"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            Attributes.Name.MAIN_CLASS.toString() to application.mainClass,
            Attributes.Name.IMPLEMENTATION_VENDOR.toString() to "Geno1024",
            Attributes.Name.CLASS_PATH.toString() to configurations.runtimeClasspath.get().joinToString(" ") { "file:///NH2Publish/lib/${it.name}" }
        )
    }
}

application {
    mainClass = "g.sw.DbMgr"
}

tasks.register("nh2Publish") {
    group = "publish"
    dependsOn(jarCount)
    dependsOn(tasks.getByName("jar"))
    doLast {
        tasks.getByName("jar").outputs.files.files.forEach {
            NH2Publish().publish(it, URI("file:///NH2Publish/bin/${project.name}.jar"))
        }
        configurations.runtimeClasspath.get().files.filter {
            it.canonicalPath.contains(".gradle")
        }.forEach { file ->
            file.copyTo(URI("file:///NH2Publish/lib/${file.name}").toPath().toFile(), true)
        }
    }
}
