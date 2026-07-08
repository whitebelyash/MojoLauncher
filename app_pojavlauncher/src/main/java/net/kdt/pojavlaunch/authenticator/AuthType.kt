package net.kdt.pojavlaunch.authenticator

import com.google.gson.annotations.SerializedName
import net.kdt.pojavlaunch.authenticator.impl.ElyByBackgroundLogin
import net.kdt.pojavlaunch.authenticator.impl.MicrosoftBackgroundLogin
import git.artdeell.mojo.R

enum class AuthType(
    private val mCreator: BackgroundLogin.Creator?,
    @JvmField val iconResource: Int,
    @JvmField val injectorUrl: String?,
    @JvmField val skinUrl: String?
) {
    @SerializedName("microsoft")
    MICROSOFT(
        MicrosoftBackgroundLogin.CREATOR,
        R.drawable.ic_auth_ms,
        null,
        "https://mineskin.eu/skin/%s"
    ),
    @SerializedName("elyby")
    ELY_BY(
        ElyByBackgroundLogin.CREATOR,
        R.drawable.ic_auth_elyby,
        "ely.by",
        "http://skinsystem.ely.by/skins/%s.png"
    ),
    @SerializedName("local")
    LOCAL(null, 0, null, null);

    fun requiresLogin(): Boolean = mCreator != null

    fun createAuth(): BackgroundLogin {
        if (mCreator == null) throw RuntimeException("This account does not support login")
        return mCreator.create()
    }
}
