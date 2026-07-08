package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

import java.util.regex.Pattern

class LocalLoginFragment : Fragment(R.layout.fragment_local_login) {
    companion object {
        const val TAG = "LOCAL_LOGIN_FRAGMENT"
    }

    private val mUsernameValidationPattern = Pattern.compile("^[a-zA-Z0-9_]*$")
    private var mUsernameEditText: EditText? = null

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        mUsernameEditText = view.findViewById(R.id.login_edit_email)
        view.findViewById<View>(R.id.login_button).setOnClickListener { v ->
            if (!checkEditText()) {
                val context = v.context
                Tools.dialog(context, context.getString(R.string.local_login_bad_username_title), context.getString(R.string.local_login_bad_username_text))
                return@setOnClickListener
            }

            ExtraCore.setValue(ExtraConstants.MOJANG_LOGIN_TODO, arrayOf(
                mUsernameEditText!!.text.toString(), ""
            ))

            Tools.swapFragment(requireActivity(), MainMenuFragment::class.java, MainMenuFragment.TAG, null)
        }
    }

    private fun checkEditText(): Boolean {
        val text = mUsernameEditText!!.text.toString()
        val matcher = mUsernameValidationPattern.matcher(text)
        return !(text.isEmpty()
                || text.length < 3
                || text.length > 16
                || !matcher.find())
    }
}
