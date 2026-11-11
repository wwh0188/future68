package io.openhoyi.session

import io.openhoyi.protocol.StartParameters
import org.junit.Assert.*
import org.junit.Test

class CustomCurveFirmwarePolicyTest {
    private val p = StartParameters(true, true, 2, 7, 93, 55, false, 8,
        20, 38, 0, 0, 150, 12, 400, 0, 0)

    @Test fun expandedFlowModesNeed114WhileExistingPressureAndHsStayCompatible() {
        for (candidate in listOf(p.copy(pressureLogic = false), p.copy(firstSegmentFlowMode = true))) {
            assertTrue(CustomCurveFirmwarePolicy.requires114(candidate))
            assertFalse(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmwareProfiles.FW_1_1_3))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmwareProfiles.FW_1_1_4))
            assertFalse(CustomCurveFirmwarePolicy.permits(candidate, 0x115))
        }
        for (candidate in listOf(p, p.copy(variableFlowLogic = false))) {
            assertFalse(CustomCurveFirmwarePolicy.requires114(candidate))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmwareProfiles.FW_1_1_3))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmwareProfiles.FW_1_1_4))
            assertFalse(CustomCurveFirmwarePolicy.permits(candidate, 0))
        }
    }
}
