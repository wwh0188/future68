package io.openhoyi.mobile

import io.openhoyi.protocol.StartParameters
import io.openhoyi.protocol.CustomCurveStartPolicy

/** Lossless recipe mapping. A profile alone never authorizes a device command. */
object CustomCurveAdapter {
    fun profile(document: CustomCurveDocument, scaleConnected: Boolean, slot: Int = 7): CurveProfile? {
        try { document.validate() } catch (_: IllegalArgumentException) { return null }
        if (document.controlMode == CustomCurveDocument.ControlMode.FLOW_RAW ||
            (slot !in 1..5 && slot != 7) || document.targetHundredthsGram % 10 != 0 ||
            document.stages.any { it.waterTenthsMl % 10 != 0 }) return null
        val stages = document.stages.toList()
        val totalMl = document.totalWaterLimitMl ?: stages.sumOf { it.waterTenthsMl / 10 }
        val maximumMl = if (scaleConnected && document.targetHundredthsGram > 0)
            maxOf(totalMl, (document.targetHundredthsGram + 24) / 25) else totalMl
        val targets = stages.map { it.target } + List(4 - stages.size) { 0 }
        val water = stages.map { it.waterTenthsMl } + List(4 - stages.size) { 0 }
        val parameters = StartParameters(document.pressureLogic, document.controlMode == CustomCurveDocument.ControlMode.FLOW,
            stages.size, slot, document.temperatureC, maximumMl, document.firstSegmentFlowMode, document.preinfusionSeconds,
            targets[0], targets[1], targets[2], targets[3], water[0], document.firstDurationSeconds, water[1], water[2], water[3])
        if (!CustomCurveStartPolicy.permits(parameters)) return null
        return CurveProfile(document.id, document.name, "", if (scaleConnected) document.targetHundredthsGram else 0,
            parameters, scaleMode = scaleConnected)
    }
}

/** Compatibility entry point retaining the old pressure-only contract. */
object CustomPressureCurveAdapter {
    fun profile(document: CustomCurveDocument, scaleConnected: Boolean, slot: Int = 7): CurveProfile? =
        if (document.controlMode == CustomCurveDocument.ControlMode.PRESSURE && !document.pressureLogic &&
            !document.firstSegmentFlowMode && document.preinfusionSeconds == 0 && document.firstDurationSeconds == 0)
            CustomCurveAdapter.profile(document, scaleConnected, slot) else null
}
