package io.openhoyi.session

import io.openhoyi.protocol.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

/** Offline GATT queue checks only. No device or physical extraction is involved. */
class FollowAndGlobalSlowModeTest {
    private val p = StartParameters(true, true, 3, 7, 93, 70, true, 4, 25, 35, 20, 0, 150, 7, 400, 150, 0)
    private class Fixture(permit: (StartParameters) -> Boolean = { false }, pressurePermit: (StartParameters) -> Boolean = { false }, frames:Set<String> = emptySet()) {
        var now = 0L
        val calls = mutableListOf<Triple<Long, Long, GattOperation>>()
        val session = DeviceSession(DeviceRole.COFFEE, object : GattDriver {
            override fun execute(generation: Long, token: Long, operation: GattOperation): Boolean {
                calls += Triple(generation, token, operation); return true
            }
            override fun close(generation: Long) = Unit
        }, { now }, customPressureStartPermit = pressurePermit, customCurveStartPermit = permit, legacyVerifiedStartFrames=frames)
        fun bytes(text: String) = text.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        fun complete(result: OperationResult = OperationResult.Success()) {
            val call = calls.last(); session.onComplete(call.first, call.second, result)
        }
        fun fresh() = session.onNotification(session.generation, KnownGatt.coffeeNotify,
            bytes("400023F02F1C770B00000000000000190321AF"))
        fun firmware(patch: Int) {
            val frame = bytes("830113FD5C007D0F350019006E")
            frame[2] = (0x10 or patch).toByte()
            frame[frame.lastIndex] = frame.dropLast(1).fold(0) { acc, b -> acc xor (b.toInt() and 255) }.toByte()
            session.onNotification(session.generation, KnownGatt.coffeeNotify, frame)
        }
        init {
            session.connect("device", CoffeeAuthentication(LocalDateTime.of(2026, 9, 20, 12, 0), "123456"))
            complete()
            complete(OperationResult.Success(listOf(
                CharacteristicInfo(KnownGatt.coffeeWrite, true, false, false, false),
                CharacteristicInfo(KnownGatt.coffeeNotify, false, false, true, false))))
            complete(); complete()
            firmware(4)
            check(session.state == DeviceState.READY); fresh()
        }
    }

    @Test fun followTemporaryExecutionIsExact113Through115OnlyEvenWithLegacyFramePermit() {
        val follow=p.copy(variableFlowLogic=false,firstSegmentFlowMode=false,target1=30,target2=90,target3=60)
        assertTrue(CustomCurveStartPolicy.permits(follow))
        val frame=CoffeeCommands.start(follow).frame.hex()
        for (patch in listOf(2,3,4,5,6)) for (legacy in listOf(false,true)) {
            val f=Fixture(permit={true}, frames=if(legacy)setOf(frame) else emptySet())
            f.firmware(patch);f.fresh()
            assertEquals(patch in 3..5,f.session.captureStartContext(follow)!=null)
            var result:OperationResult?=null
            val before=f.calls.size
            f.session.startExtraction(follow) {result=it}
            if(patch in 3..5) {
                assertEquals(before+1,f.calls.size)
                assertArrayEquals(CoffeeCommands.start(follow).frame.toByteArray(),(f.calls.last().third as GattOperation.Write).bytes)
                f.complete();assertTrue(result is OperationResult.Success)
            } else {assertEquals(before,f.calls.size);assertTrue(result is OperationResult.Failed)}
        }
    }

    @Test fun followQueuedContextRevokesAfterFirmwareChangeAndBroadPermitCannotOpenInvalidShape() {
        val follow=p.copy(variableFlowLogic=false,firstSegmentFlowMode=false,target1=30,target2=90,target3=60)
        val f=Fixture(permit={true});f.firmware(3);f.fresh()
        val context=requireNotNull(f.session.captureStartContext(follow))
        val queue=DeviceSession::class.java.getDeclaredField("queue").apply{isAccessible=true}.get(f.session) as GattQueue
        queue.enqueue(GattOperation.Discover,5000){}
        val barrier=f.calls.last();val before=f.calls.size;var result:OperationResult?=null
        f.session.startExtraction(follow,context){result=it};f.firmware(5);f.fresh()
        f.session.onComplete(barrier.first,barrier.second,OperationResult.Success())
        assertEquals(before,f.calls.size);assertTrue(result is OperationResult.Failed)
        val valid=Fixture(permit={true});valid.firmware(3);valid.fresh()
        for(bad in listOf(follow.copy(slot=1),follow.copy(segmentCount=1),follow.copy(firstSegmentFlowMode=true),
            follow.copy(target1=4),follow.copy(target2=121),follow.copy(firstFlowTenths=0),follow.copy(firstDurationSeconds=255)))
            assertNull(valid.session.captureStartContext(bad))
    }

    @Test fun slowGlobalRequiresExactCompatibilityVersionsAndRechecksQueuedFirmware() {
        val change=MachineSettingChange.LeverMode(false,true)
        for(patch in listOf(2,3,4,5,6)) {
            val f=Fixture();f.firmware(patch);f.fresh();val before=f.calls.size
            var result:OperationResult?=null
            f.session.writeSetting(change){result=it}
            if(patch in 3..5) {
                assertEquals(before+1,f.calls.size)
                assertArrayEquals(CoffeeCommands.setting(change).frame.toByteArray(),(f.calls.last().third as GattOperation.Write).bytes)
                f.complete();assertTrue(result is OperationResult.Success)
            } else {
                assertEquals(before,f.calls.size);assertTrue(result is OperationResult.Failed)
                f.session.writeSetting(change){result=it};assertEquals(before,f.calls.size)
            }
        }
        val f=Fixture();f.firmware(3);f.fresh()
        val queue=DeviceSession::class.java.getDeclaredField("queue").apply{isAccessible=true}.get(f.session) as GattQueue
        queue.enqueue(GattOperation.Discover,5000){}
        val barrier=f.calls.last();val before=f.calls.size;var result:OperationResult?=null
        f.session.writeSetting(change){result=it};f.firmware(5);f.fresh()
        f.session.onComplete(barrier.first,barrier.second,OperationResult.Success())
        assertEquals(before,f.calls.size);assertTrue(result is OperationResult.Failed)
    }

    @Test fun firmware113NewModesRetainAlarmFreshnessAndUnknownVersionGuards() {
        val follow=p.copy(variableFlowLogic=false,firstSegmentFlowMode=false,target1=30,target2=90,target3=60)
        val slow=MachineSettingChange.LeverMode(false,true)
        for (firmware in listOf(null,CoffeeFirmware(2,0,0))) {
            assertFalse(CustomCurveFirmwarePolicy.permits(follow,firmware))
            assertFalse(MachineModeFirmwarePolicy.permits(slow,firmware))
        }
        for (alarm in listOf(false,true)) {
            val f=Fixture(permit={true});f.firmware(3);f.fresh()
            if (alarm) {
                val c6=f.bytes("400023F02F1C770B00000000000000190321AF");c6[13]=0x20
                f.session.onNotification(f.session.generation,KnownGatt.coffeeNotify,c6)
            } else f.now=1501
            val before=f.calls.size;var result:OperationResult?=null
            assertNull(f.session.captureStartContext(follow))
            f.session.startExtraction(follow){result=it};assertTrue(result is OperationResult.Failed)
            f.session.writeSetting(slow){result=it};assertTrue(result is OperationResult.Failed)
            assertEquals(before,f.calls.size)
            f.now=1501
            f.session.stopExtraction(7){result=it}
            assertEquals(before+1,f.calls.size)
            assertArrayEquals(CoffeeCommands.stop(7).frame.toByteArray(),(f.calls.last().third as GattOperation.Write).bytes)
        }
    }

    @Test fun firmware113And115KeepC6BlockAndEmergencyStopWithoutFreshIdleOrNoAlarmRequirement() {
      for (patch in listOf(3,5)) {
        val f=Fixture(permit={true});f.firmware(patch);f.fresh()
        val c6=f.bytes("400023F02F1C770B00000000000000190321AF");c6[13]=0x20
        f.session.onNotification(f.session.generation,KnownGatt.coffeeNotify,c6)
        assertFalse(CoffeeAlarmPolicy.permitsNewControl(0x20))
        assertNull(f.session.captureStartContext(p))
        var result:OperationResult?=null;val before=f.calls.size
        f.session.startExtraction(p){result=it};assertTrue(result is OperationResult.Failed);assertEquals(before,f.calls.size)
        f.session.writeSetting(MachineSettingChange.BrewTemperature(93)){result=it}
        assertTrue(result is OperationResult.Failed);assertEquals(before,f.calls.size)
        f.now=1501
        f.session.stopExtraction(7){result=it}
        assertEquals(before+1,f.calls.size)
        assertArrayEquals(CoffeeCommands.stop(7).frame.toByteArray(),(f.calls.last().third as GattOperation.Write).bytes)
        f.complete();assertTrue(result is OperationResult.Success)
      }
    }
}
