package com.foyle.testing

import spark.utils.IOUtils

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class Client(private val server: String) {

    @JvmOverloads
    fun request(method: String, uri: String, requestBody: String? = null): Response {
        try {
            val url = URL(server + uri)

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.setRequestProperty("Content-Type", "application/json")
            if (requestBody != null) {
                connection.doOutput = true
                connection.outputStream.use { os -> os.write(requestBody.toByteArray(charset("UTF-8"))) }
            }
            connection.connect()
            val inputStream = if (connection.responseCode < 400)
                connection.inputStream
            else
                connection.errorStream

            val body = IOUtils.toString(inputStream)

            return Response(connection.responseCode, body)
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("Whoops!  Connection error")
        }
    }
}
