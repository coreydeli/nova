package com.papi.nova.jni

import com.papi.nova.LimeLog
import com.papi.nova.manager.ConnectionResilienceManager

/**
 * JNI bridge for the native pre-termination callback.
 * Called from moonlight-common-c's connection handler when a stream error occurs,
 * BEFORE the normal Moonlight error dialog is shown.
 *
 * The native side calls onPreTermination(errorCode) via JNI.
 * If this returns true, the error is absorbed (reconnect in progress).
 * If false, normal Moonlight error handling proceeds.
 */
object PolarisNativeHook {

    @Volatile
    private var resilienceManager: ConnectionResilienceManager? = null

    @Synchronized
    fun register(manager: ConnectionResilienceManager) {
        resilienceManager = manager
        LimeLog.info("Nova: Native hook registered")
    }

    /**
     * Clears the hook only while [manager] still owns it. An automatic reconnect starts the next
     * Game before the old one is destroyed, and the old Game must not remove the new Game's hook.
     */
    @Synchronized
    fun unregister(manager: ConnectionResilienceManager?) {
        if (manager != null && resilienceManager === manager) {
            resilienceManager = null
        }
    }

    /**
     * Called from JNI (native C layer) when a connection error occurs.
     * Must be @JvmStatic for JNI to find it.
     *
     * @param errorCode The Moonlight error code (typically -1)
     * @return true if Nova is handling the error (suppress Moonlight's error dialog)
     */
    @JvmStatic
    fun onPreTermination(errorCode: Int): Boolean {
        val manager = resilienceManager ?: return false

        LimeLog.info("Nova: Pre-termination hook fired (error=$errorCode)")
        return manager.shouldAbsorbError(errorCode)
    }
}
