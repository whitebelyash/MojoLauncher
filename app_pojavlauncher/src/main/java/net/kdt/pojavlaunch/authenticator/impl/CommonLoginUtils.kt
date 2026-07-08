package net.kdt.pojavlaunch.authenticator.impl

import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.model.OAuthTokenResponse
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import git.artdeell.mojo.R

object CommonLoginUtils {
    @Throws(IOException::class)
    fun exchangeAuthCode(url: URL, formData: String): OAuthTokenResponse {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("charset", "utf-8")
        conn.setRequestProperty(
            "Content-Length",
            formData.toByteArray(StandardCharsets.UTF_8).size.toString()
        )
        conn.requestMethod = "POST"
        conn.useCaches = false
        conn.doInput = true
        conn.doOutput = true
        conn.connect()
        try {
            val wr: OutputStream = conn.outputStream
            try {
                wr.write(formData.toByteArray(StandardCharsets.UTF_8))
            } finally {
                wr.close()
            }
        }
        return if (conn.responseCode in 200..299) {
            try {
                val reader = InputStreamReader(conn.inputStream)
                try {
                    Tools.GLOBAL_GSON.fromJson(reader, OAuthTokenResponse::class.java)
                } finally {
                    reader.close()
                }
            } finally {
                conn.disconnect()
            }
        } else {
            Log.i("CommonLogin", "Auth fail: " + Tools.read(conn.errorStream))
            throw getResponseThrowable(conn)
        }
    }

    fun convertToFormData(vararg data: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < data.size) {
            if (builder.isNotEmpty()) builder.append("&")
            builder.append(URLEncoder.encode(data[i], "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(data[i + 1], "UTF-8"))
            i += 2
        }
        return builder.toString()
    }

    @Throws(IOException::class)
    fun getResponseThrowable(conn: HttpURLConnection): RuntimeException {
        Log.i("MicrosoftLogin", "Error code: " + conn.responseCode + ": " + conn.responseMessage)
        return if (conn.responseCode == 429) {
            PresentedException(R.string.microsoft_login_retry_later)
        } else {
            RuntimeException(conn.responseMessage)
        }
    }
}
