package io.openhoyi.session

import io.openhoyi.protocol.StartParameters

/** Explicit protocol profiles only; static parity is not hardware acceptance. */
object CustomCurveFirmwarePolicy {
    fun requiresReviewedFlowProfile(parameters: StartParameters): Boolean =
        parameters.variableFlowLogic && (!parameters.pressureLogic || parameters.firstSegmentFlowMode)

    fun permits(parameters: StartParameters, firmwareInteger: Int): Boolean =
        CoffeeFirmwareProfiles.permitsControl(firmwareInteger) &&
            (!requiresReviewedFlowProfile(parameters) || firmwareInteger in setOf(
                CoffeeFirmwareProfiles.FW_1_1_4, CoffeeFirmwareProfiles.FW_1_1_5))

    fun permits(parameters: StartParameters, firmware: CoffeeFirmware?): Boolean =
        firmware != null && permits(parameters, (firmware.major shl 8) or (firmware.minor shl 4) or firmware.patch)
}
