package com.papi.nova.manager

import org.json.JSONObject

/** Typed contract returned over the paired host connection for one assigned profile. */
object WorkerLaunchContract {
    const val SOURCE = "worker_profile_v1"
    const val APP_UUID = "706f6c61-7269-4373-8000-6d756c746973"
    const val APP_ID = 1347244801
    private val profileId = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    data class Contract(val id: String, val width: Int, val height: Int, val fps: Int)

    fun isProfileApp(identity: String?) = identity == APP_UUID || identity == APP_ID.toString()

    fun parse(payload: JSONObject?): Contract? {
        payload ?: return null
        if (payload.opt("source") != SOURCE || payload.opt("status") != true) return null
        val worker = payload.optJSONObject("worker_profile") ?: return null
        val id = worker.opt("id") as? String ?: return null
        if (!profileId.matches(id) || integral(worker.opt("version")) != 1 ||
            worker.opt("app_uuid") != APP_UUID || integral(worker.opt("app_id")) != APP_ID ||
            worker.opt("codec") != "h264" || integral(worker.opt("audio_channels")) != 2) return null
        val profile = payload.optJSONObject("resolved_profile") ?: return null
        if (integral(profile.opt("policy_version")) != 1 || profile.opt("preset") != "worker") return null
        val fields = profile.optJSONObject("fields") ?: return null
        fun field(name: String): Any? {
            val detail = fields.optJSONObject(name) ?: return null
            if (detail.opt("source") != "capability_validation" || detail.opt("locked") != true ||
                detail.opt("normalized") !is Boolean || detail.opt("reason_code") != "worker_media_contract") return null
            return detail.opt("value")
        }
        val width = integral(field("display_width")) ?: return null
        val height = integral(field("display_height")) ?: return null
        val fps = integral(field("target_fps")) ?: return null
        if (width !in 320..4096 || height !in 240..2160 || width % 2 != 0 || height % 2 != 0 ||
            fps !in 15..240 || integral(field("target_bitrate_kbps")) != 8000 ||
            field("hdr") != false || field("preferred_codec") != "h264" ||
            field("display_mode") != "${width}x${height}x${fps}" ||
            payload.optJSONObject("topology_resolution")?.opt("resolved") != "gamescope_stream") return null
        return Contract(id, width, height, fps)
    }

    fun honors(
        payload: JSONObject, appIdentity: String, requestedWidth: Int, requestedHeight: Int,
        requestedFps: Float, clientMaximumFps: Float, displayLocked: Boolean,
        bitrateLocked: Boolean, bitrateCeilingKbps: Int, mirrorDesktop: Boolean,
        forcePrivate: Boolean, encoderBackend: String,
    ): Boolean {
        val contract = parse(payload) ?: return false
        return isProfileApp(appIdentity) && !mirrorDesktop && !forcePrivate &&
            (encoderBackend.isBlank() || encoderBackend == "auto") &&
            requestedFps.isFinite() && requestedFps > 0 &&
            contract.fps <= requestedFps + .5f &&
            clientMaximumFps.isFinite() && clientMaximumFps > 0 &&
            contract.fps <= clientMaximumFps + .5f &&
            (!displayLocked || contract.width == requestedWidth && contract.height == requestedHeight) &&
            (!bitrateLocked || bitrateCeilingKbps >= 8000)
    }

    private fun integral(value: Any?): Int? {
        val number = (value as? Number)?.toDouble() ?: return null
        return number.takeIf { it.isFinite() && it % 1.0 == 0.0 && it in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble() }?.toInt()
    }
}
