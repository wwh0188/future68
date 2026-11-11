package io.openhoyi.mobile

import io.openhoyi.protocol.CustomCurveStartPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CustomCurveModesTest {
    private fun pressure() = CustomCurveDocument("draft-00000000-0000-4000-8000-000000000001", "Local", 93, 3600,
        CustomCurveDocument.ControlMode.PRESSURE, listOf(CustomCurveDocument.Stage(65, 150), CustomCurveDocument.Stage(90, 400)))
    private fun flow() = pressure().copy(controlMode = CustomCurveDocument.ControlMode.FLOW,
        stages = listOf(CustomCurveDocument.Stage(30, 150), CustomCurveDocument.Stage(20, 400)),
        totalWaterLimitMl = 40, pressureLogic = true, preinfusionSeconds = 8, firstDurationSeconds = 12,
        description = "磨豆机 4.5\n18 g 粉碗，粉重 17.5 g")
    private class Disk : CustomCurveStore.Storage {
        var data: String? = null
        override fun read() = data
        override fun write(value: String) { data = value }
    }
    private fun item(document: CustomCurveDocument) = CurveLibraryItem(document.id, document.name, "", "", null, customDocument = document)

    @Test fun schemaThreeRoundTripAndStoreCopyPreservePublicDescriptionAndFlags() {
        val source = flow()
        assertEquals(3, JSONObject(source.encode()).getInt("schema"))
        assertEquals(source, CustomCurveDocument.decode(source.encode()))
        val disk = Disk(); val store = CustomCurveStore(disk)
        store.save(pressure()); store.save(source)
        val copy = store.saveCopy(source)
        val reopened = CustomCurveStore(disk)
        assertEquals(source, reopened.find(source.id))
        assertEquals(source.description, reopened.find(copy.id)!!.description)
        assertEquals(source.copy(id = copy.id), copy)
        assertEquals(CustomCurveStore.ImportStatus.CONFLICT, reopened.importDocument(source.copy(description = "Other")).status)
        assertEquals(source, reopened.find(source.id))
        for (field in listOf("pressureLogic", "firstSegmentFlowMode")) {
            assertTrue(runCatching { CustomCurveDocument.decode(JSONObject(source.encode()).put(field, 1).toString()) }.isFailure)
        }
        assertNull(CustomCurveDocument.decode(source.copy(totalWaterLimitMl = null).encode()).totalWaterLimitMl)
    }

    @Test fun officialModesMapIndependentlyAndFirstFlowChangesOnlyItsOwnUnit() {
        for (logic in listOf(false, true)) for (firstFlow in listOf(false, true)) {
            val document = flow().copy(pressureLogic = logic, firstSegmentFlowMode = firstFlow)
            val profile = requireNotNull(CustomCurveAdapter.profile(document, false))
            assertTrue(CustomCurveStartPolicy.permits(profile.parameters))
            assertTrue(profile.parameters.variableFlowLogic)
            assertEquals(logic, profile.parameters.pressureLogic)
            assertEquals(firstFlow, profile.parameters.firstSegmentFlowMode)
            assertEquals(8, profile.parameters.preinfusionSeconds)
            assertEquals(12, profile.parameters.firstDurationSeconds)
            assertEquals(40, profile.parameters.maximumWaterMl)
            assertEquals(144, requireNotNull(CustomCurveAdapter.profile(document, true)).parameters.maximumWaterMl)
            val units = CurveStageParameters.preview(item(document)).stages.map { it.unit }
            assertEquals(if (firstFlow) CurveStageParameters.Unit.FLOW_ML_PER_SECOND else CurveStageParameters.Unit.PRESSURE_BAR, units[0])
            assertEquals(CurveStageParameters.Unit.FLOW_ML_PER_SECOND, units[1])
        }
        val follow = requireNotNull(CustomCurveAdapter.profile(pressure().copy(pressureLogic = true), false))
        assertTrue(follow.parameters.pressureLogic); assertFalse(follow.parameters.variableFlowLogic)
        assertNull(CustomPressureCurveAdapter.profile(flow(), false))
        assertNull(CustomCurveAdapter.profile(pressure().copy(firstSegmentFlowMode = true), false))
        assertNull(CustomCurveAdapter.profile(flow().copy(stages = listOf(CustomCurveDocument.Stage(101, 150))), false))
        assertNull(CustomCurveAdapter.profile(flow().copy(firstSegmentFlowMode = true, stages = listOf(CustomCurveDocument.Stage(9, 150))), false))
    }

    @Test fun legacyRawStaysReadOnlyAndDescriptionDoesNotChangeRecipeFingerprint() {
        val raw = pressure().copy(controlMode = CustomCurveDocument.ControlMode.FLOW_RAW)
        val decoded = CustomCurveDocument.decode(raw.encode())
        assertEquals(CustomCurveDocument.ControlMode.FLOW_RAW, decoded.controlMode)
        assertNull(CustomCurveAdapter.profile(decoded, false))
        assertFalse(CurveStageParameters.preview(item(decoded)).renderable)
        val source = flow()
        assertEquals(CustomCurveFingerprint.of(source), CustomCurveFingerprint.of(source.copy(description = "Changed", name = "Renamed")))
        assertNotEquals(CustomCurveFingerprint.of(source), CustomCurveFingerprint.of(source.copy(pressureLogic = false)))
        assertNotEquals(CustomCurveFingerprint.of(source), CustomCurveFingerprint.of(source.copy(firstSegmentFlowMode = true)))
        assertNotEquals(CustomCurveFingerprint.of(source), CustomCurveFingerprint.of(source.copy(firstDurationSeconds = 13)))
        assertNotEquals(CustomCurveFingerprint.of(source), CustomCurveFingerprint.of(source.copy(preinfusionSeconds = 9)))
        val plan = CurveRecipePlan.from(item(source))
        assertEquals(source.description, plan.advice); assertEquals(8, plan.preinfusionSeconds); assertEquals(12, plan.firstStageDurationSettingSeconds)
    }

    @Test fun currentStoredFlowOnlyReceivesPermissionAndNotesMayChangeWithoutChangingParameters() {
        var current = flow()
        val library = CurveLibrary(emptyList(), draftsProvider = { listOf(item(current)) }, enableCustomPressureExecution = true)
        val old = requireNotNull(library.resolve(current.id, false))
        assertTrue(library.canStart(item(current)))
        assertTrue(library.permitsCustomCurveStart(current.id, old.parameters))
        assertFalse(library.permitsCustomPressureStart(current.id, old.parameters))
        current = current.copy(description = "New description")
        assertTrue(library.permitsCustomCurveStart(current.id, old.parameters))
        current = current.copy(pressureLogic = false)
        assertFalse(library.permitsCustomCurveStart(current.id, old.parameters))
    }

    @Test fun factoryCopyKeepsOriginalLogicTimesAndDescriptionAndGetsLocalIdentity() {
        val factory = java.io.File("src/main/assets/factory_curves_v3.tsv").inputStream().use(FactoryCurveCatalog::load)
            .first { it.variableFlowLogic }
        val source = CurveLibraryItem(factory.id, factory.name, "", "", null, factory)
        val copied = CustomCurveDocument.fromLibraryItem(source)
        assertNotEquals(factory.id, copied.id)
        assertEquals(CustomCurveDocument.ControlMode.FLOW, copied.controlMode)
        assertEquals(factory.pressureLogic, copied.pressureLogic)
        assertFalse(copied.firstSegmentFlowMode)
        assertEquals(factory.tips, copied.description)
        assertEquals(factory.preinfusionSeconds, copied.preinfusionSeconds)
        assertEquals(factory.firstDurationSeconds, copied.firstDurationSeconds)
        assertEquals(factory.flowMl, copied.totalWaterLimitMl)
        assertEquals(factory.targets.take(factory.segmentCount), copied.stages.map { it.target })
    }
}
