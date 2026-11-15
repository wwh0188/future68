package io.openhoyi.mobile

import android.content.Context
import io.openhoyi.session.CustomCurveFirmwarePolicy

/** Explain draft capability using the same complete parameters as dispatch authorization. */
internal fun Context.profileExecutionSummary(doc: CustomCurveDocument): String {
    val parameters = CustomCurveAdapter.profile(doc, false)?.parameters
    return getString(when {
        parameters == null -> R.string.curve_modes_execution
        CustomCurveFirmwarePolicy.isPressureFollow(parameters) -> R.string.follow_firmware_required
        CustomCurveFirmwarePolicy.requiresReviewedFlowProfile(parameters) -> R.string.flow_editor_firmware_required
        else -> R.string.curve_modes_compatible
    })
}
