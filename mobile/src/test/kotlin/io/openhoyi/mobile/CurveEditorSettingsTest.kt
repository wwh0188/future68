package io.openhoyi.mobile
import org.junit.Assert.*
import org.junit.Test

class CurveEditorSettingsTest {
    @Test fun malformedAndOutOfRangeInputsHaveSpecificFieldsAndRecoveryClearsThem() {
        val bad = CurveEditorSettings.errors("", "256", "-", "0", 700, true, 250, "36")
        assertEquals(CurveEditorSettings.Error.BLOOM, bad["preinfusion"])
        assertEquals(CurveEditorSettings.Error.DURATION, bad["first-duration"])
        assertEquals(CurveEditorSettings.Error.COMPENSATION, bad["compensation"])
        assertEquals(CurveEditorSettings.Error.TOTAL, bad["total-water-limit"])
        assertTrue(CurveEditorSettings.errors("3", "6", "-0.2", "70", 700, true, 250, "36").values.all { it == null })
    }
    @Test fun privateLegacyDurationAndCompensationStayReadableWithoutChangingDefaults() {
        val preserved = CurveEditorSettings.errors("0", "255", "36", "", 500, false, 150, "36",
            maximumDuration = 255, enforceCompensationTarget = false)
        assertTrue(preserved.values.all { it == null })
        assertEquals(CurveEditorSettings.Error.DURATION,
            CurveEditorSettings.errors("0", "255", "0", "", 500, true, 150, "36")["first-duration"])
    }
    @Test fun firstFlowExitAndCompensationRelationAreNeverGenericFailures() {
        val errors = CurveEditorSettings.errors("0", "0", "36", "", 500, true, 0, "36")
        assertEquals(CurveEditorSettings.Error.EXIT, errors["first-duration"])
        assertEquals(CurveEditorSettings.Error.COMPENSATION_TARGET, errors["compensation"])
        assertNull(errors["total-water-limit"])
    }
}
