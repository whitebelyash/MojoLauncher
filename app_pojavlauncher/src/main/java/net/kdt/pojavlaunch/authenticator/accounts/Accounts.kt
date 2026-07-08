package net.kdt.pojavlaunch.authenticator.accounts

import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import java.io.File
import java.io.IOException
import java.util.*

class Accounts private constructor(
    @JvmField val accounts: List<Account>,
    @JvmField val selectionIndex: Int
) {
    companion object {
        private const val PROFILE_PREF_FILE = "selected_account_file"

        @Throws(IOException::class)
        fun load(): Accounts {
            val accountsDir = File(Tools.DIR_ACCOUNT_NEW)
            synchronized(Accounts::class.java) {
                FileUtils.ensureDirectory(accountsDir)
            }
            val accountFiles = accountsDir.listFiles()
                ?: throw IOException("Failed to create account directory")
            val selectedAccount = getSelectedAccount()
            val accounts = ArrayList<Account>(accountFiles.size)
            var selectedAccountIdx = 0
            for (accFile in accountFiles) {
                val account = loadAccount(accFile) ?: continue
                accounts.add(account)
                if (accFile.name == selectedAccount) {
                    selectedAccountIdx = accounts.size - 1
                }
            }
            accounts.trimToSize()
            return Accounts(Collections.unmodifiableList(accounts), selectedAccountIdx)
        }

        private fun loadAccount(source: File): Account? {
            val acc: Account
            try {
                acc = JSONUtils.readFromFile(source, Account::class.java) ?: return null
            } catch (e: Exception) {
                Log.w("Accounts", "Failed to load account", e)
                return null
            }
            acc.mSaveLocation = source
            if (acc.accessToken == null) acc.accessToken = "0"
            if (acc.profileId == null) acc.profileId = "00000000-0000-0000-0000-000000000000"
            if (acc.username == null) acc.username = "0"
            if (acc.refreshToken == null) acc.refreshToken = "0"
            if (acc.authType == null) {
                acc.authType = if (acc.isMicrosoft) AuthType.MICROSOFT else AuthType.LOCAL
            }
            return acc
        }

        private fun getSelectedAccount(): String {
            return LauncherPreferences.DEFAULT_PREF.getString(PROFILE_PREF_FILE, "")
        }

        fun getCurrent(): Account? {
            val selectedAccount = getSelectedAccount()
            return loadAccount(File(Tools.DIR_ACCOUNT_NEW, selectedAccount))
        }

        private fun pickAccountPath(): File {
            var profilePath: File
            do {
                val profileName = UUID.randomUUID().toString()
                profilePath = File(Tools.DIR_ACCOUNT_NEW, profileName)
            } while (profilePath.exists())
            return profilePath
        }

        @Throws(IOException::class)
        fun create(setter: Setter): Account {
            val account = Account()
            setter.writeAccount(account)
            account.mSaveLocation = pickAccountPath()
            account.save()
            return account
        }

        fun setCurrent(account: Account) {
            LauncherPreferences.DEFAULT_PREF
                .edit().putString(PROFILE_PREF_FILE, account.mSaveLocation?.name)
                .apply()
        }

        fun delete(account: Account) {
            account.mSaveLocation?.delete()
        }
    }

    fun interface Setter {
        @Throws(IOException::class)
        fun writeAccount(account: Account)
    }
}
