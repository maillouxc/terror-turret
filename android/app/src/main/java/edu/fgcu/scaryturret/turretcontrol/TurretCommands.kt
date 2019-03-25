package edu.fgcu.scaryturret.turretcontrol

object TurretCommands {

    // These commands need no arguments
    const val CMD_SAFETY_ON = "SAFETY ON"
    const val CMD_SAFETY_OFF = "SAFETY OFF"
    const val CMD_FIRE = "FIRE"
    const val CMD_CEASE_FIRE = "CEASE FIRE"

    // Don't forget to provided the needed arguments to these commands!
    const val CMD_ROTATE_FORMAT = "ROTATE SPEED %d"
    const val CMD_PITCH_FORMAT = "PITCH SPEED %d"

}
