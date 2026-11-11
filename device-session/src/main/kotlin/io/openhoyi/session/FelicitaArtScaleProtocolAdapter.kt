package io.openhoyi.session

/** Compatibility ID for saved V2 ART selections. All behavior and protocol evidence now come
 * from the default writable Felicita adapter; no preference migration or hardware claim. */
object FelicitaArtScaleProtocolAdapter:ScaleProtocolAdapter by FelicitaScaleProtocolAdapter {
    override val id="felicita-art-ascii-verified"
}
