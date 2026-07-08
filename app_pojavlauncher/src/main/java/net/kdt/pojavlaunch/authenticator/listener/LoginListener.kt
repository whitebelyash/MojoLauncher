package net.kdt.pojavlaunch.authenticator.listener

import net.kdt.pojavlaunch.authenticator.accounts.Account

interface LoginListener {
    fun onLoginDone(account: Account)
    fun onLoginError(errorMessage: Throwable)
    fun onLoginProgress(step: Int)
    fun setMaxLoginProgress(max: Int)
}
