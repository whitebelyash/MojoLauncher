package net.kdt.pojavlaunch.authenticator

import androidx.annotation.NonNull
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.authenticator.accounts.Account

interface BackgroundLogin {
    fun createAccount(@NonNull loginListener: LoginListener, code: String)
    fun refreshAccount(@NonNull loginListener: LoginListener, account: Account)

    interface Creator {
        fun create(): BackgroundLogin
    }
}
