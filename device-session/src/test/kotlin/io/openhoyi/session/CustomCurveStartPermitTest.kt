package io.openhoyi.session

import io.openhoyi.protocol.*
import org.junit.Assert.*
import org.junit.Test

class CustomCurveStartPermitTest {
    // The shared fixture is in studio mode and reports 94.07 C: keep the real
    // temperature readiness gate satisfied while testing recipe authorization.
    private val p = StartParameters(false, true, 2, 7, 94, 55, true, 20,
        20, 38, 0, 0, 150, 12, 400, 0, 0)

    private fun fixture(
        customCurveStartPermit:(StartParameters)->Boolean={false},
        customPressureStartPermit:(StartParameters)->Boolean={false}
    ):CoffeeSessionFixture = CoffeeSessionFixture(customCurveStartPermit,customPressureStartPermit).also { firmware(it,4) }

    private fun firmware(f:CoffeeSessionFixture,patch:Int) {
        val bytes=f.hex("830113FD5C007D0F350019006E")
        bytes[2]=(0x10 or patch).toByte()
        bytes[bytes.lastIndex]=bytes.dropLast(1).fold(0) {acc,b->acc xor (b.toInt() and 255)}.toByte()
        f.session.onNotification(f.session.generation,KnownGatt.coffeeNotify,bytes)
        f.idle(f.now)
    }

    @Test fun newFlowRequires114AndQueuedFirmwareChangeRevokesStart() {
        val f=fixture(customCurveStartPermit={true})
        firmware(f,3)
        assertNull(f.session.captureStartContext(p))
        firmware(f,4)
        val context=requireNotNull(f.session.captureStartContext(p))
        val queue=DeviceSession::class.java.getDeclaredField("queue").apply {isAccessible=true}.get(f.session) as GattQueue
        queue.enqueue(GattOperation.Discover,5000) {}
        val barrier=f.calls.last();val before=f.calls.size
        var result:OperationResult?=null
        f.session.startExtraction(p,context) {result=it}
        assertNull(result)
        firmware(f,3)
        f.session.onComplete(barrier.first,barrier.second,OperationResult.Success())
        assertEquals(before,f.calls.size)
        assertTrue(result is OperationResult.Failed)
        f.session.tick()
        assertEquals(before,f.calls.size)
    }

    @Test fun expandedCurveSubsetNeedsSeparateExactHostPermit() {
        assertTrue(CustomCurveStartPolicy.permits(p))
        assertFalse(CustomPressureStartPolicy.permits(p))
        assertNull(fixture().session.captureStartContext(p))
        assertNull(fixture { true }.session.captureStartContext(p))
        val f = fixture(customCurveStartPermit = { it == p })
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
            val f = fixture(customCurveStartPermit = {
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

    @Test fun hostCannotExtendValidatedRangesOrAuthorizeLossyValues() {
        val f = fixture(customCurveStartPermit = { true })
        for (bad in listOf(p.copy(target1 = 9), p.copy(target2 = 101),
            p.copy(variableFlowLogic = false), p.copy(firstFlowTenths = 151), p.copy(maximumWaterMl = 0), p.copy(maximumWaterMl = 801),
            p.copy(preinfusionSeconds = 128), p.copy(firstDurationSeconds = 256), p.copy(slot = 6))) {
            assertFalse(CustomCurveStartPolicy.permits(bad))
            assertNull(f.session.captureStartContext(bad))
        }
        // Inactive fields remain lossless in the encoded recipe; they do not set active-segment bounds.
        assertTrue(CustomCurveStartPolicy.permits(p.copy(target3 = 1, thirdFlowTenths = 10)))
        val hybrid = p.copy(firstSegmentFlowMode = false, target1 = 5)
        assertTrue(CustomCurveStartPolicy.permits(hybrid))
        assertFalse(CustomCurveStartPolicy.permits(hybrid.copy(target1 = 4)))
        val pressure = p.copy(variableFlowLogic = false, firstSegmentFlowMode = false,
            pressureLogic = true, target1 = 120)
        assertTrue(CustomCurveStartPolicy.permits(pressure))
        assertFalse(CustomCurveStartPolicy.permits(pressure.copy(target1 = 121)))
    }

    @Test fun expandedPermitStillRequiresFreshIdleAndSupportedFirmware() {
        val f = fixture(customCurveStartPermit = { true })
        assertNotNull(f.session.captureStartContext(p))
        f.now = 1501
        assertNull(f.session.captureStartContext(p))
        f.session.onNotification(f.session.generation, KnownGatt.coffeeNotify,
            f.hex("830115FD5C007D0F3500190068"))
        f.idle(f.now)
        assertNull(f.session.captureStartContext(p))
    }
}
