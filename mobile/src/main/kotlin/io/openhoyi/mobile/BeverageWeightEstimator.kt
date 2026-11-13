package io.openhoyi.mobile

enum class BeverageWeightSource { CONFIRMED, ESTIMATED }
internal data class BeverageWeightSample(val hundredthsGram: Int, val source: BeverageWeightSource, val segmentId: Int = 0)

/** Display/recording only. Never sends tare, stops a pump, or grants an extraction permit. */
internal class BeverageWeightEstimator {
    private var segmentId = 0
    private var epoch: Long? = null
    private var baseline: Int? = null
    private var lastSource: BeverageWeightSource? = null
    private var lastFreshRaw: Int? = null
    private var confirmedCoordinatesRevoked = false

    fun observe(elapsedMs: Long, freshRaw: Int?, scaleEpoch: Long, confirmedNet: Int?): BeverageWeightSample? {
        if (epoch != null && epoch != scaleEpoch) {
            epoch = scaleEpoch; baseline = null; lastSource = null; segmentId++
            lastFreshRaw = null; confirmedCoordinatesRevoked = false
            // A reconnect/tare changes coordinates. Keep an explicit gap before a new estimate.
            return null
        }
        epoch = scaleEpoch
        if (freshRaw == null) return null
        val previousRaw = lastFreshRaw
        lastFreshRaw = freshRaw
        if (previousRaw != null && previousRaw > 80 && freshRaw in -50..50) {
            baseline = freshRaw; lastSource = BeverageWeightSource.ESTIMATED; segmentId++
            // A physical zero can change coordinates without an App tare or connection event.
            // Revoke only this display projection; controller proof and control stay untouched.
            confirmedCoordinatesRevoked = true
            return null
        }
        if (confirmedNet != null && !confirmedCoordinatesRevoked) {
            if (lastSource == BeverageWeightSource.ESTIMATED) {
                lastSource = BeverageWeightSource.CONFIRMED; baseline = null; segmentId++
                return null
            }
            lastSource = BeverageWeightSource.CONFIRMED
            return BeverageWeightSample(confirmedNet.coerceAtLeast(0), BeverageWeightSource.CONFIRMED, segmentId)
        }
        if (lastSource == BeverageWeightSource.CONFIRMED) {
            lastSource = null; baseline = null; segmentId++
            return null // Never silently reinterpret lost proof as the same continuous segment.
        }
        // Begin the display-only fallback baseline after 2 s. This local sampling policy
        // neither requests a machine/scale action nor acknowledges a tare.
        if (elapsedMs < 2_000) return null
        if (baseline == null) baseline = freshRaw
        lastSource = BeverageWeightSource.ESTIMATED
        val delta = (freshRaw.toLong() - requireNotNull(baseline)).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        return BeverageWeightSample(delta, BeverageWeightSource.ESTIMATED, segmentId)
    }
}
