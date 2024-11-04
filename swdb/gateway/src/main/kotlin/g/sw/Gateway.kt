package g.sw

import com.sun.net.httpserver.HttpServer
import g.ufi.pvt.GMRequest
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import java.time.LocalDateTime
import kotlin.concurrent.thread

object Gateway
{
    private fun GMRequest.handleTo(output: OutputStream)
    {
        // <editor-fold desc="Verify Service">
        val target = File(URI("file:///NH2Publish/bin/$service.jar"))
        if (!target.exists() || target.canonicalFile.relativeTo(File(URI("file:///NH2Publish/bin"))).toString().contains(".."))
        {
            output.close()
            return
        }
        // </editor-fold>
        Runtime.getRuntime().exec(
            arrayOf("java", "-jar", target.canonicalPath, *headers.entries.map { "${it.key}=${it.value}" }.toTypedArray())
        ).apply {
            inputStream.copyTo(output)
            errorStream.copyTo(output)
            waitFor()
        }
    }

    fun openGp(port: Int) = thread {
        println("GP Port: $port")
        ServerSocket(port).apply server@ {
            while (true) {
                accept().apply income@ {
                    thread worker@ {
                        val request = GMRequest.read(getInputStream())
                        println("${LocalDateTime.now()} GP $request")
                        request.handleTo(this@income.getOutputStream())
                        close()
                    }
                }
            }
        }
    }

    fun openHttp(port: Int) = thread {
        println("HTTP Port: $port")
        HttpServer.create(InetSocketAddress(port), 0).apply {
            createContext("/") { exchange ->
                val url = exchange.requestURI.toString().split("/")[1]
                val headers = exchange.requestHeaders.map { (key, values) ->
                    key to values.joinToString("; ")
                }.toMap().toMutableMap()
                val request = GMRequest(url, headers, GMRequest.GMInitiator())
                println("${LocalDateTime.now()} HTTP $request")
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0)
                request.handleTo(exchange.responseBody)
                exchange.close()
            }
            start()
        }
    }

    @JvmStatic
    fun main(args: Array<String>)
    {
        args.getOrNull(0)?.toInt()?.let(::openGp)
        args.getOrNull(1)?.toInt()?.let(::openHttp)
    }
}
