package com.papi.nova.manager

/**
 * Decides when an automatically relaunched stream has recovered, which is what earns the
 * reconnect budget back.
 *
 * One rendered frame is not enough. A stream that renders briefly and then drops again would reset
 * the budget on every relaunch and never reach the terminated dialog, so frames have to keep
 * arriving for [stableMs] first. A performance sample that shows no new frames starts the wait over.
 */
class ReconnectRecoveryTracker(private val stableMs: Long = DEFAULT_STABLE_MS) {
    private var lastFramesRendered = 0L
    private var renderingSinceMs = NOT_RENDERING
    private var recovered = false

    /**
     * Feed every performance sample.
     *
     * @return true exactly once, on the first sample after rendering has held for [stableMs]
     */
    fun onSample(framesRendered: Long, nowMs: Long): Boolean {
        if (recovered) return false

        if (framesRendered > lastFramesRendered) {
            if (renderingSinceMs == NOT_RENDERING) {
                renderingSinceMs = nowMs
            }
        } else {
            renderingSinceMs = NOT_RENDERING
        }
        lastFramesRendered = framesRendered

        if (renderingSinceMs != NOT_RENDERING && nowMs - renderingSinceMs >= stableMs) {
            recovered = true
            return true
        }
        return false
    }

    companion object {
        const val DEFAULT_STABLE_MS = 15_000L
        private const val NOT_RENDERING = -1L
    }
}
