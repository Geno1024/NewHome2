package g.bs

import org.gradle.api.Project
import java.io.File

class BuildCount(val project: Project, val name: String)
{
    fun read() = project.file("$name-count.txt").takeIf(File::exists)?.readText(Charsets.UTF_8)?.trim()?.toInt()?:0

    fun inc(): File = project.file("$name-count.txt").apply {
        writeText((takeIf(File::exists)?.readText(Charsets.UTF_8)?.trim()?.toInt()?.inc()?:1).toString())
    }
}
