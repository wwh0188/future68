package io.openhoyi.mobile

internal object CustomCurveFingerprint {
    /** Only recipe parameters, not the editable display name or live scale connection. */
    fun of(document: CustomCurveDocument): String {
        document.validate()
        val normalized = buildString {
            append("custom-curve-parameters-v1|")
            append(document.temperatureC).append('|')
            append(document.targetHundredthsGram).append('|')
            append(document.controlMode.name).append('|')
            append(document.stages.size).append('|')
            document.stages.forEach { append(it.target).append(',').append(it.waterTenthsMl).append(';') }
            if (document.pressureLogic || document.firstSegmentFlowMode || document.preinfusionSeconds != 0 || document.firstDurationSeconds != 0) {
                append("|logic=").append(document.pressureLogic).append(',').append(document.firstSegmentFlowMode)
                    .append(',').append(document.preinfusionSeconds).append(',').append(document.firstDurationSeconds)
            }
            if (document.compensationHundredthsGram != 0 || document.inactiveStages.any { it.target != 0 || it.waterTenthsMl != 0 }) {
                append("|fidelity-v4=").append(document.compensationHundredthsGram)
                document.allStages.drop(document.stages.size).forEach { append(';').append(it.target).append(',').append(it.waterTenthsMl) }
            }
            // Keep historical schema-1 fingerprints stable until an explicit ceiling is saved.
            document.totalWaterLimitMl?.let { append("|totalWaterLimitMl=").append(it) }
        }
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
