package g.ufi.pvt

import g.ufi.UFI
import g.ufi.utils.readStringUntil
import java.io.InputStream
import java.io.OutputStream

data class GMRequest(
    var service: String = "",
    var headers: MutableMap<String, String> = mutableMapOf(),
    var initiator: GMInitiator = GMInitiator()
) : UFI
{
    companion object
    {
        fun read(input: InputStream) = GMRequest(
            input.readStringUntil('\n'),
            (0 until input.readStringUntil('\n').toInt())
                .associate {
                    with (input.readStringUntil('\n').split(": ", limit = 2)) {
                        this[0] to this[1]
                    }
                }
                .toMutableMap()
        ).apply {
            initiator = GMInitiator(headers["Initiator"]?:"")
        }
    }

    override fun write(output: OutputStream) = output.write(toString().toByteArray())

    override fun read(input: InputStream) = GMRequest.read(input)

    override fun toString() = "$service\n" +
        "${headers.size}\n" +
        headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }

    data class GMInitiator(
        var name: String = ""
    ) : UFI
    {
        companion object
        {
            fun read(input: InputStream) = GMInitiator(input.readStringUntil(' '))
        }

        override fun write(output: OutputStream) = output.write(toString().toByteArray())

        override fun read(input: InputStream) = GMInitiator.read(input)

        override fun toString(): String = "Initiator: $name"
    }
}
