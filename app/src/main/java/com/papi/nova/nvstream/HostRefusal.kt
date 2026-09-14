package com.papi.nova.nvstream

import com.papi.nova.nvstream.http.HostHttpResponseException

/**
 * A launch the host refused and explained.
 *
 * Polaris records why every refused launch failed and sends it as the status
 * message plus error_code and error_action; before that, all of them reached
 * the couch as "error 503". This is what the launch sheet shows when the host
 * said something, and it is absent when the host did not, so Sunshine and
 * older hosts keep the generic text.
 */
data class HostRefusal(
    val status: Int,
    val code: String,
    val message: String,
    val action: String?,
) {
    /** The sheet text: the host's message, its fix, and the code a support thread can search for. */
    fun describe(): String {
        val builder = StringBuilder(message.trim())
        val fix = action?.trim().orEmpty()
        if (fix.isNotEmpty()) {
            builder.append("\n\n").append(fix)
        }
        builder.append("\n\nHost said: ").append(code).append(" (error ").append(status).append(")")
        return builder.toString()
    }

    companion object {
        /** Null unless the host named its refusal; a bare status carries nothing worth a sheet. */
        @JvmStatic
        fun from(exception: HostHttpResponseException): HostRefusal? {
            val code = exception.getHostCode()?.trim().orEmpty()
            if (code.isEmpty()) {
                return null
            }
            // The host's status_message is the message followed by the action; show the
            // message alone above the action so the fix is not read twice.
            val action = exception.getHostAction()?.trim()?.takeIf { it.isNotEmpty() }
            var message = exception.getErrorMessage().trim()
            if (action != null && message.endsWith(action)) {
                message = message.removeSuffix(action).trimEnd()
            }
            return HostRefusal(exception.getErrorCode(), code, message, action)
        }
    }
}
