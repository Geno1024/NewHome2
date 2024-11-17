@file:OptIn(ExperimentalUnsignedTypes::class)

package g.ufi

import g.ufi.utils.read1
import g.ufi.utils.read2UBE
import g.ufi.utils.read4UBE
import java.io.InputStream
import java.io.OutputStream

class GPG
{
    class Packet(
        var format: Format = Format.OpenPGP,
        var type: Type = Type.UserID,
        var length: UInt = 0U,
        var body: UByteArray = ubyteArrayOf(),
    ) : UFI
    {
        enum class Format(val format: UByte, val typeIdMask: UByte, val typeIdMaskShr: Int)
        {
            OpenPGP(3U, 0b00111111U, 0),
            Legacy(2U, 0b00111100U, 2);

            companion object
            {
                fun fromValue(format: UByte) = entries.firstOrNull { (format and 0b1100_0000U).toInt() shr 6 == it.format.toInt() }?: OpenPGP
            }
        }

        enum class Type(val id: UByte)
        {
            PublicKeyEncryptedSessionKey(1U),
            Signature(2U),
            SymmetricKeyEncryptedSessionKey(3U),
            OnePassSignature(4U),
            SecretKey(5U),
            PublicKey(6U),
            SecretSubkey(7U),
            CompressedData(8U),
            SymmetricallyEncryptedData(9U),
            Marker(10U),
            LiteralData(11U),
            Trust(12U),
            UserID(13U),
            PublicSubkey(14U),
            UserAttribute(17U),
            SymmetricallyEncryptedAndIntegrityProtectedData(18U),
            Padding(21U);

            companion object
            {
                fun fromValue(id: UByte, format: Format) = entries.firstOrNull { (id and format.typeIdMask).toInt() shr format.typeIdMaskShr == it.id.toInt() }?: UserID
            }
        }

        fun readLength(header: UByte, format: Format, input: InputStream): UInt = with (input) {
            when (format)
            {
                Format.OpenPGP -> when (val first = read1())
                {
                    in 0U..191U -> first.toUInt()
                    in 192U..223U -> ((first - 192U) shl 8) + read1().toUInt() + 192U
                    in 224U..254U -> (1 shl (first and 0x1fU).toInt()).toUInt()
                    in 255U..255U -> read4UBE()
                    else -> first.toUInt()
                }
                Format.Legacy -> when (header and 0b11U)
                {
                    0U.toUByte() -> read1().toUInt()
                    1U.toUByte() -> read2UBE().toUInt()
                    2U.toUByte() -> read4UBE()
                    3U.toUByte() -> TODO()
                    else -> TODO()
                }
            }
        }

        companion object
        {
            fun read(input: InputStream) = Packet().read(input)
        }

        override fun write(output: OutputStream)
        {
            TODO("Not yet implemented")
        }

        override fun read(input: InputStream): Packet = with (input) {
            val header = read1()
            val format = Format.fromValue(header)
            val length = readLength(header, format, input)
            Packet(
                format,
                Type.fromValue(header, format),
                length,
                readNBytes(length.toInt()).toUByteArray()
            )
        }
    }

    abstract class PublicKey : UFI
    {
        abstract fun armor(): String

        data class V4(
            var packets: MutableList<Packet> = mutableListOf(),
//            var magic: UByte = 0x99U,
        ) : PublicKey()
        {
            override fun armor(): String
            {
                TODO("Not yet implemented")
            }

            companion object
            {
                fun read(input: InputStream) = V4().read(input)
            }

            override fun write(output: OutputStream) = output.write(armor().toByteArray())

            override fun read(input: InputStream): V4 = with (input) {
                V4().apply {
                    while (available() > 0)
                    {
                        packets.add(Packet.read(this@with))
                    }
                }
            }
        }
    }
}
