package net.kdt.pojavlaunch.fragments

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad
import net.kdt.pojavlaunch.customcontrols.gamepad.GamepadMapperAdapter

import fr.spse.gamepad_remapper.RemapperManager
import fr.spse.gamepad_remapper.RemapperView

class GamepadMapperFragment : Fragment(R.layout.fragment_controller_remapper),
    View.OnKeyListener, View.OnGenericMotionListener, AdapterView.OnItemSelectedListener {
    companion object {
        const val TAG = "GamepadMapperFragment"
    }

    private val mRemapperViewBuilder = RemapperView.Builder(null)
        .remapA(true)
        .remapB(true)
        .remapX(true)
        .remapY(true)
        .remapLeftJoystick(true)
        .remapRightJoystick(true)
        .remapStart(true)
        .remapSelect(true)
        .remapLeftShoulder(true)
        .remapRightShoulder(true)
        .remapLeftTrigger(true)
        .remapRightTrigger(true)
        .remapDpad(true)
    private val mExitHandler = Handler(Looper.getMainLooper())
    private val mExitRunnable = Runnable {
        val activity = activity
        if (activity == null) return@Runnable
        activity.onBackPressed()
    }
    private var mInputManager: RemapperManager? = null
    private var mMapperAdapter: GamepadMapperAdapter? = null
    private var mGamepad: Gamepad? = null

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val buttonRecyclerView: RecyclerView = view.findViewById(R.id.gamepad_remapper_recycler)
        mMapperAdapter = GamepadMapperAdapter(view.context)
        buttonRecyclerView.layoutManager = LinearLayoutManager(view.context)
        buttonRecyclerView.adapter = mMapperAdapter
        buttonRecyclerView.setOnKeyListener(this)
        buttonRecyclerView.setOnGenericMotionListener(this)
        buttonRecyclerView.requestFocus()
        mInputManager = RemapperManager(view.context, mRemapperViewBuilder)
        val grabStateSpinner: Spinner = view.findViewById(R.id.gamepad_remapper_mode_spinner)
        val mGrabStateAdapter = ArrayAdapter<String>(view.context, R.layout.support_simple_spinner_dropdown_item)
        mGrabStateAdapter.addAll(getString(R.string.customctrl_visibility_in_menus), getString(R.string.customctrl_visibility_ingame))
        grabStateSpinner.adapter = mGrabStateAdapter
        grabStateSpinner.setSelection(0)
        grabStateSpinner.onItemSelectedListener = this
    }

    private fun createGamepad(inputDevice: InputDevice) {
        mGamepad = object : Gamepad(inputDevice, mMapperAdapter, null) {
            override fun handleGamepadInput(keycode: Int, value: Float) {
                if (keycode == KeyEvent.KEYCODE_BUTTON_SELECT) {
                    handleExitButton(value > 0.5f)
                }
                super.handleGamepadInput(keycode, value)
            }
        }
    }

    private fun handleExitButton(isPressed: Boolean) {
        if (isPressed) mExitHandler.postDelayed(mExitRunnable, 400)
        else mExitHandler.removeCallbacks(mExitRunnable)
    }

    override fun onKey(view: View, i: Int, keyEvent: KeyEvent): Boolean {
        val mainView = view
        if (!Gamepad.isGamepadEvent(keyEvent) || mainView == null) return false
        if (mGamepad == null) createGamepad(keyEvent.device!!)
        mInputManager!!.handleKeyEventInput(mainView.context, keyEvent, mGamepad!!)
        return true
    }

    override fun onGenericMotion(view: View, motionEvent: MotionEvent): Boolean {
        val mainView = view
        if (!Gamepad.isGamepadEvent(motionEvent) || mainView == null) return false
        if (mGamepad == null) createGamepad(motionEvent.device!!)
        mInputManager!!.handleMotionEventInput(mainView.context, motionEvent, mGamepad!!)
        return true
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        val grab = i == 1
        mMapperAdapter!!.setGrabState(grab)
    }

    override fun onNothingSelected(adapterView: AdapterView<*>) {
    }
}
