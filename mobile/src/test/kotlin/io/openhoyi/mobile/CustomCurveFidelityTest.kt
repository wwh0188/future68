package io.openhoyi.mobile

import io.openhoyi.protocol.CustomCurveStartPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CustomCurveFidelityTest {
    private fun curve() = CustomCurveDocument("draft-00000000-0000-4000-8000-000000000001", "Local", 93, 3600,
        CustomCurveDocument.ControlMode.FLOW, listOf(CustomCurveDocument.Stage(30, 150), CustomCurveDocument.Stage(20, 400)),
        40, true, false, 8, 12, "Recipe notes")
    private class Disk : CustomCurveStore.Storage {
        var data: String? = null
        override fun read() = data
        override fun write(value: String) { data = value }
    }

    @Test fun oldSchemaThreeAndFingerprintStayStableIncludingDescriptionEdits() {
        val source = curve()
        assertEquals(3, JSONObject(source.encode()).getInt("schema"))
        assertEquals("47648497eea4907572811be0b1318810bf0502b29e8844a6bbaaf6ddf5198c61", CustomCurveFingerprint.of(source))
        assertEquals(CustomCurveFingerprint.of(source), CustomCurveFingerprint.of(source.copy(name = "Renamed", description = "Other")))
        val disk = Disk(); CustomCurveStore(disk).save(source)
        assertEquals(source, CustomCurveStore(disk).find(source.id))
        // Legacy b1/b0 canonical must remain byte-for-byte stable.
        val bools = JSONArray().put(true).put(false)
        assertEquals(sha("[b1b0]"), CurveDocumentJson.checksum(bools))
        val upstreamShape = JSONObject(source.encode()).put("configuration", JSONObject())
        assertTrue(runCatching { CustomCurveDocument.decode(upstreamShape.toString()) }.isFailure)
    }

    @Test fun schemaFourReopensAndMapsCompensationAndAllFourSlotsWithoutTruncation() {
        val tail = mutableListOf(CustomCurveDocument.Stage(88, 123), CustomCurveDocument.Stage(255, 65535))
        val source = curve().copy(compensationHundredthsGram = -125, inactiveStages = tail)
        val disk = Disk(); val store = CustomCurveStore(disk); store.save(source)
        tail[0] = CustomCurveDocument.Stage(1, 1)
        val saved = requireNotNull(CustomCurveStore(disk).find(source.id))
        assertEquals(4, JSONObject(saved.encode()).getInt("schema"))
        val plan = CurveRecipePlan.from(CurveLibraryItem(saved.id, saved.name, "", "", null, customDocument = saved))
        assertEquals(saved.description, plan.advice)
        assertEquals(saved.compensationHundredthsGram, plan.compensationHundredthsGram)
        assertEquals(saved.inactiveStages, plan.inactiveStages)
        assertEquals(550, plan.assignedWaterTenthsMl)
        assertEquals(-125, saved.compensationHundredthsGram)
        assertEquals(listOf(CustomCurveDocument.Stage(88, 123), CustomCurveDocument.Stage(255, 65535)), saved.inactiveStages)
        val profile = requireNotNull(CustomCurveAdapter.profile(saved, true))
        assertEquals(-125, profile.compensationHundredthsGram)
        assertEquals(88, profile.parameters.target3); assertEquals(255, profile.parameters.target4)
        assertEquals(123, profile.parameters.thirdFlowTenths); assertEquals(65535, profile.parameters.fourthFlowTenths)
        assertNotEquals(CustomCurveFingerprint.of(curve()), CustomCurveFingerprint.of(saved))
        assertNotEquals(CustomCurveFingerprint.of(saved), CustomCurveFingerprint.of(saved.copy(compensationHundredthsGram = -124)))
        val malformed = JSONObject(saved.encode()).put("inactiveStages", JSONArray())
        assertTrue(runCatching { CustomCurveDocument.decode(malformed.toString()) }.isFailure)
    }

    @Test fun profileCopyKeepsCompensationAndInactiveSlotsAndActiveWaterCeiling() {
        val p = requireNotNull(CustomCurveAdapter.profile(curve(), false)).parameters.copy(target3 = 99, thirdFlowTenths = 65000)
        val profile = CurveProfile("source", "Captured", "", 3600, p, -127)
        val copied = CustomCurveDocument.fromLibraryItem(CurveLibraryItem("source", "Captured", "", "", profile))
        assertEquals(-127, copied.compensationHundredthsGram)
        assertEquals(99, copied.allStages[2].target)
        assertEquals(65000, copied.allStages[2].waterTenthsMl)
        assertEquals(800, copied.maximumSupportedWaterLimitMl)
        assertEquals(p, requireNotNull(CustomCurveAdapter.profile(copied, false)).parameters)
    }

    @Test fun fourStageRecipeCanShrinkSaveAndExpandWithoutLosingHiddenInputs() {
        val original = curve().copy(stages = curve().stages + listOf(CustomCurveDocument.Stage(35, 200), CustomCurveDocument.Stage(15, 100)))
        val shortened = original.copy(stages = original.stages.take(2), inactiveStages = original.stages.drop(2))
        val reopened = CustomCurveDocument.decode(shortened.encode())
        val expanded = reopened.copy(stages = reopened.allStages, inactiveStages = emptyList())
        assertEquals(original, expanded)
        assertEquals(requireNotNull(CustomCurveAdapter.profile(original, false)).parameters,
            requireNotNull(CustomCurveAdapter.profile(expanded, false)).parameters)
    }

    @Test fun historicalSingleFlowStageKeepsPressureFirstAndFlowUnitsInHiddenLaterSlots() {
        val historical = curve().copy(stages = listOf(curve().stages.first()),
            inactiveStages = listOf(CustomCurveDocument.Stage(20, 400), CustomCurveDocument.Stage(15, 100), CustomCurveDocument.Stage(0, 0)))
        val plan = CurveRecipePlan.from(CurveLibraryItem(historical.id, historical.name, "", "", null, customDocument = historical))
        assertEquals(CurveStageParameters.Unit.PRESSURE_BAR, plan.preview.stages.single().unit)
        assertEquals(List(3) { CurveStageParameters.Unit.FLOW_ML_PER_SECOND }, plan.inactiveStageUnits)
        assertEquals(historical.inactiveStages, plan.inactiveStages)
        assertEquals(150, plan.assignedWaterTenthsMl)
        val rawPlan = CurveRecipePlan.from(CurveLibraryItem(historical.id, historical.name, "", "", null,
            customDocument = historical.copy(controlMode = CustomCurveDocument.ControlMode.FLOW_RAW)))
        assertEquals(List(3) { CurveStageParameters.Unit.RAW_UNKNOWN }, rawPlan.inactiveStageUnits)
    }

    @Test fun historicalNonExecutableFlowDraftDoesNotMakeWholeStoreUnavailable() {
        val oneStage = curve().copy(stages = listOf(curve().stages.first()), firstDurationSeconds = 255)
        val disk = Disk(); val store = CustomCurveStore(disk)
        store.save(oneStage); store.save(curve().copy(id = "draft-00000000-0000-4000-8000-000000000002"))
        val reopened = CustomCurveStore(disk)
        assertEquals(2, reopened.list().size)
        assertEquals(oneStage, reopened.find(oneStage.id))
        assertEquals(oneStage, CustomCurveDocument.decode(oneStage.encode()))
        assertNull(CustomCurveAdapter.profile(oneStage, false))
        assertNull(CustomCurveAdapter.profile(curve().copy(firstDurationSeconds = 255), false))
        reopened.save(oneStage.copy(description = "Correct later"))
        assertEquals(255, reopened.find(oneStage.id)!!.firstDurationSeconds)
    }

    @Test fun timedFirstStageAllowsZeroWaterButNotZeroTimeOrSentinel255() {
        val timed = curve().copy(stages = listOf(CustomCurveDocument.Stage(10, 0), CustomCurveDocument.Stage(20, 400)), firstSegmentFlowMode = true)
        val p = requireNotNull(CustomCurveAdapter.profile(timed, false)).parameters
        assertTrue(CustomCurveStartPolicy.permits(p))
        assertFalse(CustomCurveStartPolicy.permits(p.copy(firstDurationSeconds = 0)))
        assertFalse(CustomCurveStartPolicy.permits(p.copy(firstDurationSeconds = 255)))
        assertNull(CustomCurveAdapter.profile(timed.copy(firstDurationSeconds = 0), false))
        assertNull(CustomCurveAdapter.profile(timed.copy(firstDurationSeconds = 255), false))
        assertFalse(CustomCurveStartPolicy.permits(p.copy(secondFlowTenths = 0)))
    }

    private fun sha(text: String) = java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
