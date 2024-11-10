package g.ufi.utils

import java.io.InputStream

class PartialInputStream(
    private val prefix: ByteArray,
    private val stream: InputStream,
) : InputStream()
{
    private var pos: Int = 0
    private var stringLength: Int = prefix.size
    private var prefixFinish: Boolean = false

    override fun read(): Int
    {
        if (prefixFinish)
        {
            return stream.read()
        }
        else
        {
            val c = prefix[pos++]
            if (pos == stringLength) prefixFinish = true
            return c.toInt() and 0xFF
        }
    }
}
