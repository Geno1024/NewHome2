package g.ufi

import java.io.InputStream
import java.io.OutputStream

interface UFI
{
    fun write(output: OutputStream)
    fun read(input: InputStream): UFI
}
