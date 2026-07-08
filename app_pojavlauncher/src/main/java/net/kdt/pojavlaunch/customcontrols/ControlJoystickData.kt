package net.kdt.pojavlaunch.customcontrols

class ControlJoystickData : ControlData {
    var forwardLock = false
    var absolute = false

    constructor() : super()

    constructor(properties: ControlJoystickData) : super(properties) {
        forwardLock = properties.forwardLock
        absolute = properties.absolute
    }
}
