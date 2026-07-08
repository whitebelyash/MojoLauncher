package net.kdt.pojavlaunch.customcontrols.gamepad

import net.kdt.pojavlaunch.LwjglGlfwKeycode

class GamepadMap {
    companion object {
        const val MOUSE_SCROLL_DOWN: Short = -1
        const val MOUSE_SCROLL_UP: Short = -2
        const val MOUSE_LEFT: Short = -3
        const val MOUSE_MIDDLE: Short = -4
        const val MOUSE_RIGHT: Short = -5
        const val UNSPECIFIED: Short = -6

        fun getDefaultGameMap(): GamepadMap {
            val gameMap = createEmptyMap()

            gameMap.BUTTON_A.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_SPACE.toShort()
            gameMap.BUTTON_B.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_Q.toShort()
            gameMap.BUTTON_X.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_E.toShort()
            gameMap.BUTTON_Y.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_F.toShort()

            gameMap.DIRECTION_FORWARD.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_W.toShort()
            gameMap.DIRECTION_BACKWARD.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_S.toShort()
            gameMap.DIRECTION_RIGHT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_D.toShort()
            gameMap.DIRECTION_LEFT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_A.toShort()

            gameMap.DPAD_UP.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toShort()
            gameMap.DPAD_DOWN.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_O.toShort()
            gameMap.DPAD_RIGHT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_K.toShort()
            gameMap.DPAD_LEFT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_J.toShort()

            gameMap.SHOULDER_LEFT.keycodes[0] = MOUSE_SCROLL_UP
            gameMap.SHOULDER_RIGHT.keycodes[0] = MOUSE_SCROLL_DOWN

            gameMap.TRIGGER_LEFT.keycodes[0] = MOUSE_RIGHT
            gameMap.TRIGGER_RIGHT.keycodes[0] = MOUSE_LEFT

            gameMap.THUMBSTICK_LEFT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL.toShort()
            gameMap.THUMBSTICK_RIGHT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toShort()
            gameMap.THUMBSTICK_RIGHT.isToggleable = true

            gameMap.BUTTON_START.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toShort()
            gameMap.BUTTON_SELECT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_TAB.toShort()

            return gameMap
        }

        fun getDefaultMenuMap(): GamepadMap {
            val menuMap = createEmptyMap()

            menuMap.BUTTON_A.keycodes[0] = MOUSE_LEFT
            menuMap.BUTTON_B.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toShort()
            menuMap.BUTTON_X.keycodes[0] = MOUSE_RIGHT
            {
                val keycodes = menuMap.BUTTON_Y.keycodes
                keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toShort()
                keycodes[1] = MOUSE_RIGHT
            }()

            {
                val keycodes = menuMap.DIRECTION_FORWARD.keycodes
                keycodes[0] = keycodes[1] = keycodes[2] = keycodes[3] = MOUSE_SCROLL_UP
            }()

            {
                val keycodes = menuMap.DIRECTION_BACKWARD.keycodes
                keycodes[0] = keycodes[1] = keycodes[2] = keycodes[3] = MOUSE_SCROLL_DOWN
            }()

            menuMap.DPAD_DOWN.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_O.toShort()
            menuMap.DPAD_RIGHT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_K.toShort()
            menuMap.DPAD_LEFT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_J.toShort()

            menuMap.SHOULDER_LEFT.keycodes[0] = MOUSE_SCROLL_UP
            menuMap.SHOULDER_RIGHT.keycodes[0] = MOUSE_SCROLL_DOWN

            menuMap.BUTTON_SELECT.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toShort()

            return menuMap
        }

        @Suppress("unused")
        fun createEmptyMap(): GamepadMap {
            val emptyMap = createAndInitializeButtons()
            for (button in emptyMap.getButtons()) {
                button.keycodes = shortArrayOf(UNSPECIFIED, UNSPECIFIED, UNSPECIFIED, UNSPECIFIED)
            }
            return emptyMap
        }

        fun getSpecialKeycodeNames(): Array<String> {
            return arrayOf("UNSPECIFIED", "MOUSE_RIGHT", "MOUSE_MIDDLE", "MOUSE_LEFT", "SCROLL_UP", "SCROLL_DOWN")
        }

        private fun createAndInitializeButtons(): GamepadMap {
            val gamepadMap = GamepadMap()
            gamepadMap.BUTTON_A = GamepadButton()
            gamepadMap.BUTTON_B = GamepadButton()
            gamepadMap.BUTTON_X = GamepadButton()
            gamepadMap.BUTTON_Y = GamepadButton()
            gamepadMap.BUTTON_START = GamepadButton()
            gamepadMap.BUTTON_SELECT = GamepadButton()
            gamepadMap.TRIGGER_RIGHT = GamepadButton()
            gamepadMap.TRIGGER_LEFT = GamepadButton()
            gamepadMap.SHOULDER_RIGHT = GamepadButton()
            gamepadMap.SHOULDER_LEFT = GamepadButton()
            gamepadMap.DIRECTION_FORWARD = GamepadEmulatedButton()
            gamepadMap.DIRECTION_BACKWARD = GamepadEmulatedButton()
            gamepadMap.DIRECTION_RIGHT = GamepadEmulatedButton()
            gamepadMap.DIRECTION_LEFT = GamepadEmulatedButton()
            gamepadMap.THUMBSTICK_RIGHT = GamepadButton()
            gamepadMap.THUMBSTICK_LEFT = GamepadButton()
            gamepadMap.DPAD_UP = GamepadButton()
            gamepadMap.DPAD_RIGHT = GamepadButton()
            gamepadMap.DPAD_DOWN = GamepadButton()
            gamepadMap.DPAD_LEFT = GamepadButton()
            return gamepadMap
        }
    }

    lateinit var BUTTON_A: GamepadButton
    lateinit var BUTTON_B: GamepadButton
    lateinit var BUTTON_X: GamepadButton
    lateinit var BUTTON_Y: GamepadButton
    lateinit var BUTTON_START: GamepadButton
    lateinit var BUTTON_SELECT: GamepadButton
    lateinit var TRIGGER_RIGHT: GamepadButton
    lateinit var TRIGGER_LEFT: GamepadButton
    lateinit var SHOULDER_RIGHT: GamepadButton
    lateinit var SHOULDER_LEFT: GamepadButton
    lateinit var THUMBSTICK_RIGHT: GamepadButton
    lateinit var THUMBSTICK_LEFT: GamepadButton
    lateinit var DPAD_UP: GamepadButton
    lateinit var DPAD_DOWN: GamepadButton
    lateinit var DPAD_RIGHT: GamepadButton
    lateinit var DPAD_LEFT: GamepadButton

    lateinit var DIRECTION_FORWARD: GamepadEmulatedButton
    lateinit var DIRECTION_BACKWARD: GamepadEmulatedButton
    lateinit var DIRECTION_RIGHT: GamepadEmulatedButton
    lateinit var DIRECTION_LEFT: GamepadEmulatedButton

    fun resetPressedState() {
        BUTTON_A.resetButtonState()
        BUTTON_B.resetButtonState()
        BUTTON_X.resetButtonState()
        BUTTON_Y.resetButtonState()
        BUTTON_START.resetButtonState()
        BUTTON_SELECT.resetButtonState()
        TRIGGER_LEFT.resetButtonState()
        TRIGGER_RIGHT.resetButtonState()
        SHOULDER_LEFT.resetButtonState()
        SHOULDER_RIGHT.resetButtonState()
        THUMBSTICK_LEFT.resetButtonState()
        THUMBSTICK_RIGHT.resetButtonState()
        DPAD_UP.resetButtonState()
        DPAD_RIGHT.resetButtonState()
        DPAD_DOWN.resetButtonState()
        DPAD_LEFT.resetButtonState()
    }

    fun getButtons(): Array<GamepadEmulatedButton> {
        return arrayOf(
            BUTTON_A, BUTTON_B, BUTTON_X, BUTTON_Y,
            BUTTON_SELECT, BUTTON_START,
            TRIGGER_LEFT, TRIGGER_RIGHT,
            SHOULDER_LEFT, SHOULDER_RIGHT,
            THUMBSTICK_LEFT, THUMBSTICK_RIGHT,
            DPAD_UP, DPAD_RIGHT, DPAD_DOWN, DPAD_LEFT,
            DIRECTION_FORWARD, DIRECTION_BACKWARD,
            DIRECTION_LEFT, DIRECTION_RIGHT
        )
    }
}
