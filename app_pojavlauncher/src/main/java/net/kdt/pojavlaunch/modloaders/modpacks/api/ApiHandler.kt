package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.util.ArrayMap
import android.util.Log
import com.google.gson.Gson
import net.kdt.pojavlaunch.Tools
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.util.Map
import java.util.Objects

@Suppress("unused")
class ApiHandler(val baseUrl: String, val additionalHeaders: Map<String, String>?) {
    constructor(url: String, apiKey: String) : this(url) {
        additionalHeaders = ArrayMap<String, String>().apply { put("x-api-key", apiKey) }
    }

    constructor(url: String) : this(url, null as Map<String, String>?)

    fun <T> get(endpoint: String, tClass: Class<T>): T? = getFullUrl(additionalHeaders, "$baseUrl/$endpoint", tClass)
    fun <T> get(endpoint: String, query: HashMap<String, Any>, tClass: Class<T>): T? = getFullUrl(additionalHeaders, "$baseUrl/$endpoint", query, tClass)
    fun <T> post(endpoint: String, body: T, tClass: Class<T>): T? = postFullUrl(additionalHeaders, "$baseUrl/$endpoint", body, tClass)
    fun <T> post(endpoint: String, query: HashMap<String, Any>, body: T, tClass: Class<T>): T? = postFullUrl(additionalHeaders, "$baseUrl/$endpoint", query, body, tClass)

    companion object {
        @JvmStatic
        fun getRaw(url: String): String? = getRaw(null, url)

        @JvmStatic
        fun getRaw(headers: Map<String, String>?, url: String): String? {
            Log.d("ApiHandler", url)
            return try {
                val conn = URL(url).openConnection() as HttpURLConnection
                addHeaders(conn, headers)
                val inputStream = conn.inputStream
                val data = Tools.read(inputStream)
                Log.d(ApiHandler::class.java.toString(), data)
                inputStream.close()
                conn.disconnect()
                data
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun postRaw(url: String, body: String): String? = postRaw(null, url, body)

        @JvmStatic
        fun postRaw(headers: Map<String, String>?, url: String, body: String): String? {
            return try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                addHeaders(conn, headers)
                conn.doOutput = true
                val outputStream = conn.outputStream
                val input = body.toByteArray(StandardCharsets.UTF_8)
                outputStream.write(input, 0, input.size)
                outputStream.close()
                val inputStream = conn.inputStream
                val data = Tools.read(inputStream)
                inputStream.close()
                conn.disconnect()
                data
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        private fun addHeaders(connection: HttpURLConnection, headers: Map<String, String>?) {
            if (headers != null) {
                for ((key, value) in headers) connection.addRequestProperty(key, value)
            }
        }

        private fun parseQueries(query: HashMap<String, Any>): String {
            val params = StringBuilder("?")
            for ((key, value) in query) {
                params.append(urlEncodeUTF8(key))
                    .append("=")
                    .append(urlEncodeUTF8(Objects.toString(value)))
                    .append("&")
            }
            return params.substring(0, params.length - 1)
        }

        @JvmStatic
        fun <T> getFullUrl(url: String, tClass: Class<T>): T? = getFullUrl(null, url, tClass)

        @JvmStatic
        fun <T> getFullUrl(url: String, query: HashMap<String, Any>, tClass: Class<T>): T? = getFullUrl(null, url, query, tClass)

        @JvmStatic
        fun <T> postFullUrl(url: String, body: T, tClass: Class<T>): T? = postFullUrl(null, url, body, tClass)

        @JvmStatic
        fun <T> postFullUrl(url: String, query: HashMap<String, Any>, body: T, tClass: Class<T>): T? = postFullUrl(null, url, query, body, tClass)

        @JvmStatic
        fun <T> getFullUrl(headers: Map<String, String>?, url: String, tClass: Class<T>): T? = Gson().fromJson(getRaw(headers, url), tClass)

        @JvmStatic
        fun <T> getFullUrl(headers: Map<String, String>?, url: String, query: HashMap<String, Any>, tClass: Class<T>): T? =
            getFullUrl(headers, url + parseQueries(query), tClass)

        @JvmStatic
        fun <T> postFullUrl(headers: Map<String, String>?, url: String, body: T, tClass: Class<T>): T? =
            Gson().fromJson(postRaw(headers, url, body.toString()), tClass)

        @JvmStatic
        fun <T> postFullUrl(headers: Map<String, String>?, url: String, query: HashMap<String, Any>, body: T, tClass: Class<T>): T? =
            Gson().fromJson(postRaw(headers, url + parseQueries(query), body.toString()), tClass)

        private fun urlEncodeUTF8(input: String): String {
            return try {
                URLEncoder.encode(input, "UTF-8")
            } catch (e: java.io.UnsupportedEncodingException) {
                throw RuntimeException("UTF-8 is required")
            }
        }
    }
}
