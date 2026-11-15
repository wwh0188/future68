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
        if (p.variableFlowLogic && (p.segmentCount !in 2..4 || p.firstDurationSeconds > 254 ||
                (p.firstFlowTenths == 0 && p.firstDurationSeconds == 0))) return false
        // Temporary follow recipes use the complete reviewed shape. Other private curve
        // modes retain their existing wire ranges and independent cumulative cutoff policy.
        if (p.pressureLogic && !p.variableFlowLogic &&
            (p.slot != 7 || p.segmentCount !in 2..4 || p.firstDurationSeconds > 254)) return false
        val targets = listOf(p.target1, p.target2, p.target3, p.target4)
        val water = listOf(p.firstFlowTenths, p.secondFlowTenths, p.thirdFlowTenths, p.fourthFlowTenths)
        for (index in 0 until p.segmentCount) {
            val range = when {
                p.pressureLogic && !p.variableFlowLogic -> if (index == 0) 5..120 else 10..120
                !p.variableFlowLogic -> 0..120 // Preserve the existing constant-pressure recipe range.
                index == 0 && !p.firstSegmentFlowMode -> 5..100
                else -> 10..100
            }
            val minimumWater = if (p.variableFlowLogic && index == 0) 0 else 10
            val maximumWater = if (p.variableFlowLogic || p.pressureLogic) 9990 else 65530
            if (targets[index] !in range || water[index] !in minimumWater..maximumWater || water[index] % 10 != 0) return false
        }
        if (targets.drop(p.segmentCount).any { it !in 0..255 } || water.drop(p.segmentCount).any { it !in 0..65535 }) return false
        // The cumulative cutoff is independent of the segment allocations in the official APK.
        return p.maximumWaterMl in 1..maxOf(water.take(p.segmentCount).sum() / 10, 800)
    }
}
