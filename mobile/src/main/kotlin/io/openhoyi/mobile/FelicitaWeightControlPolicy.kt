package io.openhoyi.mobile

import io.openhoyi.protocol.DecodeResult
import io.openhoyi.protocol.FelicitaNotification
import io.openhoyi.protocol.FelicitaNotificationCodec
import io.openhoyi.protocol.ScaleObservation
import io.openhoyi.session.DeviceConnectionGate
import io.openhoyi.session.DeviceState
import io.openhoyi.session.ExtractionState
import io.openhoyi.session.FelicitaReadOnlyScaleProtocolAdapter
import io.openhoyi.session.FelicitaScaleProtocolAdapter
import io.openhoyi.session.ScaleReadingPolicy
import io.openhoyi.session.ScaleSelectionPolicy
import io.openhoyi.session.StandaloneTare
import io.openhoyi.session.observationEvidence
import java.util.Locale

/** An ephemeral confirmation, never a stored authorization or a device address in diagnostics. */
internal data class FelicitaWeightControlContext(val address: String, val protocolId: String, val epoch: Long) {
    override fun toString() = "FelicitaWeightControlContext([redacted])"
}

/** Only an explicit choice for the currently connected, freshly reporting Felicita can enable ART. */
internal object FelicitaWeightControlPolicy {
    fun context(address: String?, protocolId: String?, epoch: Long, state: DeviceState,
        observation: ScaleObservation?, now: Long, shot: ExtractionState, manualShot: Boolean,
        preparationActive: Boolean, tare: StandaloneTare.State, mock: Boolean): FelicitaWeightControlContext? {
        if (mock || address == null || protocolId !in setOf(FelicitaScaleProtocolAdapter.id,
                FelicitaReadOnlyScaleProtocolAdapter.id) || state != DeviceState.READY ||
            !DeviceConnectionGate.mayChangeScale(shot) || manualShot || preparationActive ||
            tare in setOf(StandaloneTare.State.WRITING, StandaloneTare.State.WAITING_ZERO, StandaloneTare.State.UNKNOWN)) return null
        val sample = observation ?: return null
        val selection = ScaleSelectionPolicy.remembered(address.uppercase(Locale.ROOT), protocolId) ?: return null
        val adapter = ScaleSelectionPolicy.adapter(selection.protocolId) ?: return null
        if (!sample.capabilities.weight || sample.evidence != adapter.observationEvidence ||
            !ScaleReadingPolicy.isFresh(sample.hundredthsGram, sample.receivedAtMs, now)) return null
        val decoded = FelicitaNotificationCodec.decode(sample.raw.toByteArray())
        if (decoded !is DecodeResult.Valid || decoded.value.unit != FelicitaNotification.Unit.GRAM ||
            decoded.value.signedHundredths != sample.hundredthsGram) return null
        return FelicitaWeightControlContext(selection.address, selection.protocolId, epoch)
    }

    fun stillCurrent(expected: FelicitaWeightControlContext, current: FelicitaWeightControlContext?): Boolean =
        current != null && expected == current
}
