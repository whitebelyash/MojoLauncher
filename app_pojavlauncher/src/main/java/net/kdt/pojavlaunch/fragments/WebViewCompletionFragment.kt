package net.kdt.pojavlaunch.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.annotation.NonNull
import androidx.fragment.app.Fragment

import git.artdeell.mojo.R

abstract class WebViewCompletionFragment : Fragment {
    private val mTrackedUrl: String
    private val mAuthUrl: String
    private var mWebview: WebView? = null
    private var mBlankClient = true
    private var mIsCompleted = false

    protected constructor(mTrackedUrl: String, mAuthUrl: String) {
        this.mTrackedUrl = mTrackedUrl
        this.mAuthUrl = mAuthUrl
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mWebview = inflater.inflate(R.layout.fragment_microsoft_login, container, false) as WebView
        setWebViewSettings()
        if (savedInstanceState == null) startNewSession()
        else restoreWebViewState(savedInstanceState)
        return mWebview
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebViewSettings() {
        val settings = mWebview!!.settings
        settings.javaScriptEnabled = true
        mWebview!!.webViewClient = WebViewTrackClient()
        mBlankClient = false
    }

    private fun startNewSession() {
        CookieManager.getInstance().removeAllCookies {
            mWebview!!.clearHistory()
            mWebview!!.clearCache(true)
            mWebview!!.clearFormData()
            mWebview!!.clearHistory()
            mWebview!!.loadUrl(mAuthUrl)
        }
    }

    private fun restoreWebViewState(savedInstanceState: Bundle) {
        Log.i("MSAuthFragment", "Restoring state...")
        if (mWebview!!.restoreState(savedInstanceState) == null) {
            Log.w("MSAuthFragment", "Failed to restore state, starting afresh")
            startNewSession()
        }
    }

    override fun onStart() {
        super.onStart()
        if (mBlankClient) mWebview!!.webViewClient = WebViewTrackClient()
    }

    override fun onSaveInstanceState(@NonNull outState: Bundle) {
        mWebview!!.webViewClient = WebViewClient()
        mBlankClient = true
        super.onSaveInstanceState(outState)
        mWebview!!.saveState(outState)
    }

    fun canGoBack(): Boolean = mWebview!!.canGoBack()
    fun goBack() { mWebview!!.goBack() }

    inner class WebViewTrackClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith(mTrackedUrl)) {
                internalSignalCompletion(url)
                return true
            }
            return super.shouldOverrideUrlLoading(view, url)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {}

        override fun onPageFinished(view: WebView, url: String) {
            if (url.startsWith(mTrackedUrl)) {
                internalSignalCompletion(url)
            }
        }
    }

    private fun internalSignalCompletion(fullUrl: String) {
        if (mIsCompleted) return
        mIsCompleted = true
        signalCompletion(fullUrl)
    }

    protected abstract fun signalCompletion(fullUrl: String)
}
