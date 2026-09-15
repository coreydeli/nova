package com.papi.nova.nvstream.http

import com.papi.nova.shared.polaris.model.PolarisGame
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NvAppEmulatorMetadataTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun emulatorEntriesCarryTheHostLabelsIntoTheLegacyGrid() {
        val game = json.decodeFromString(
            PolarisGame.serializer(),
            """
            {"id":"g1","app_id":7,"name":"Game One","source":"emulator","platform":"switch","runtime":"eden","platform_label":"Nintendo Switch","runtime_label":"Eden"}
            """.trimIndent()
        )
        val app = NvApp("Game One")
        assertTrue(app.applyPolarisMetadata(game))
        assertEquals("Emulator", app.sourceLabel)
        assertEquals("Nintendo Switch", app.platformLabel)
        assertEquals("Eden", app.runtimeLabel)
        assertEquals("Emulator · Nintendo Switch · Eden", app.metadataLabel)
        assertFalse(app.applyPolarisMetadata(game))
    }

    @Test
    fun mappedValuesStillWinTheirOwnLabelsWithoutAServerLabel() {
        val game = json.decodeFromString(
            PolarisGame.serializer(),
            """
            {"id":"g3","app_id":9,"name":"Hades","source":"lutris","platform":"windows","runtime":"wine"}
            """.trimIndent()
        )
        val app = NvApp("Hades")
        app.applyPolarisMetadata(game)
        assertEquals("Lutris · Windows · Wine", app.metadataLabel)
    }
}
