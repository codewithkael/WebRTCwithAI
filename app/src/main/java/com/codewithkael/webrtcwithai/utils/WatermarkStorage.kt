package com.codewithkael.webrtcwithai.utils

import android.content.Context
import com.codewithkael.webrtcwithai.webrt.WebRTCFactory
import androidx.core.content.edit

object WatermarkStorage {
    private const val PREF = "wm_pref"
    private const val KEY_URI = "wm_uri"
    private const val KEY_LOC = "wm_loc"
    private const val KEY_MARGIN = "wm_margin_dp"
    private const val KEY_SIZE = "wm_size_fraction"

    data class Config(
        val uri: String?,
        val location: WebRTCFactory.WatermarkLocation,
        val marginDp: Float,
        val sizeFraction: Float
    )

    fun load(ctx: Context): Config {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val uri = sp.getString(KEY_URI, null)
        val locStr = sp.getString(KEY_LOC, WebRTCFactory.WatermarkLocation.BOTTOM_LEFT.name)
        val loc = runCatching { WebRTCFactory.WatermarkLocation.valueOf(locStr!!) }
            .getOrDefault(WebRTCFactory.WatermarkLocation.BOTTOM_LEFT)
        val marginDp = sp.getFloat(KEY_MARGIN, 12f)
        val sizeFraction = sp.getFloat(KEY_SIZE, 0.20f)
        return Config(uri, loc, marginDp, sizeFraction)
    }

    fun save(ctx: Context, cfg: Config) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_URI, cfg.uri)
                    .putString(KEY_LOC, cfg.location.name)
                    .putFloat(KEY_MARGIN, cfg.marginDp)
                    .putFloat(KEY_SIZE, cfg.sizeFraction)
            }
    }
}
