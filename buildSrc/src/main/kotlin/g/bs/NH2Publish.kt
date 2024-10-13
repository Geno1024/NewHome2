package g.bs

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.toPath

class NH2Publish
{
    fun publish(file: File, target: URI)
    {
        when (target.scheme.lowercase())
        {
            "file" -> Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
