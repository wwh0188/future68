package io.openhoyi.mobile

import io.openhoyi.session.CoffeeFirmware
import io.openhoyi.session.CustomCurveFirmwarePolicy
import org.junit.Assert.*
import org.junit.Test

class CustomFlowCurveTest {
    private class Disk : CustomCurveStore.Storage {
        var data: String? = null
        override fun read() = data
        override fun write(value: String) { data = value }
    }
    @Test fun followRoundtripRetainsPrivateFieldsAndHasSeparateFirmwarePermission() {
        val follow = CustomCurveDocument(CustomCurveDocument.newId(), "Follow", 93, 3600,
            CustomCurveDocument.ControlMode.PRESSURE,
            listOf(CustomCurveDocument.Stage(30, 150), CustomCurveDocument.Stage(90, 400)),
            totalWaterLimitMl = 70, pressureLogic = true, description = "磨豆 4.5\n粉重 18 g",
            compensationHundredthsGram = 20,
            inactiveStages = listOf(CustomCurveDocument.Stage(47, 121), CustomCurveDocument.Stage(29, 65001)))
        val restored = CustomCurveDocument.decode(follow.encode())
        assertEquals(follow, restored)
        val disk = Disk(); CustomCurveStore(disk).save(restored)
        assertEquals(follow, CustomCurveStore(disk).find(follow.id))
        val parameters = requireNotNull(CustomCurveAdapter.profile(restored, false)).parameters
        assertTrue(parameters.pressureLogic); assertFalse(parameters.variableFlowLogic)
        assertEquals(47, parameters.target3); assertEquals(121, parameters.thirdFlowTenths)
        for (patch in listOf(2, 3, 4, 5, 6)) assertEquals(patch in 3..5,
            CustomCurveFirmwarePolicy.permits(parameters, CoffeeFirmware(1, 1, patch)))
    }
}
