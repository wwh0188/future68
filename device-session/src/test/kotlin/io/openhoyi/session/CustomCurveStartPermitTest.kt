package io.openhoyi.session

import io.openhoyi.protocol.*
import org.junit.Assert.*
import org.junit.Test

class CustomCurveStartPermitTest {
    // The shared fixture is in studio mode and reports 94.07 C: keep the real
    // temperature readiness gate satisfied while testing recipe authorization.
    private val p = StartParameters(false, true, 2, 7, 94, 55, true, 8,
        20, 38, 0, 0, 150, 12, 400, 0, 0)

    @Test fun expandedCurveSubsetNeedsSeparateExactHostPermit() {
        assertTrue(CustomCurveStartPolicy.permits(p))
        assertFalse(CustomPressureStartPolicy.permits(p))
        assertNull(CoffeeSessionFixture().session.captureStartContext(p))
        assertNull(CoffeeSessionFixture { true }.session.captureStartContext(p))
        val f = CoffeeSessionFixture(customCurveStartPermit = { it == p })
        assertNotNull(f.session.captureStartContext(p))
        assertNull(f.session.captureStartContext(p.copy(pressureLogic = true)))
        val before = f.calls.size
        var result: OperationResult? = null
        f.session.startExtraction(p) { result = it }
        assertEquals(before + 1, f.calls.size)
        assertArrayEquals(CoffeeCommands.start(p).frame.toByteArray(), (f.calls.last().third as GattOperation.Write).bytes)
        f.complete(OperationResult.Unknown("link lost"))
        assertTrue(result is OperationResult.Unknown)
        f.session.tick()
        assertEquals(before + 1, f.calls.size)
    }

    @Test fun revokedOrThrowingPermitAtDispatchSendsNothing() {
        for (throwOnRevoke in listOf(false, true)) {
            var allowed = true
            val f = CoffeeSessionFixture(customCurveStartPermit = {
                if (!allowed && throwOnRevoke) error("recipe unavailable") else allowed
            })
            val context = requireNotNull(f.session.captureStartContext(p))
            val queue = DeviceSession::class.java.getDeclaredField("queue").apply { isAccessible = true }
                .get(f.session) as GattQueue
            queue.enqueue(GattOperation.Discover, 5000) {}
            val barrier = f.calls.last()
            val count = f.calls.size
            var result: OperationResult? = null
            f.session.startExtraction(p, context) { result = it }
            allowed = false
            f.session.onComplete(barrier.first, barrier.second, OperationResult.Success())
            assertTrue(result is OperationResult.Failed)
            f.session.tick()
            assertEquals(count, f.calls.size)
        }
    }

    @Test fun hostCannotExtendValidatedRangesOrAuthorizeLossyOrInactiveValues() {
        val f = CoffeeSessionFixture(customCurveStartPermit = { true })
        for (bad in listOf(p.copy(target1 = 9), p.copy(target2 = 101),
            p.copy(variableFlowLogic = false), p.copy(firstFlowTenths = 151), p.copy(target3 = 1),
            p.copy(thirdFlowTenths = 10), p.copy(maximumWaterMl = 0), p.copy(maximumWaterMl = 801),
            p.copy(preinfusionSeconds = 128), p.copy(firstDurationSeconds = 256), p.copy(slot = 6))) {
            assertFalse(CustomCurveStartPolicy.permits(bad))
            assertNull(f.session.captureStartContext(bad))
        }
        val hybrid = p.copy(firstSegmentFlowMode = false, target1 = 5)
        assertTrue(CustomCurveStartPolicy.permits(hybrid))
        assertFalse(CustomCurveStartPolicy.permits(hybrid.copy(target1 = 4)))
        val pressure = p.copy(variableFlowLogic = false, firstSegmentFlowMode = false,
            pressureLogic = true, target1 = 120)
        assertTrue(CustomCurveStartPolicy.permits(pressure))
        assertFalse(CustomCurveStartPolicy.permits(pressure.copy(target1 = 121)))
    }

    @Test fun expandedPermitStillRequiresFreshIdleAndSupportedFirmware() {
        val f = CoffeeSessionFixture(customCurveStartPermit = { true })
        assertNotNull(f.session.captureStartContext(p))
        f.now = 1501
        assertNull(f.session.captureStartContext(p))
        f.session.onNotification(f.session.generation, KnownGatt.coffeeNotify,
            f.hex("830115FD5C007D0F3500190068"))
        f.idle(f.now)
        assertNull(f.session.captureStartContext(p))
    }
}
