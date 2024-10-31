@file:OptIn(ExperimentalUnsignedTypes::class)

package g.ufi.utils

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
