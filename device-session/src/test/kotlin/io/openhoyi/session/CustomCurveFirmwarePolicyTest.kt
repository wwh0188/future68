package io.openhoyi.session

import io.openhoyi.protocol.StartParameters
import org.junit.Assert.*
import org.junit.Test

class CustomCurveFirmwarePolicyTest {
    private val p = StartParameters(true, true, 2, 7, 93, 55, false, 8,
        20, 38, 0, 0, 150, 12, 400, 0, 0)

    @Test fun allModesRequireExactOfficialCompatibilityProfile() {
        for (candidate in listOf(p.copy(pressureLogic = false), p.copy(firstSegmentFlowMode = true))) {
            assertTrue(CustomCurveFirmwarePolicy.requiresReviewedFlowProfile(candidate))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmwareProfiles.FW_1_1_3))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmwareProfiles.FW_1_1_4))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, 0x115))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmware(1,1,5)))
            assertFalse(CustomCurveFirmwarePolicy.permits(candidate, 0x116))
            assertFalse(CustomCurveFirmwarePolicy.permits(candidate, null))
        }
        for (candidate in listOf(p, p.copy(variableFlowLogic = false))) {
            assertFalse(CustomCurveFirmwarePolicy.requiresReviewedFlowProfile(candidate))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmwareProfiles.FW_1_1_3))
            assertTrue(CustomCurveFirmwarePolicy.permits(candidate, CoffeeFirmwareProfiles.FW_1_1_4))
            assertFalse(CustomCurveFirmwarePolicy.permits(candidate, 0))
        }
    }
}
