package com.kdt.mcgui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.BackgroundLogin
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.authenticator.impl.PresentedException
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper

import java.io.IOException
import java.util.HashMap
import java.util.Objects

import fr.spse.extended_view.ExtendedTextView
import git.artdeell.mojo.R

class AccountSpinner : AppCompatSpinner, LoginListener, AdapterView.OnItemSelectedListener, ValueAnimator.AnimatorUpdateListener {
    private var mAdapter: Adapter? = null
    private var mMaxSteps = 5
    private val mLoginStepAnimator = ValueAnimator.ofFloat(mMaxSteps.toFloat())
    private val mLoginBarPaint = Paint()
    private var mLoginStep: Float = mMaxSteps.toFloat()

    private inner class LoginExtraListener(private val mAuthType: AuthType) : ExtraListener<String> {
        override fun onValueSet(key: String, value: String): Boolean {
            mLoginBarPaint.color = resources.getColor(R.color.minebutton_color)
            val backgroundLogin = mAuthType.createAuth()
            backgroundLogin.createAccount(this@AccountSpinner, value)
            return false
        }
    }

    private val mMicrosoftLoginListener = LoginExtraListener(AuthType.MICROSOFT)
    private val mElyByLoginListener = LoginExtraListener(AuthType.ELY_BY)

    private val mMojangLoginListener = ExtraListener<String?> { _, value ->
        try {
            val account = Accounts.create { acc -> acc.username = value!! }
            onLoginDone(account)
        } catch (e: IOException) {
            onLoginError(e)
        }
        false
    }

    private val mRefreshAccountsListener = ExtraListener<Boolean> { k, v ->
        reload()
        false
    }

    constructor(@NonNull context: Context, mode: Int) : super(context, mode) { init() }
    constructor(@NonNull context: Context) : super(context) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?, defStyleAttr: Int, mode: Int) : super(context, attrs, defStyleAttr, mode) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?, defStyleAttr: Int, mode: Int, popupTheme: Resources.Theme?) : super(context, attrs, defStyleAttr, mode, popupTheme) { init() }

    private fun init() {
        mAdapter = Adapter(context)
        setAdapter(mAdapter)
        onItemSelectedListener = this
        reload()

        setBackgroundColor(resources.getColor(R.color.background_status_bar))
        mLoginBarPaint.color = resources.getColor(R.color.minebutton_color)
        mLoginBarPaint.strokeWidth = resources.getDimensionPixelOffset(R.dimen._2sdp).toFloat()
        mLoginStepAnimator.addUpdateListener(this)
        mLoginStep = mMaxSteps.toFloat()

        ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mMojangLoginListener)
        ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, mMicrosoftLoginListener)
        ExtraCore.addExtraListener(ExtraConstants.ELYBY_LOGIN_TODO, mElyByLoginListener)
        ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, mRefreshAccountsListener)
    }

    private fun reload() {
        PojavApplication.sExecutorService.execute {
            try {
                val accounts = Accounts.load()
                Tools.runOnUiThread { refresh(accounts) }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    private fun refresh(accounts: Accounts) {
        mAdapter!!.setNotifyOnChange(false)
        mAdapter!!.clear()
        mAdapter!!.add(null)
        mAdapter!!.setNotifyOnChange(true)
        mAdapter!!.addAll(accounts.accounts)

        if (accounts.accounts.isEmpty()) {
            setSelection(0)
        } else {
            setSelection(accounts.selectionIndex + 1)
            refreshAccount(Objects.requireNonNull(selectedItem as Account))
        }
    }

    private fun refreshAccount(account: Account) {
        ProgressKeeper.waitUntilDone {
            val refreshAccount = account.reload() ?: return@waitUntilDone
            val authType = refreshAccount.authType
            if (authType.requiresLogin() && System.currentTimeMillis() > refreshAccount.expiresAt) {
                authType.createAuth().refreshAccount(this@AccountSpinner, refreshAccount)
            }
        }
    }

    private fun dismissPopup() {
        onDetachedFromWindow()
        onAttachedToWindow()
    }

    private fun createAccount() {
        setSelection(0)
        dismissPopup()
        ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true)
    }

    override fun onDraw(@NonNull canvas: Canvas) {
        super.onDraw(canvas)
        val bottom = height - mLoginBarPaint.strokeWidth / 2f
        val lineFillPercent = mLoginStep / mMaxSteps
        canvas.drawLine(0f, bottom, lineFillPercent * width, bottom, mLoginBarPaint)
    }

    override fun onLoginDone(account: Account) {
        mLoginStep = mMaxSteps.toFloat()
        invalidate()
        Toast.makeText(context, R.string.main_login_done, Toast.LENGTH_SHORT).show()
        Accounts.setCurrent(account)
        reload()
    }

    override fun onLoginError(errorMessage: Throwable) {
        mLoginBarPaint.color = Color.RED
        invalidate()

        val context = context
        if (context !is Activity) return
        if (context is LifecycleOwner) {
            val lifecycleOwner = context as LifecycleOwner
            val state = lifecycleOwner.lifecycle.currentState
            if (state != Lifecycle.State.RESUMED) return
        }

        if (errorMessage is PresentedException) {
            val exception = errorMessage as PresentedException
            val cause = exception.cause
            if (cause == null) {
                Tools.dialog(context, context.getString(R.string.global_error), exception.toString(context))
            } else {
                Tools.showError(context, exception.toString(context), exception.cause!!)
            }
        } else {
            Tools.showError(context, errorMessage)
        }
    }

    override fun onLoginProgress(step: Int) {
        mLoginStepAnimator.cancel()
        mLoginStepAnimator.setFloatValues(mLoginStep, step.toFloat())
        mLoginStepAnimator.start()
    }

    override fun setMaxLoginProgress(max: Int) {
        mMaxSteps = max
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        val account = mAdapter!!.getItem(i)
        if (account == null) {
            if (i == 0) {
                createAccount()
            } else {
                Tools.showError(adapterView!!.context, NullPointerException())
            }
            return
        }
        Accounts.setCurrent(account)
        refreshAccount(account)
        dismissPopup()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}

    override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
        mLoginStep = valueAnimator.animatedValue as Float
        invalidate()
    }

    private inner class Adapter(context: Context) : ArrayAdapter<Account>(context, R.layout.item_account) {
        private val mSkinHeadCache = HashMap<Int, BitmapDrawable>()
        private val mInflater = LayoutInflater.from(context)

        @NonNull
        override fun getView(position: Int, @Nullable convertView: View?, @NonNull parent: ViewGroup): View {
            val view = convertView ?: mInflater.inflate(R.layout.item_account, parent, false)
            populateView(view, position, false)
            return view
        }

        override fun getDropDownView(position: Int, @Nullable convertView: View?, @NonNull parent: ViewGroup): View {
            val view = convertView ?: mInflater.inflate(R.layout.item_account, parent, false)
            populateView(view, position, true)
            return view
        }

        private fun populateView(view: View, position: Int, isDropDown: Boolean) {
            val resources = resources
            val theme = context.theme

            val textview = view.findViewById<ExtendedTextView>(R.id.account_item)
            val deleteButton = view.findViewById<ImageView>(R.id.delete_account_button)

            if (position == 0) {
                val plusDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_add, theme)
                textview.setCompoundDrawables(plusDrawable, null, null, null)
                textview.setText(R.string.main_add_account)
                deleteButton.visibility = View.GONE
                if (isDropDown || count == 1) view.setOnClickListener { createAccount() }
                return
            }

            if (isDropDown) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener { showDeleteDialog(it.context, position) }
            } else {
                deleteButton.visibility = View.GONE
            }

            val account = getItem(position) ?: return

            val authTypeResource = account.authType.iconResource
            val authType: Drawable? = if (authTypeResource != 0) {
                ResourcesCompat.getDrawable(resources, authTypeResource, theme)
            } else null

            val headCacheHash = System.identityHashCode(account)
            var accountHead: BitmapDrawable? = mSkinHeadCache[headCacheHash]
            if (accountHead == null) {
                val accountSkinFace = account.getSkinFace()
                if (accountSkinFace != null) {
                    accountHead = BitmapDrawable(resources, accountSkinFace)
                    mSkinHeadCache[headCacheHash] = accountHead
                }
            }

            textview.text = account.username
            textview.setCompoundDrawablesRelative(accountHead, null, authType, null)
        }

        private fun showDeleteDialog(context: Context, position: Int) {
            AlertDialog.Builder(context)
                .setMessage(R.string.warning_remove_account)
                .setPositiveButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.global_delete) { _, _ ->
                    val account = getItem(position)
                    Accounts.delete(account)
                    reload()
                }
                .show()
        }
    }
}
