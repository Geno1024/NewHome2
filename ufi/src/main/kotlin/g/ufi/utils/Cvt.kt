@file:OptIn(ExperimentalUnsignedTypes::class)

package g.ufi.utils

import java.util.*

fun String.toSnakeCase() = split("-").map { part ->
    part.lowercase(Locale.getDefault()).replaceFirstChar { c -> c.uppercase(Locale.getDefault()) }
}.joinToString("-")

fun UShort.toUByteArrayBE() = ubyteArrayOf(
    div(256U).toUByte(),
    toUByte()
)

fun UInt.toUByteArrayBE() = ubyteArrayOf(
    shr(24).toUByte(),
    shr(16).toUByte(),
    shr(8).toUByte(),
    toUByte()
)
