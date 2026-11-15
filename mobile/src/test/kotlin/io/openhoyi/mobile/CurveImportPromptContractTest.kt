package io.openhoyi.mobile

import io.openhoyi.protocol.CustomCurveStartPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Private prompt examples use the actual private parser and execution adapter. */
class CurveImportPromptContractTest {
    private val directory get() = File(requireNotNull(System.getProperty("hoyi.curve.prompt.dir")))
    private fun document(name: String) = CustomCurveDocument.decode(File(directory, "examples/$name.json").readText())
    private fun executable(doc: CustomCurveDocument) {
        assertEquals(doc, CustomCurveDocument.decode(doc.encode()))
        for (scale in listOf(false, true)) {
            val profile = requireNotNull(CustomCurveAdapter.profile(doc, scale))
            assertTrue(CustomCurveStartPolicy.permits(profile.parameters))
            assertEquals(doc.description, CustomCurveDocument.decode(doc.encode()).description)
        }
    }
    @Test fun publishedPrivateExamplesRoundTripAndCoverModeCombinations() {
        for (logic in listOf(false, true)) {
            executable(document("pressure").copy(pressureLogic = logic))
            for (firstFlow in listOf(false, true)) {
                val flow = document("flow").copy(pressureLogic = logic, firstSegmentFlowMode = firstFlow)
                executable(flow)
                assertEquals(firstFlow, flow.isFlowStage(0))
                assertTrue(flow.isFlowStage(1))
            }
        }
        assertEquals(-50, document("pressure").compensationHundredthsGram)
        assertEquals(3600, document("flow").targetHundredthsGram)
        assertEquals(8, document("flow").firstDurationSeconds)
    }
    @Test fun importIsNotExecutionAndUnsupportedHistoricalDraftsStayReadable() {
        val p = document("pressure")
        val timedOut = p.copy(firstDurationSeconds = 255, pressureLogic = true)
        assertEquals(timedOut, CustomCurveDocument.decode(timedOut.encode()))
        assertNull(CustomCurveAdapter.profile(timedOut, false))
        assertNull(CustomCurveAdapter.profile(p.copy(targetHundredthsGram = 3601), true))
        val legacy = p.copy(controlMode = CustomCurveDocument.ControlMode.FLOW_RAW)
        assertEquals(legacy, CustomCurveDocument.decode(legacy.encode()))
        assertNull(CustomCurveAdapter.profile(legacy, false))
    }
    @Test fun rejectsUpstreamNestedFormatAndMissingOrExtraFields() {
        val p = document("pressure")
        for (raw in listOf(
            JSONObject(p.encode()).put("configuration", JSONObject()).toString(),
            JSONObject(p.encode()).also { it.remove("totalWaterLimitMl") }.toString(),
            JSONObject(p.encode()).put("temperatureC", "92").toString(),
        )) assertTrue(runCatching { CustomCurveDocument.decode(raw) }.isFailure)
    }
    @Test fun promptContainsTheSameExamplesAndPersonalHelpHasItsOwnRoute() {
        val prompt = File(directory, "personal-photo-to-json.zh-CN.txt").readText()
        for (name in listOf("pressure", "flow")) {
            assertTrue(prompt.contains(File(directory, "examples/$name.json").readText().trim()))
        }
        assertTrue(prompt.contains("schema 固定4"))
        assertEquals(AppSettingsFragment.PERSONAL_CURVE_IMPORT_GUIDE_URL,
            AppSettingsFragment.curveImportGuideUrl("io.openhoyi.mobile.personal"))
        assertEquals(AppSettingsFragment.CURVE_IMPORT_GUIDE_URL,
            AppSettingsFragment.curveImportGuideUrl("io.openhoyi.mobile"))
    }
}
