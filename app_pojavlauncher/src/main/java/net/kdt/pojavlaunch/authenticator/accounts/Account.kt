package net.kdt.pojavlaunch.authenticator.accounts

import android.graphics.BitmapFactory
import android.util.Log
import net.kdt.pojavlaunch.*
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import java.io.*
import java.net.URL
import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.gson.JsonParseException
import org.apache.commons.io.IOUtils

@Keep
class Account {
    @JvmField
    @Transient
    var mSaveLocation: File? = null
    @JvmField var accessToken: String = "0"
    @JvmField var profileId: String = "00000000-0000-0000-0000-000000000000"
    @JvmField var username: String = "Steve"
    @JvmField var authType: AuthType = AuthType.LOCAL
    @JvmField var isMicrosoft: Boolean = false
    @JvmField var refreshToken: String = "0"
    @JvmField var xuid: String? = null
    @JvmField var expiresAt: Long = 0

    @Transient
    private var mFaceCache: Bitmap? = null

    protected constructor()

    fun updateSkinFace() {
        val skinFaceUrlTemplate = authType.skinUrl ?: return
        val skinFaceUrl = String.format(skinFaceUrlTemplate, username)
        try {
            Log.i("SkinLoader", "Updating skin face...")
            val skinFile = getSkinFaceFile()
            val skinBytes = IOUtils.toByteArray(URL(skinFaceUrl))
            val skinBitmap = BitmapFactory.decodeByteArray(skinBytes, 0, skinBytes.size)
                ?: return
            val skinFace = SkinHeadRenderer().render(100, skinBitmap) ?: return
            skinBitmap.recycle()
            FileOutputStream(skinFile).use { fileOutputStream ->
                skinFace.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream)
            }
            Log.i("SkinLoader", "Update skin face success")
        } catch (e: IOException) {
            Log.w("SkinLoader", "Could not update skin face", e)
        }
    }

    fun isLocal(): Boolean = accessToken == "0"

    @Throws(IOException::class)
    fun save() {
        FileUtils.ensureParentDirectory(mSaveLocation)
        JSONUtils.writeToFile(mSaveLocation, this)
    }

    fun reload(): Account? {
        return try {
            val account = JSONUtils.readFromFile(mSaveLocation, Account::class.java) ?: return null
            account.mSaveLocation = mSaveLocation
            account
        } catch (_: IOException) {
            null
        } catch (_: JsonParseException) {
            null
        }
    }

    fun getSkinFace(): Bitmap? {
        if (isLocal()) return null
        val skinFaceFile = getSkinFaceFile()
        if (!skinFaceFile.exists()) return null
        if (mFaceCache == null) {
            mFaceCache = BitmapFactory.decodeFile(skinFaceFile.absolutePath)
        }
        return mFaceCache
    }

    private fun getSkinFaceFile(): File {
        return File(Tools.DIR_CACHE, "skin-face-$profileId-${authType.name}.webp")
    }
}
