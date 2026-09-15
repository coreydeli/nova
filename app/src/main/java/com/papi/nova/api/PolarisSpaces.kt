package com.papi.nova.api

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import org.json.JSONObject
import java.io.StringReader

/** Only the paired device's permitted Spaces. Names and activity never confer permission. */
data class PolarisSpace(val id: String, val name: String, val state: String, val selected: Boolean, val libraryEnabled: Boolean = false)
data class PolarisSpaces(val enabled: Boolean, val available: Boolean, val canSwitch: Boolean,
    val selectedId: String, val spaces: List<PolarisSpace>, val desktopAllowed: Boolean = false) {
    val selected: PolarisSpace? get() = spaces.singleOrNull { it.selected }
    companion object {
        private val idPattern = Regex("[A-Za-z0-9_-]{1,128}")
        private val states = setOf("ready", "starting", "running", "stopping", "in_use", "unavailable")
        fun parse(payload: String): PolarisSpaces? = runCatching {
            require(payload.toByteArray(Charsets.UTF_8).size <= 2 * 1024 * 1024)
            // JSONObject on Android accepts duplicate keys. Reject ambiguous documents first.
            JsonReader(StringReader(payload)).use { reader ->
                fun visit(depth: Int) {
                    require(depth <= 4)
                    when (reader.peek()) {
                        JsonToken.BEGIN_OBJECT -> { reader.beginObject(); val keys = mutableSetOf<String>()
                            while (reader.hasNext()) { require(keys.add(reader.nextName())); visit(depth + 1) }; reader.endObject() }
                        JsonToken.BEGIN_ARRAY -> { reader.beginArray(); while (reader.hasNext()) visit(depth + 1); reader.endArray() }
                        else -> reader.skipValue()
                    }
                }
                visit(0); require(reader.peek() == JsonToken.END_DOCUMENT)
            }
            val json = JSONObject(payload)
            require(json.opt("schema") is Int && json.getInt("schema") == 1 && json.opt("status") == true)
            fun flag(key: String): Boolean { require(json.opt(key) is Boolean); return json.getBoolean(key) }
            val enabled = flag("enabled"); val available = flag("available"); val canSwitch = flag("can_switch")
            require(json.opt("selected_space_id") is String)
            val selectedId = json.getString("selected_space_id")
            val array = json.getJSONArray("spaces"); require(array.length() <= 4096)
            val spaces = (0 until array.length()).map { index ->
                val entry = array.getJSONObject(index)
                require(listOf("id", "name", "state").all { entry.opt(it) is String } && entry.opt("selected") is Boolean)
                val id = entry.getString("id"); val name = entry.getString("name"); val state = entry.getString("state")
                require(id != "desktop" && idPattern.matches(id) && name.isNotBlank() && name.toByteArray(Charsets.UTF_8).size <= 128 &&
                    name.none { it.code < 32 || it.code == 127 } && state in states)
                require(!entry.has("library_enabled") || entry.opt("library_enabled") is Boolean)
                PolarisSpace(id, name, state, entry.getBoolean("selected"), entry.optBoolean("library_enabled", false))
            }
            require(spaces.map { it.id }.toSet().size == spaces.size)
            require(!available || enabled)
            require(!canSwitch || available)
            require(!json.has("desktop_allowed") || json.opt("desktop_allowed") is Boolean)
            val desktopAllowed = json.optBoolean("desktop_allowed", false)
            require(if (available) {
                if (selectedId == "desktop") desktopAllowed && spaces.none { it.selected }
                else spaces.count { it.selected } == 1 && spaces.single { it.selected }.id == selectedId
            } else selectedId.isEmpty() && spaces.none { it.selected })
            PolarisSpaces(enabled, available, canSwitch, selectedId, spaces, desktopAllowed)
        }.getOrNull()
    }
}
