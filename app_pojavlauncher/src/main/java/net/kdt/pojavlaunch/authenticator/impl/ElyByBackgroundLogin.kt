package net.kdt.pojavlaunch.authenticator.impl

import net.kdt.pojavlaunch.PojavApplication.sExecutorService
import android.util.Log
import androidx.annotation.NonNull
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.BackgroundLogin
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.authenticator.model.OAuthTokenResponse
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable

class ElyByBackgroundLogin private constructor() : BackgroundLogin {
    companion object {
        val CREATOR: BackgroundLogin.Creator = BackgroundLogin.Creator { ElyByBackgroundLogin() }
        private const val authTokenUrl = "https://account.ely.by/api/oauth2/v1/token"
        private const val accountInfoUrl = "https://account.ely.by/api/account/v1/info"
    }

    private var mOAuthData: OAuthTokenResponse? = null
    private var mAccountInfo: ElyAccountInfo? = null
    private var mExpiresAt: Long = 0

    private fun acquireAccountDetails(
        @NonNull loginListener: LoginListener,
        continuation: Callable<Void?>,
        code: String,
        isRefresh: Boolean
    ) {
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, 0)
        sExecutorService.execute {
            loginListener.setMaxLoginProgress(2)
            try {
                notifyProgress(loginListener, 1)
                acquireTokens(isRefresh, code)
                notifyProgress(loginListener, 2)
                mAccountInfo = acquireAccountData(mOAuthData!!.accessToken)
                continuation.call()
            } catch (e: Exception) {
                Log.e("MicroAuth", "Exception thrown during authentication", e)
                Tools.runOnUiThread { loginListener.onLoginError(e) }
            }
            ProgressLayout.clearProgress(ProgressLayout.AUTHENTICATE)
        }
    }

    private fun fillAccount(acc: Account) {
        acc.expiresAt = mExpiresAt
        acc.authType = AuthType.ELY_BY
        acc.accessToken = mOAuthData!!.accessToken
        acc.refreshToken = mOAuthData!!.refreshToken
        acc.username = mAccountInfo!!.username
        acc.profileId = mAccountInfo!!.uuid
        acc.xuid = null
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
            fillAccount(account)
            account.save()
            Tools.runOnUiThread { loginListener.onLoginDone(account) }
            null
        }, account.refreshToken, true)
    }

    @Throws(IOException::class)
    private fun acquireTokens(isRefresh: Boolean, code: String) {
        val url = URL(authTokenUrl)
        Log.i("MicrosoftLogin", "isRefresh=$isRefresh, authCode= $code")
        val formData = CommonLoginUtils.convertToFormData(
            "client_id", "mojolauncher2",
            "client_secret", "o14Zb2Zzj0_k6o4kN0t1mIEhoQxeayn8hYi5VSX2q3NXrdQm5T2Q6wqsCfpv1vhu",
            "redirect_uri", "internalredirect://complete",
            if (isRefresh) "refresh_token" else "code", code,
            "grant_type", if (isRefresh) "refresh_token" else "authorization_code"
        )
        mOAuthData = CommonLoginUtils.exchangeAuthCode(url, formData)
        mExpiresAt = mOAuthData!!.expiresIn * 1000 + System.currentTimeMillis()
    }

    @Throws(IOException::class)
    private fun acquireAccountData(accessToken: String): ElyAccountInfo {
        val url = URL(accountInfoUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.useCaches = false
        conn.connect()
        return if (conn.responseCode in 200..299) {
            try {
                val reader = InputStreamReader(conn.inputStream)
                try {
                    Tools.GLOBAL_GSON.fromJson(reader, ElyAccountInfo::class.java)
                } finally {
                    reader.close()
                }
            } finally {
                conn.disconnect()
            }
        } else {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    private fun notifyProgress(listener: LoginListener, step: Int) {
        Tools.runOnUiThread { listener.onLoginProgress(step) }
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, step * 50)
    }

    private class ElyAccountInfo {
        @JvmField var uuid: String? = null
        @JvmField var username: String? = null
    }
}
