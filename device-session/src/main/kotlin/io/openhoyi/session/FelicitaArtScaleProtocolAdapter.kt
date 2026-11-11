package io.openhoyi.session

import io.openhoyi.protocol.*

/** Explicit ART profile backed by captured FFE1 gram notifications and manual tare/zero evidence.
 * The 54 command comes from the legacy implementation/static mapping, not captured write bytes.
 * The five trailing status bytes remain opaque; no timer, battery or automatic tare is claimed. */
object FelicitaArtScaleProtocolAdapter:ScaleProtocolAdapter {
    override val id="felicita-art-ascii-verified"
    override val capabilities=ScaleCapabilities(tare=true,validatedWeightControl=true)
    override val notifyEndpoint=FelicitaReadOnlyScaleProtocolAdapter.notifyEndpoint
    override val writeEndpoint=notifyEndpoint
    override val initializationCommands=emptyList<TimedCommand>()
    override val tareCommand get()=FelicitaLiveCodec.tare()
    override val transportVerified=true
    override fun permitsWrite(operation:GattOperation.Write):Boolean =
        operation.endpoint==writeEndpoint && operation.bytes.contentEquals(tareCommand.frame.toByteArray())
    override fun decode(bytes:ByteArray,receivedAtMs:Long):DecodeResult<ScaleObservation> =
        newDecoder()(bytes,receivedAtMs)
    override fun newDecoder(onEvent:(ScaleProtocolEvent)->Unit):(ByteArray,Long)->DecodeResult<ScaleObservation> = {bytes,at->
        when(val result=FelicitaNotificationCodec.decode(bytes)) {
            is DecodeResult.Valid->if(result.value.unit==FelicitaNotification.Unit.GRAM)
                DecodeResult.Valid(ScaleObservation(result.value.signedHundredths,at,capabilities,
                    ScaleEvidence.VERIFIED_TRANSPORT,raw=result.value.raw))
                else {onEvent(ScaleProtocolEvent.ReadingInvalidated);DecodeResult.Unknown(result.value.raw)}
            is DecodeResult.Invalid->{onEvent(ScaleProtocolEvent.ReadingInvalidated);result}
            is DecodeResult.Unknown->{onEvent(ScaleProtocolEvent.ReadingInvalidated);result}
        }
    }
}
