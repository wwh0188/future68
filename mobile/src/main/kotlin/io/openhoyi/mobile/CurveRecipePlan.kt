package io.openhoyi.mobile

/** Read-only metadata for a library preview. Water is an amount, never a flow target. */
internal data class CurveRecipePlan(
    val preview: CurveStageParameters.Preview,
    val stageWaterTenthsMl: List<Int>,
    val totalWaterLimitMl: Int?,
    val preinfusionSeconds: Int?,
    val firstStageDurationSettingSeconds: Int?,
    val endMode: String?,
    val targetWeightHundredthsGram: Int?,
    val advice: String?,
    val compensationHundredthsGram: Int = 0,
    val inactiveStages: List<CustomCurveDocument.Stage> = emptyList(),
    val inactiveStageUnits: List<CurveStageParameters.Unit> = emptyList(),
) {
    val assignedWaterTenthsMl: Int get() = stageWaterTenthsMl.sum()

    private fun withInactiveStageUnits(unitAt: (Int) -> CurveStageParameters.Unit): CurveRecipePlan =
        copy(inactiveStageUnits = inactiveStages.indices.map { unitAt(preview.stages.size + it) })

    companion object {
        fun from(item: CurveLibraryItem): CurveRecipePlan {
            val preview = CurveStageParameters.preview(item)
            item.factoryCurve?.let { curve ->
                return CurveRecipePlan(preview, curve.segmentFlowMl.take(curve.segmentCount).map { it * 10 },
                    curve.flowMl, curve.preinfusionSeconds, curve.firstDurationSeconds,
                    null, curve.weightTenthsGram * 10, curve.tips, curve.weightCompensationTenthsGram * 10,
                    (curve.segmentCount until 4).map { CustomCurveDocument.Stage(curve.targets[it], curve.segmentFlowMl[it] * 10) }.withoutEmptyTail()).withInactiveStageUnits { index ->
                        if (curve.variableFlowLogic && index > 0) CurveStageParameters.Unit.FLOW_ML_PER_SECOND
                        else CurveStageParameters.Unit.PRESSURE_BAR
                    }
            }
            item.controlProfile?.let { profile ->
                val p = profile.parameters
                return CurveRecipePlan(preview,
                    listOf(p.firstFlowTenths, p.secondFlowTenths, p.thirdFlowTenths, p.fourthFlowTenths)
                        .take(p.segmentCount), p.maximumWaterMl, p.preinfusionSeconds, p.firstDurationSeconds,
                    profile.endMode, profile.targetHundredthsGram, null, profile.compensationHundredthsGram,
                    (p.segmentCount until 4).map { CustomCurveDocument.Stage(
                        listOf(p.target1, p.target2, p.target3, p.target4)[it],
                        listOf(p.firstFlowTenths, p.secondFlowTenths, p.thirdFlowTenths, p.fourthFlowTenths)[it]) }.withoutEmptyTail()).withInactiveStageUnits { index ->
                        if (p.variableFlowLogic && (index > 0 || p.firstSegmentFlowMode)) CurveStageParameters.Unit.FLOW_ML_PER_SECOND
                        else CurveStageParameters.Unit.PRESSURE_BAR
                    }
            }
            item.customDocument?.let { document ->
                return CurveRecipePlan(preview, document.stages.map { it.waterTenthsMl }, document.totalWaterLimitMl ?:
                    document.assignedWaterTenthsMl.takeIf { document.controlMode != CustomCurveDocument.ControlMode.FLOW_RAW && it % 10 == 0 }?.div(10),
                    document.preinfusionSeconds.takeUnless { document.controlMode == CustomCurveDocument.ControlMode.FLOW_RAW },
                    document.firstDurationSeconds.takeUnless { document.controlMode == CustomCurveDocument.ControlMode.FLOW_RAW },
                    null, document.targetHundredthsGram, document.description.takeIf { it.isNotBlank() }, document.compensationHundredthsGram, document.inactiveStages).withInactiveStageUnits { index ->
                        when {
                            document.controlMode == CustomCurveDocument.ControlMode.FLOW_RAW -> CurveStageParameters.Unit.RAW_UNKNOWN
                            document.isFlowStage(index) -> CurveStageParameters.Unit.FLOW_ML_PER_SECOND
                            else -> CurveStageParameters.Unit.PRESSURE_BAR
                        }
                    }
            }
            return CurveRecipePlan(preview, emptyList(), null, null, null, null, null, null)
        }
    }
}
