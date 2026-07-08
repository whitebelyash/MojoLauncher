package net.kdt.pojavlaunch.fragments

import net.kdt.pojavlaunch.extra.ExtraConstants

class ElyByLoginFragment : OAuthFragment("internalredirect",
    "https://account.ely.by/oauth2/v1" +
            "?client_id=mojolauncher2" +
            "&redirect_uri=internalredirect%3A%2F%2Fcomplete" +
            "&response_type=code" +
            "&scope=account_info%20offline_access%20minecraft_server_session",
    ExtraConstants.ELYBY_LOGIN_TODO) {
    companion object {
        const val TAG = "ELYBY_LOGIN_FRAGMENT"
    }
}
