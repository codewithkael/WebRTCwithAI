package com.codewithkael.webrtcwithai.utils

import android.content.Context
import androidx.core.content.edit

object FilterStorage {
    private const val PREF = "filters_pref"
    private const val KEY_FACE = "flt_face"
    private const val KEY_BLUR = "flt_blur"
    private const val KEY_WATERMARK = "flt_watermark"
    private const val KEY_FACE_MESH = "flt_face_mesh"

    data class Config(
        val faceDetect: Boolean,
        val blurBackground: Boolean,
        val watermark: Boolean,
        val faceMesh: Boolean
    )

    fun load(ctx: Context): Config {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return Config(
            faceDetect = sp.getBoolean(KEY_FACE, true),
            blurBackground = sp.getBoolean(KEY_BLUR, true),
            watermark = sp.getBoolean(KEY_WATERMARK, true),
            faceMesh = sp.getBoolean(KEY_FACE_MESH, false)
        )
    }

    fun save(ctx: Context, cfg: Config) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_FACE, cfg.faceDetect)
                    .putBoolean(KEY_BLUR, cfg.blurBackground)
                    .putBoolean(KEY_WATERMARK, cfg.watermark)
                    .putBoolean(KEY_FACE_MESH, cfg.faceMesh)
            }
    }
}
