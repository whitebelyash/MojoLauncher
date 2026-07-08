package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.net.Uri
import android.widget.Toast

import androidx.fragment.app.FragmentActivity

import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraCore

import git.artdeell.mojo.R

class OAuthFragment : WebViewCompletionFragment {
    private val mExtraCoreConstant: String

    constructor(mTrackedUrl: String, mAuthUrl: String, mExtraCoreConstant: String) : super(mTrackedUrl, mAuthUrl) {
        this.mExtraCoreConstant = mExtraCoreConstant
    }

    private fun displayError(context: Context, uri: Uri) {
        var errorMessage = uri.getQueryParameter(QUERY_ERROR_DECRIPTION)
        if (errorMessage == null) errorMessage = uri.getQueryParameter(QUERY_ERROR_NAME)
        if (errorMessage == null) errorMessage = getString(R.string.oauth_unknown_error)
        Tools.dialog(context, getString(R.string.global_error), errorMessage)
    }

    override fun signalCompletion(fullUrl: String) {
        val activity = activity
        if (activity == null) return
        val uri = Uri.parse(fullUrl)
        val error = uri.getQueryParameter(QUERY_ERROR_NAME)
        val code = uri.getQueryParameter(QUERY_OAUTH_CODE)
        if (code == null) {
            activity.onBackPressed()
            if (ERROR_ACCESS_DENIED == error) return
            displayError(activity, uri)
            return
        }
        ExtraCore.setValue(mExtraCoreConstant, code)
        Toast.makeText(activity, R.string.oauth_web_complete, Toast.LENGTH_SHORT).show()
        Tools.backToMainMenu(activity)
    }

    companion object {
        private const val QUERY_ERROR_NAME = "error"
        private const val QUERY_ERROR_DECRIPTION = "error_description"
        private const val QUERY_OAUTH_CODE = "code"
        private const val ERROR_ACCESS_DENIED = "access_denied"
    }
}
