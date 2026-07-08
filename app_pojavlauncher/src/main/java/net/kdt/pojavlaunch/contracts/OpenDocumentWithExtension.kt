package net.kdt.pojavlaunch.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import net.kdt.pojavlaunch.PojavApplication
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class OpenDocumentWithExtension(extension: String) :
    ActivityResultContract<Any, Uri>() {

    private val extensionMimeTypeFuture: Future<String> = PojavApplication.sExecutorService.submit {
        val extensionMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        extensionMimeType ?: "*/*"
    }

    @NonNull
    override fun createIntent(@NonNull context: Context, @NonNull input: Any): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            intent.type = extensionMimeTypeFuture.get()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        }
        return intent
    }

    @Nullable
    override fun getSynchronousResult(
        @NonNull context: Context,
        @NonNull input: Any
    ): SynchronousResult<Uri>? = null

    @Nullable
    override fun parseResult(resultCode: Int, @Nullable intent: Intent?): Uri? {
        if (intent == null || resultCode != Activity.RESULT_OK) return null
        return intent.data
    }
}
