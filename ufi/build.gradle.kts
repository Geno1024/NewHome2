import g.bs.BuildCount
import g.bs.NH2Publish
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.util.jar.Attributes

plugins {
    kotlin("jvm")
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
            Attributes.Name.IMPLEMENTATION_VENDOR.toString() to "Geno1024",
            Attributes.Name.CLASS_PATH.toString() to configurations.runtimeClasspath.get().joinToString(" ") { "file:///NH2Publish/lib/${it.name}" }
        )
    }
}

tasks.register("nh2Publish") {
    group = "publish"
    dependsOn(jarCount)
    dependsOn(tasks.getByName("jar"))
    doLast {
        tasks.getByName("jar").outputs.files.files.forEach {
            NH2Publish().publish(it, URI("file:///NH2Publish/lib/${project.name}.jar"))
        }
    }
}
