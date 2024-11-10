package g.ufi.utils

import java.io.InputStream

fun InputStream.read1() = read().toUByte()

fun InputStream.read2UBE() = (read() * 256 + read()).toUShort()

fun InputStream.read4UBE() = (read() * 16777216 + read() * 65536 + read() * 256 + read()).toUInt()

fun InputStream.readStringUntil(vararg delim: Char, skipNext: Int = 0) = StringBuilder().apply {
    while (true)
    {
        val c = read().toChar()
        if (c in delim) break else append(c)
    }
    readNBytes(skipNext)
}.toString()
