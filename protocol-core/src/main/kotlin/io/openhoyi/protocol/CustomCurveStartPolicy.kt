package io.openhoyi.protocol

/** Custom-curve subset compared with original 1.2.6 APK encoding, not hardware acceptance.
 * A host must separately authorize the exact current stored recipe on every dispatch.
 * Legacy FLOW_RAW documents have no implicit authorization through this wire-only policy.
 */
object CustomCurveStartPolicy {
    fun permits(p: StartParameters): Boolean {
        if (p.temperatureC !in 75..105 || p.segmentCount !in 1..4 ||
            (p.slot !in 1..5 && p.slot != 7) || p.preinfusionSeconds !in 0..127 ||
            p.firstDurationSeconds !in 0..255 || (p.firstSegmentFlowMode && !p.variableFlowLogic)) return false
        val targets = listOf(p.target1, p.target2, p.target3, p.target4)
        val water = listOf(p.firstFlowTenths, p.secondFlowTenths, p.thirdFlowTenths, p.fourthFlowTenths)
        for (index in 0 until p.segmentCount) {
            val range = when {
                !p.variableFlowLogic -> 0..120 // Preserve the existing pressure recipe range.
                index == 0 && !p.firstSegmentFlowMode -> 5..100
                else -> 10..100
            }
            if (targets[index] !in range || water[index] !in 10..65530 || water[index] % 10 != 0) return false
        }
        if (targets.drop(p.segmentCount).any { it != 0 } || water.drop(p.segmentCount).any { it != 0 }) return false
        // The cumulative cutoff is independent of the segment allocations in the official APK.
        return p.maximumWaterMl in 1..maxOf(water.sum() / 10, 800)
    }
}
