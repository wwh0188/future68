package io.openhoyi.mobile

import io.openhoyi.protocol.ByteFrame
import io.openhoyi.protocol.ScaleCapabilities
import io.openhoyi.protocol.ScaleEvidence
import io.openhoyi.protocol.ScaleObservation
import io.openhoyi.session.*
import org.junit.Assert.*
import org.junit.Test

class FelicitaWeightControlPolicyTest {
    private val address = "AA:BB:CC:DD:EE:FF"
    // Anonymous gram notification captured during the user's place/remove/tare diagnostic.
    private val frame = ByteFrame("01022B303231353934206743EB02229E0D0A".chunked(2)
        .map { it.toInt(16).toByte() }.toByteArray())
    private val observation = ScaleObservation(21594, 900, ScaleCapabilities(tare = true),
        ScaleEvidence.DOCUMENTED_TRANSPORT, raw = frame)
    private fun context(address: String? = this.address, protocol: String? = FelicitaScaleProtocolAdapter.id,
        epoch: Long = 3, state: DeviceState = DeviceState.READY, sample: ScaleObservation? = observation,
        now: Long = 1000, shot: ExtractionState = ExtractionState.IDLE, manual: Boolean = false,
        preparing: Boolean = false, tare: StandaloneTare.State = StandaloneTare.State.IDLE, mock: Boolean = false) =
        FelicitaWeightControlPolicy.context(address, protocol, epoch, state, sample, now, shot, manual,
            preparing, tare, mock)

    @Test fun onlyFreshGramObservationFromTheCurrentlySelectedFelicitaCanOfferOptIn() {
        val legacy = context(address = address.lowercase())!!
        assertEquals(address, legacy.address)
        assertNotNull(context(protocol = FelicitaReadOnlyScaleProtocolAdapter.id,
            sample = observation.copy(evidence = ScaleEvidence.LIVE_READ_ONLY)))
        assertNull(context(protocol = FelicitaReadOnlyScaleProtocolAdapter.id))
        assertNull(context(protocol = BookooScaleProtocolAdapter.id))
        assertNull(context(protocol = FelicitaArtScaleProtocolAdapter.id))
        assertNull(context(protocol = null))
        assertNull(context(address = null))
        assertNull(context(address = "invalid"))
        assertNull(context(sample = null))
        assertNull(context(sample = observation.copy(capabilities = ScaleCapabilities(weight = false))))
        assertNull(context(sample = observation.copy(evidence = ScaleEvidence.OFFLINE_CANDIDATE)))
        assertNotNull(context(now = 2400))
        assertNull(context(now = 2401))
        assertNull(context(now = 899))
        assertFalse(legacy.toString().contains(address))
    }

    @Test fun nonGramDamagedAndMismatchedRawReadingsCannotOfferOptIn() {
        val ounce = frame.toByteArray().apply { this[9] = 'o'.code.toByte(); this[10] = 'z'.code.toByte() }
        val unknown = frame.toByteArray().apply { this[9] = 'k'.code.toByte() }
        val damaged = frame.toByteArray().apply { this[17] = 0 }
        for (raw in listOf(ounce, unknown, damaged, byteArrayOf()))
            assertNull(context(sample = observation.copy(raw = ByteFrame(raw))))
        assertNull(context(sample = observation.copy(hundredthsGram = 21595)))
        assertNull(context(sample = observation.copy(hundredthsGram = 600001)))
    }

    @Test fun inFlightOrUnknownOperationsAndMockCannotChangeTheProfile() {
        for (state in DeviceState.entries.filter { it != DeviceState.READY }) assertNull(context(state = state))
        for (shot in listOf(ExtractionState.STARTING, ExtractionState.RUNNING,
                ExtractionState.STOP_REQUESTED, ExtractionState.OUTCOME_UNKNOWN)) assertNull(context(shot = shot))
        for (tare in listOf(StandaloneTare.State.WRITING, StandaloneTare.State.WAITING_ZERO,
                StandaloneTare.State.UNKNOWN)) assertNull(context(tare = tare))
        assertNull(context(manual = true)); assertNull(context(preparing = true)); assertNull(context(mock = true))
        assertNotNull(context(shot = ExtractionState.ENDED_OBSERVED, tare = StandaloneTare.State.CONFIRMED))
    }

    @Test fun confirmationIsBoundToAddressProtocolAndConnectionEpochAndRechecksCurrentConditions() {
        val captured = context()!!
        fun current(value: FelicitaWeightControlContext?) = FelicitaWeightControlPolicy.stillCurrent(captured, value)
        assertTrue(current(context()))
        assertTrue(current(context(sample = observation.copy(receivedAtMs = 999))))
        assertFalse(current(context(address = "AA:BB:CC:DD:EE:00")))
        assertFalse(current(context(epoch = 4)))
        assertFalse(current(context(protocol = FelicitaReadOnlyScaleProtocolAdapter.id,
            sample = observation.copy(evidence = ScaleEvidence.LIVE_READ_ONLY))))
        assertFalse(current(context(state = DeviceState.DISCONNECTED)))
        assertFalse(current(context(tare = StandaloneTare.State.UNKNOWN)))
        assertFalse(current(context(now = 2401)))
        assertFalse(current(null))
    }
}
