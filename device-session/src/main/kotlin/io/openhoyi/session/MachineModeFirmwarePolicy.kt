package io.openhoyi.session

import io.openhoyi.protocol.MachineSettingChange

/** Exact compatibility profiles for the independent global slow-flow command (0x03).
 * This does not authorize full slow-flow recipes or claim hardware acceptance. */
object MachineModeFirmwarePolicy {
    fun permits(change: MachineSettingChange.LeverMode, firmwareInteger: Int): Boolean =
        !change.flow || change.pressure || CoffeeFirmwareProfiles.permitsControl(firmwareInteger)

    fun permits(change: MachineSettingChange.LeverMode, firmware: CoffeeFirmware?): Boolean =
        permits(change, firmware?.let { (it.major shl 8) or (it.minor shl 4) or it.patch } ?: -1)
}
