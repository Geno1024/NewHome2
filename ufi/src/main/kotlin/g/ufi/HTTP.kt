package g.ufi

import g.ufi.utils.readStringUntil
import g.ufi.utils.toSnakeCase
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.*
import java.util.zip.GZIPInputStream

class HTTP
{
    enum class Method
    {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        PATCH,
        CONNECT
    }

    data class Url(
        var scheme: String = "",
        var host: String = "",
        var path: String = "",
        var query: String = "",
    )
    {
        companion object
        {
            fun read(string: String) = Url(
                if (string.contains("://")) string.substringBefore("://") else "",
                if (string.contains("://")) string.substringAfter("://").substringBefore("/") else if (string.startsWith("/")) "" else string.substringAfter("/"),
                (if (string.startsWith("/")) string else (string.substringAfter("://").substringAfter("/", "") + "/")).substringBefore("?"),
                string.substringAfter("?", "")
            )
        }

        override fun toString(): String = "$scheme://$host$path?$query"
    }

    enum class Version(val presentation: String)
    {
        HTTP09("HTTP/0.9"),
        HTTP10("HTTP/1.0"),
        HTTP11("HTTP/1.1");

        companion object
        {
            fun parse(presentation: String) = entries.firstOrNull { it.presentation == presentation } ?: HTTP10
        }
    }

    abstract class Body : UFI
    {
        companion object
        {
            fun inferBody(contentType: String?, transferEncoding: String?, contentLength: Int?, input: InputStream): Body =
                when
                {
                    (transferEncoding == null) and (contentType?.startsWith("multipart/form-data") == false) ->
                    {
                        PlainContent(input.readNBytes(contentLength?:0))
                    }



                    else ->
                    {
                        PlainContent()
                    }
                }
        }

        abstract fun getBodyStream(): InputStream

        data class PlainContent(
            var content: ByteArray = ByteArray(0)
        ) : Body()
        {
            override fun getBodyStream(): InputStream = ByteArrayInputStream(content)

            override fun write(output: OutputStream) = output.write(content)

            override fun read(input: InputStream): PlainContent = TODO()

            fun read(input: InputStream, bytes: Int): PlainContent = PlainContent(input.readNBytes(bytes))
        }

        data class GzipContent(
            var gzipped: ByteArray = ByteArray(0)
        ) : Body()
        {
            override fun getBodyStream(): InputStream = GZIPInputStream(ByteArrayInputStream(gzipped))

            override fun write(output: OutputStream) = output.write(gzipped)

            override fun read(input: InputStream): GzipContent = TODO()

            fun read(input: InputStream, bytes: Int): GzipContent = GzipContent(input.readNBytes(bytes))
        }

        data class Chunked(
            var chunk: MutableList<Chunk> = mutableListOf()
        )
        {
            data class Chunk(
                var length: Int = 0,
                var body: ByteArray = ByteArray(0)
            )
            {
                fun read(input: InputStream) = with (input) {
                    Chunk(readStringUntil('\r', skipNext = 1).toInt(16)).apply {
                        body = readNBytes(length)
                    }
                }
            }
        }
    }

    data class Request(
        var method: Method = Method.GET,
        var url: Url = Url("/", ""),
        var version: Version = Version.HTTP11,
        var headers: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var body: Body = Body.PlainContent()
    ) : UFI
    {
        companion object
        {
            fun read(input: InputStream): Request = Request().read(input)
        }

        override fun write(output: OutputStream) = with (output) {
            write("$method $url ${version.presentation}\r\n".toByteArray())
            headers.forEach { (k, v) -> write("$k: ${v.joinToString("; ")}\r\n".toByteArray()) }
            write("\r\n".toByteArray())
            body.write(output)
            write("\r\n".toByteArray())
        }

        override fun read(input: InputStream): Request = with (input) {
            Request(
                Method.valueOf(readStringUntil(' ')),
                Url.read(readStringUntil(' ')),
                Version.parse(readStringUntil('\r', skipNext = 1)),
            ).apply {
                while (true)
                {
                    val headerLine = readStringUntil('\r', skipNext = 1)
                    if (headerLine.isEmpty()) break
                    var (key, value) = headerLine.split(':', limit = 2)
                    key = key.trim().toSnakeCase()
                    value = value.trim()
                    if (headers.containsKey(key)) headers[key]!! += value
                    else headers[key] = mutableListOf(value)
                }
            }
        }

        fun getHost() = headers.getOrDefault("Host", mutableListOf(url.host))[0].split(":")[0]

        fun getPort() = url.host.split(":").getOrNull(1)?.toInt()?:run {
            when (url.scheme.lowercase(Locale.getDefault()))
            {
                "http" -> 80
                "https" -> 443
                else -> 80
            }
        }

        fun send(host: String = getHost(), port: Int = getPort(), proxy: Proxy = Proxy.NO_PROXY) = with (Socket(proxy)) socket@ {
            connect(InetSocketAddress(host, port))
            this@Request.write(this@socket.getOutputStream())
            Response.read(this@socket.getInputStream())
        }

        override fun toString(): String = "$method $url $version ${headers.size}"

        fun toVerboseString(): String = "$method $url $version\n" + headers.entries.joinToString("\n") { "${it.key}: ${it.value.joinToString("; ")}" }
    }

    data class Response(
        var version: Version = Version.HTTP11,
        var status: Int = 200,
        var message: String = "",
        var headers: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var body: Body = Body.PlainContent()
    ) : UFI
    {
        companion object
        {
            fun read(input: InputStream): Response = Response().read(input)
        }

        override fun write(output: OutputStream) = with (output) {
            write("${version.presentation} $status $message\r\n".toByteArray())
            headers.forEach { (k, v) -> write("$k: ${v.joinToString("; ")}\r\n".toByteArray()) }
            write("\r\n".toByteArray())
            body.write(output)
            write("\r\n".toByteArray())
        }

        override fun read(input: InputStream): Response = with (input) input@ {
            Response(
                Version.parse(readStringUntil(' ')),
                readStringUntil(' ').toInt(),
                readStringUntil('\r', skipNext = 1),
            ).apply {
                while (true)
                {
                    val headerLine = readStringUntil('\r', skipNext = 1)
                    if (headerLine.isEmpty()) break
                    var (key, value) = headerLine.split(':', limit = 2)
                    key = key.trim().toSnakeCase()
                    value = value.trim()
                    if (headers.containsKey(key)) headers[key]!! += value
                    else headers[key] = mutableListOf(value)
                }
                body = Body.inferBody(headers["Content-Type"]?.get(0), headers["Transfer-Encoding"]?.get(0), headers["Content-Length"]?.get(0)?.toInt(), this@input)
            }
        }

    }
}
