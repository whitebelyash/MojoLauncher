package net.kdt.pojavlaunch.authenticator.impl

import net.kdt.pojavlaunch.PojavApplication.sExecutorService
import android.util.ArrayMap
import android.util.Log
import androidx.annotation.NonNull
import com.kdt.mcgui.ProgressLayout
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.BackgroundLogin
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.authenticator.model.OAuthTokenResponse
import net.kdt.pojavlaunch.authenticator.accounts.Account
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.Callable

class MicrosoftBackgroundLogin private constructor() : BackgroundLogin {
    companion object {
        val CREATOR: BackgroundLogin.Creator = BackgroundLogin.Creator { MicrosoftBackgroundLogin() }
        private const val authTokenUrl = "https://login.live.com/oauth20_token.srf"
        private const val xblAuthUrl = "https://user.auth.xboxlive.com/user/authenticate"
        private const val xstsAuthUrl = "https://xsts.auth.xboxlive.com/xsts/authorize"
        private const val mcLoginUrl = "https://api.minecraftservices.com/authentication/login_with_xbox"
        private const val mcProfileUrl = "https://api.minecraftservices.com/minecraft/profile"
        private const val mcStoreUrl = "https://api.minecraftservices.com/entitlements/mcstore"

        private val XSTS_ERRORS: Map<Long, Int> = ArrayMap<Long, Int>().apply {
            put(2148916233L, R.string.xerr_no_account)
            put(2148916235L, R.string.xerr_not_available)
            put(2148916236L, R.string.xerr_adult_verification)
            put(2148916237L, R.string.xerr_adult_verification)
            put(2148916238L, R.string.xerr_child)
        }
    }

    @JvmField var msRefreshToken: String? = null
    @JvmField var mcName: String? = null
    @JvmField var mcToken: String? = null
    @JvmField var mcUuid: String? = null
    @JvmField var msXsts: String? = null
    @JvmField var doesOwnGame: Boolean = false
    @JvmField var expiresAt: Long = 0

    private fun acquireAccountDetails(
        @NonNull loginListener: LoginListener,
        continuation: Callable<Void?>,
        code: String,
        isRefresh: Boolean
    ) {
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, 0)
        sExecutorService.execute {
            loginListener.setMaxLoginProgress(5)
            try {
                notifyProgress(loginListener, 1)
                val accessToken = acquireAccessToken(isRefresh, code)
                notifyProgress(loginListener, 2)
                val xboxLiveToken = acquireXBLToken(accessToken)
                notifyProgress(loginListener, 3)
                val xsts = acquireXsts(xboxLiveToken)
                notifyProgress(loginListener, 4)
                val token = acquireToken(xsts[0], xsts[1])
                notifyProgress(loginListener, 5)
                fetchOwnedItems(token)
                checkProfile(token)
                msXsts = xsts[0]
                continuation.call()
            } catch (e: Exception) {
                Log.e("MicroAuth", "Exception thrown during authentication", e)
                Tools.runOnUiThread { loginListener.onLoginError(e) }
            } finally {
                ProgressLayout.clearProgress(ProgressLayout.AUTHENTICATE)
            }
        }
    }

    private fun fillAccount(acc: Account) {
        acc.xuid = msXsts
        acc.accessToken = mcToken
        acc.username = mcName
        acc.profileId = mcUuid
        acc.authType = AuthType.MICROSOFT
        acc.refreshToken = msRefreshToken
        acc.expiresAt = expiresAt
        acc.updateSkinFace()
    }

    override fun createAccount(@NonNull loginListener: LoginListener, code: String) {
        acquireAccountDetails(loginListener, Callable {
            val account = Accounts.create(this::fillAccount)
            Tools.runOnUiThread { loginListener.onLoginDone(account) }
            null
        }, code, false)
    }

    override fun refreshAccount(@NonNull loginListener: LoginListener, account: Account) {
        acquireAccountDetails(loginListener, Callable {
            if (doesOwnGame) fillAccount(account)
            account.save()
            Tools.runOnUiThread { loginListener.onLoginDone(account) }
            null
        }, account.refreshToken, true)
    }

    @Throws(IOException::class)
    private fun acquireAccessToken(isRefresh: Boolean, code: String): String {
        val url = URL(authTokenUrl)
        Log.i("MicrosoftLogin", "isRefresh=$isRefresh, authCode= $code")

        val formData = CommonLoginUtils.convertToFormData(
            "client_id", "00000000402b5328",
            if (isRefresh) "refresh_token" else "code", code,
            "grant_type", if (isRefresh) "refresh_token" else "authorization_code",
            "redirect_url", "https://login.live.com/oauth20_desktop.srf",
            "scope", "service::user.auth.xboxlive.com::MBI_SSL"
        )

        val response = CommonLoginUtils.exchangeAuthCode(url, formData)
        msRefreshToken = response.refreshToken
        return response.accessToken
    }

    @Throws(IOException::class, JSONException::class)
    private fun acquireXBLToken(accessToken: String): String {
        val url = URL(xblAuthUrl)
        val data = JSONObject()
        val properties = JSONObject()
        properties.put("AuthMethod", "RPS")
        properties.put("SiteName", "user.auth.xboxlive.com")
        properties.put("RpsTicket", accessToken)
        data.put("Properties", properties)
        data.put("RelyingParty", "http://auth.xboxlive.com")
        data.put("TokenType", "JWT")
        val req = data.toString()
        val conn = url.openConnection() as HttpURLConnection
        setCommonProperties(conn, req)
        conn.connect()
        try {
            val wr: OutputStream = conn.outputStream
            try {
                wr.write(req.toByteArray(StandardCharsets.UTF_8))
            } finally {
                wr.close()
            }
        }
        return if (conn.responseCode in 200..299) {
            val jo = JSONObject(Tools.read(conn.inputStream))
            conn.disconnect()
            Log.i("MicrosoftLogin", "Xbl Token = " + jo.getString("Token"))
            jo.getString("Token")
        } else {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun acquireXsts(xblToken: String): Array<String> {
        val url = URL(xstsAuthUrl)
        val data = JSONObject()
        val properties = JSONObject()
        properties.put("SandboxId", "RETAIL")
        properties.put("UserTokens", JSONArray(Collections.singleton(xblToken)))
        data.put("Properties", properties)
        data.put("RelyingParty", "rp://api.minecraftservices.com/")
        data.put("TokenType", "JWT")
        val req = data.toString()
        Log.i("MicroAuth", req)
        val conn = url.openConnection() as HttpURLConnection
        setCommonProperties(conn, req)
        Log.i("MicroAuth", conn.requestMethod)
        conn.connect()
        try {
            val wr: OutputStream = conn.outputStream
            try {
                wr.write(req.toByteArray(StandardCharsets.UTF_8))
            } finally {
                wr.close()
            }
        }
        return when {
            conn.responseCode in 200..299 -> {
                val jo = JSONObject(Tools.read(conn.inputStream))
                val uhs = jo.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0).getString("uhs")
                val token = jo.getString("Token")
                conn.disconnect()
                Log.i("MicrosoftLogin", "Xbl Xsts = $token; Uhs = $uhs")
                arrayOf(uhs, token)
            }

            conn.responseCode == 401 -> {
                val responseContents = Tools.read(conn.errorStream)
                val jo = JSONObject(responseContents)
                val xerr = jo.optLong("XErr", -1)
                val localeId = XSTS_ERRORS[xerr]
                if (localeId != null) {
                    throw PresentedException(RuntimeException(responseContents), localeId)
                }
                throw PresentedException(RuntimeException(responseContents), R.string.xerr_unknown, xerr)
            }

            else -> throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun acquireToken(xblUhs: String, xblXsts: String): String {
        val url = URL(mcLoginUrl)
        val data = JSONObject()
        data.put("identityToken", "XBL3.0 x=$xblUhs;$xblXsts")
        val req = data.toString()
        val conn = url.openConnection() as HttpURLConnection
        setCommonProperties(conn, req)
        conn.connect()
        try {
            val wr: OutputStream = conn.outputStream
            try {
                wr.write(req.toByteArray(StandardCharsets.UTF_8))
            } finally {
                wr.close()
            }
        }
        return if (conn.responseCode in 200..299) {
            expiresAt = System.currentTimeMillis() + 86400000
            val jo = JSONObject(Tools.read(conn.inputStream))
            conn.disconnect()
            Log.i("MicrosoftLogin", "MC token: " + jo.getString("access_token"))
            mcToken = jo.getString("access_token")
            jo.getString("access_token")
        } else {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    @Throws(IOException::class)
    private fun fetchOwnedItems(mcAccessToken: String) {
        val url = URL(mcStoreUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $mcAccessToken")
        conn.useCaches = false
        conn.connect()
        if (conn.responseCode !in 200..299) {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun checkProfile(mcAccessToken: String) {
        val url = URL(mcProfileUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $mcAccessToken")
        conn.useCaches = false
        conn.connect()

        if (conn.responseCode in 200..299) {
            val s = Tools.read(conn.inputStream)
            conn.disconnect()
            Log.i("MicrosoftLogin", "profile:$s")
            val jsonObject = JSONObject(s)
            val name = jsonObject["name"] as String
            val uuid = jsonObject["id"] as String
            val uuidDashes = uuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(),
                "$1-$2-$3-$4-$5"
            )
            doesOwnGame = true
            Log.i("MicrosoftLogin", "UserName = $name")
            Log.i("MicrosoftLogin", "Uuid = $uuidDashes")
            mcName = name
            mcUuid = uuidDashes
        } else {
            Log.i("MicrosoftLogin", "It seems that this Microsoft Account does not own the game.")
            doesOwnGame = false
            throw PresentedException(RuntimeException(conn.responseMessage), R.string.mc_not_owned)
        }
    }

    private fun notifyProgress(listener: LoginListener, step: Int) {
        Tools.runOnUiThread { listener.onLoginProgress(step) }
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, step * 20)
    }

    private fun setCommonProperties(conn: HttpURLConnection, formData: String) {
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("charset", "utf-8")
        try {
            conn.setRequestProperty(
                "Content-Length",
                formData.toByteArray(StandardCharsets.UTF_8).size.toString()
            )
            conn.requestMethod = "POST"
        } catch (e: ProtocolException) {
            Log.e("MicrosoftAuth", e.toString())
        }
        conn.useCaches = false
        conn.doInput = true
        conn.doOutput = true
    }
}
