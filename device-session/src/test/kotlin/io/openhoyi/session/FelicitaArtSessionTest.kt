package io.openhoyi.session

import io.openhoyi.protocol.*
import org.junit.Assert.*
import org.junit.Test

class FelicitaArtSessionTest {
    // Anonymous notification bytes only, from user diagnostics ZIP SHA-256
    // 5fc48e58f53522be25f2da462fb1bab485f98804bfcba9b1dcb4563749a79dbd.
    // No captured write payload is claimed. The 54 command has separate static evidence.
    private fun hex(value:String)=value.chunked(2).map {it.toInt(16).toByte()}.toByteArray()
    private val zero get()=hex("01022b303030303030206743e34f229e0d0a")
    private val positive get()=hex("01022b3030363336312067432033229f0d0a")
    private val negative get()=hex("01022d3030333739322067434d11229e0d0a")
    private val parameters=StartParameters(true,true,3,7,92,136,false,0,20,35,18,0,150,5,400,130,0)
    private class Driver:GattDriver {
        val operations=mutableListOf<Triple<Long,Long,GattOperation>>()
        override fun execute(generation:Long,token:Long,operation:GattOperation):Boolean {
            operations+=Triple(generation,token,operation);return true
        }
        override fun close(generation:Long) {}
    }
    private class Coffee:CoffeeControl {
        override val ready=true
        var starts=0
        var stops=0
        override fun prepareStart(parameters:StartParameters)=true
        override fun startConditionsValid(parameters:StartParameters)=true
        override fun start(parameters:StartParameters,beforeDispatch:()->Boolean,done:(OperationResult)->Unit) {
            assertTrue(beforeDispatch());starts++;done(OperationResult.Success())
        }
        override fun stop(done:(OperationResult)->Unit) {stops++;done(OperationResult.Success())}
    }
    private inner class Fixture {
        var now=100L
        var serial=0L
        val driver=Driver()
        val samples=mutableListOf<ScaleObservation>()
        val tare=StandaloneTare(clock={now})
        var observed:((ScaleObservation)->Unit)?=null
        val adapter=FelicitaArtScaleProtocolAdapter
        val endpoint=adapter.notifyEndpoint
        val session=DeviceSession(DeviceRole.BOOKOO,driver,{now},scaleAdapter=adapter,
            scaleObservation={sample->samples+=sample;tare.sample(++serial,sample.hundredthsGram);observed?.invoke(sample)})
        val coffee=Coffee()
        val control=ScaleSessionControl(session,tare,{serial})
        val extraction=ExtractionController(coffee,control,{now})
        init {observed=extraction::weight}
        fun complete(result:OperationResult=OperationResult.Success()) {
            val(g,t,_)=driver.operations.last();session.onComplete(g,t,result)
        }
        fun subscribe() {
            session.connect("AA:BB:CC:DD:EE:FF");complete()
            complete(OperationResult.Success(listOf(CharacteristicInfo(endpoint,true,false,true,false))))
            complete()
            assertEquals(DeviceState.SYNCHRONIZING,session.state)
        }
        fun receive(bytes:ByteArray) {now++;session.onNotification(session.generation,endpoint,bytes)}
        fun ready() {subscribe();receive(positive);assertEquals(DeviceState.READY,session.state)}
        fun writes()=driver.operations.mapNotNull {it.third as? GattOperation.Write}
        fun beginWeightShot() {
            ready();assertTrue(extraction.start(parameters,2700,0))
            receive(zero) // A zero preceding write completion cannot authorize start.
            assertEquals(0,coffee.starts);assertEquals(StandaloneTare.State.WRITING,tare.state)
            now++;complete();assertEquals(StandaloneTare.State.WAITING_ZERO,tare.state)
            receive(zero)
            assertEquals(StandaloneTare.State.CONFIRMED,tare.state)
            assertEquals(ExtractionState.RUNNING,extraction.state);assertEquals(1,coffee.starts)
        }
    }
    @Test fun capturedGramFramesAreSignedAndEligibleOnlyOnExplicitArtProfile() {
        val f=Fixture();f.subscribe();assertTrue(f.writes().isEmpty())
        for((frame,expected) in listOf(positive to 6361,negative to -3792,zero to 0)) {
            f.receive(frame)
            val sample=f.samples.last()
            assertEquals(expected,sample.hundredthsGram);assertTrue(sample.eligibleForControl)
            assertEquals(ScaleEvidence.VERIFIED_TRANSPORT,sample.evidence)
            assertEquals(ByteFrame(frame),sample.raw)
        }
        assertTrue(f.adapter.transportVerified);assertFalse(f.adapter.documentedTransport)
        assertFalse(f.adapter.capabilities.automaticTare);assertNull(f.adapter.heartbeat)
        assertFalse(f.adapter.capabilities.deviceTimer);assertFalse(f.adapter.capabilities.battery)
        assertFalse(FelicitaScaleProtocolAdapter.capabilities.validatedWeightControl)
        assertFalse(FelicitaReadOnlyScaleProtocolAdapter.capabilities.validatedWeightControl)
    }
    @Test fun nonGramUnknownUnitAndDamagedFramesRevokeReadyWithoutRefreshingWeight() {
        val f=Fixture();f.ready()
        val invalid=listOf(
            zero.apply {this[9]='o'.code.toByte();this[10]='z'.code.toByte()},
            zero.apply {this[9]='k'.code.toByte()},
            zero.apply {this[0]=3},
            zero.apply {this[3]='?'.code.toByte()},
            zero.apply {this[17]=0},
            zero.copyOf(17),zero+byteArrayOf(0))
        for(bytes in invalid) {
            val count=f.samples.size
            f.receive(bytes)
            assertEquals(DeviceState.SYNCHRONIZING,f.session.state)
            assertFalse(f.control.ready);assertEquals(count,f.samples.size)
            f.receive(zero);assertEquals(DeviceState.READY,f.session.state)
        }
    }
    @Test fun onlyExactTareOnFfe1CanBeWrittenAndConnectNeverWrites() {
        val f=Fixture();f.ready();assertTrue(f.writes().isEmpty())
        fun permits(endpoint:Endpoint,bytes:ByteArray)=
            OutboundWritePolicy.permits(DeviceRole.BOOKOO,GattOperation.Write(endpoint,bytes,true),f.adapter)
        assertTrue(permits(f.endpoint,byteArrayOf(0x54)))
        assertFalse(permits(KnownGatt.bookooWrite,byteArrayOf(0x54)))
        assertFalse(permits(KnownGatt.coffeeWrite,byteArrayOf(0x54)))
        for(bytes in listOf(byteArrayOf(),byteArrayOf(0x55),byteArrayOf(0x54,0)))assertFalse(permits(f.endpoint,bytes))
        f.now+=60_000;f.session.tick();assertTrue(f.writes().isEmpty())
    }
    @Test fun explicitSelectionPersistsAndOnlySameAddressLegacyDiscoveryCanReconnectArt() {
        val address="AA:BB:CC:DD:EE:FF"
        val art=ScaleSelectionPolicy.remembered(address,FelicitaArtScaleProtocolAdapter.id)!!
        assertSame(FelicitaArtScaleProtocolAdapter,ScaleSelectionPolicy.adapter(art.protocolId))
        assertTrue(ScaleSelectionPolicy.matchesDiscovered(art,address.lowercase(),art.protocolId))
        assertTrue(ScaleSelectionPolicy.matchesDiscovered(art,address,FelicitaScaleProtocolAdapter.id))
        assertFalse(ScaleSelectionPolicy.matchesDiscovered(art,"AA:BB:CC:DD:EE:00",FelicitaScaleProtocolAdapter.id))
        for(id in listOf(BookooScaleProtocolAdapter.id,TimemoreDotScaleProtocolAdapter.id,
            FelicitaReadOnlyScaleProtocolAdapter.id,null))assertFalse(ScaleSelectionPolicy.matchesDiscovered(art,address,id))
        val legacy=ScaleSelection(address,FelicitaScaleProtocolAdapter.id)
        assertTrue(ScaleSelectionPolicy.matchesDiscovered(legacy,address,legacy.protocolId))
        assertFalse(ScaleSelectionPolicy.matchesDiscovered(legacy,address,art.protocolId))
        assertEquals(legacy,ScaleSelectionPolicy.remembered(address,legacy.protocolId))
    }
    @Test fun savedReadOnlySelectionKeepsItsAdapterOnSameAddressLegacyDiscovery() {
        val address="AA:BB:CC:DD:EE:FF"
        val readOnly=ScaleSelectionPolicy.remembered(address,FelicitaReadOnlyScaleProtocolAdapter.id)!!
        assertTrue(ScaleSelectionPolicy.matchesDiscovered(readOnly,address,FelicitaScaleProtocolAdapter.id))
        assertSame(FelicitaReadOnlyScaleProtocolAdapter,ScaleSelectionPolicy.adapter(readOnly.protocolId))
        assertFalse(ScaleSelectionPolicy.matchesDiscovered(readOnly,"AA:BB:CC:DD:EE:00",FelicitaScaleProtocolAdapter.id))
        for(id in listOf(FelicitaArtScaleProtocolAdapter.id,BookooScaleProtocolAdapter.id,null))
            assertFalse(ScaleSelectionPolicy.matchesDiscovered(readOnly,address,id))
        assertFalse(ScaleSelectionPolicy.matchesDiscovered(
            ScaleSelection(address,FelicitaScaleProtocolAdapter.id),address,readOnly.protocolId))
    }
    @Test fun weightShotWaitsForPostWriteZeroThenStartsOnceAndStopsOnceAfterSevenSeconds() {
        val f=Fixture();f.beginWeightShot()
        assertEquals("54",ByteFrame(f.writes().single().bytes).hex())
        val started=f.now
        f.receive(zero);assertEquals(1,f.coffee.starts)
        f.now=started+6998;f.receive(positive);assertEquals(0,f.coffee.stops)
        f.now=started+7000;f.receive(positive)
        assertEquals(StopReason.TARGET_WEIGHT,f.extraction.stopReason);assertEquals(1,f.coffee.stops)
        f.receive(positive);f.extraction.tick();assertEquals(1,f.coffee.stops)
        assertEquals(1,f.writes().size)
    }
    @Test fun disconnectionAndStaleWeightRequestOneProtectiveStop() {
        val disconnected=Fixture();disconnected.beginWeightShot()
        disconnected.session.disconnect();disconnected.tare.disconnected();disconnected.extraction.scaleDisconnected()
        disconnected.extraction.tick();assertEquals(1,disconnected.coffee.stops)
        assertEquals(StopReason.SCALE_UNAVAILABLE,disconnected.extraction.stopReason)
        val stale=Fixture();stale.beginWeightShot()
        stale.now+=1501;stale.extraction.tick();stale.extraction.tick()
        assertEquals(1,stale.coffee.stops);assertEquals(StopReason.SCALE_UNAVAILABLE,stale.extraction.stopReason)
    }
    @Test fun unknownTareDoesNotRetryAndOldGenerationZeroCannotConfirmReplacementTare() {
        val unknown=Fixture();unknown.ready();assertTrue(unknown.extraction.start(parameters,2700,0))
        unknown.complete(OperationResult.Unknown("lost write result"));unknown.receive(zero)
        unknown.now+=10_000;unknown.session.tick();unknown.extraction.tick()
        assertEquals(StandaloneTare.State.UNKNOWN,unknown.tare.state)
        assertFalse(unknown.control.startAllowed);assertEquals(0,unknown.coffee.starts)
        assertEquals(1,unknown.writes().size)
        val f=Fixture();f.ready();val old=f.session.generation
        f.session.disconnect();f.subscribe();f.receive(positive)
        assertTrue(f.extraction.start(parameters,2700,0));f.now++;f.complete()
        f.now++;f.session.onNotification(old,f.endpoint,zero)
        assertEquals(StandaloneTare.State.WAITING_ZERO,f.tare.state);assertEquals(0,f.coffee.starts)
        f.receive(zero);assertEquals(StandaloneTare.State.CONFIRMED,f.tare.state);assertEquals(1,f.coffee.starts)
    }
    @Test fun flowOnlyShotNeverPerformsDelayedAutomaticTare() {
        val f=Fixture();f.ready();assertTrue(f.extraction.start(parameters,0,0))
        assertEquals(1,f.coffee.starts)
        f.now+=1500;f.extraction.tick();f.now+=30_000;f.extraction.tick()
        assertTrue(f.writes().isEmpty());assertEquals(StandaloneTare.State.IDLE,f.tare.state)
        assertEquals(0,f.coffee.stops)
    }
}
