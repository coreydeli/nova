package com.papi.nova.nvstream

import com.papi.nova.nvstream.http.HostHttpResponseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HostRefusalTest {

    @Test
    fun aHostThatNamedItsRefusalGetsASheetWithMessageActionAndCode() {
        val refusal = HostRefusal.from(
            HostHttpResponseException(
                503,
                "No video encoder could start against the private stream compositor. On NVIDIA, pick Private Stream (GPU-native).",
                "encoder_probe_failed",
                "On NVIDIA, pick Private Stream (GPU-native).",
            ),
        )

        assertEquals(
            HostRefusal(503, "encoder_probe_failed", "No video encoder could start against the private stream compositor.", "On NVIDIA, pick Private Stream (GPU-native)."),
            refusal,
        )
        assertEquals(
            "No video encoder could start against the private stream compositor.\n\n" +
                "On NVIDIA, pick Private Stream (GPU-native).\n\n" +
                "Host said: encoder_probe_failed (error 503)",
            refusal!!.describe(),
        )
    }

    @Test
    fun anActionIsOptional() {
        val refusal = HostRefusal.from(HostHttpResponseException(503, "The previous session is still stopping.", "session_stopping", null))

        assertEquals("The previous session is still stopping.\n\nHost said: session_stopping (error 503)", refusal!!.describe())
    }

    @Test
    fun aBareStatusIsNotARefusalWorthASheet() {
        // Sunshine and older Polaris hosts send status_message only; the generic dialog stays.
        assertNull(HostRefusal.from(HostHttpResponseException(503, "Failed to initialize video capture/encoding.")))
        assertNull(HostRefusal.from(HostHttpResponseException(503, "x", "", "do something")))
    }
}
