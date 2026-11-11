package io.openhoyi.session

import io.openhoyi.protocol.StartParameters

/** Protocol evidence only; firmware membership does not imply hardware acceptance. */
object CustomCurveFirmwarePolicy {
    fun requires114(parameters: StartParameters): Boolean =
        parameters.variableFlowLogic && (!parameters.pressureLogic || parameters.firstSegmentFlowMode)

    fun permits(parameters: StartParameters, firmwareInteger: Int): Boolean =
        CoffeeFirmwareProfiles.permitsControl(firmwareInteger) &&
            (!requires114(parameters) || firmwareInteger == CoffeeFirmwareProfiles.FW_1_1_4)
}
