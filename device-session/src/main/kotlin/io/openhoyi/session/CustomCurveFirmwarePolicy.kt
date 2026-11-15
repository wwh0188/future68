package io.openhoyi.session

import io.openhoyi.protocol.CustomCurveStartPolicy
import io.openhoyi.protocol.StartParameters

/** Exact official-client compatibility profiles, not hardware acceptance. Persistent
 * writes additionally require their independent parameter and transaction guards. */
object CustomCurveFirmwarePolicy {
    fun requiresReviewedFlowProfile(parameters: StartParameters): Boolean =
        parameters.variableFlowLogic && (!parameters.pressureLogic || parameters.firstSegmentFlowMode)

    fun isPressureFollow(parameters: StartParameters): Boolean = parameters.pressureLogic && !parameters.variableFlowLogic

    fun permits(parameters: StartParameters, firmwareInteger: Int): Boolean =
        CoffeeFirmwareProfiles.permitsControl(firmwareInteger)

    fun permits(parameters: StartParameters, firmware: CoffeeFirmware?): Boolean =
        firmware != null && permits(parameters, (firmware.major shl 8) or (firmware.minor shl 4) or firmware.patch)

    /** Mode/version gate only; dispatch still validates the curve and transaction owner. */
    fun permitsStoredRecipe(parameters: StartParameters, firmwareInteger: Int): Boolean =
        permits(parameters, firmwareInteger) && (!isPressureFollow(parameters) || CustomCurveStartPolicy.permits(parameters))

    fun permitsStoredRecipe(parameters: StartParameters, firmware: CoffeeFirmware?): Boolean =
        firmware != null && permitsStoredRecipe(parameters, (firmware.major shl 8) or (firmware.minor shl 4) or firmware.patch)
}
