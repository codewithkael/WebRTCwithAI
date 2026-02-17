package com.codewithkael.webrtcwithai.utils.imageProcessor

import android.content.Context
import android.graphics.Bitmap

class VideoEffectsPipeline(context: Context) {

    private val faceOval = FaceOvalEffect()
    private val blur = BackgroundBlurEffect(context)
    private val faceMesh = FaceMeshEffect()
    private val watermark = WatermarkEffect()

    private val imageLabeling = ImageLabelingEffect()
    private val objectDetection = ObjectDetectionEffect()
    private val poseDetection = PoseDetectionEffect()

    data class Enabled(
        val faceDetect: Boolean,
        val blurBackground: Boolean,
        val watermark: Boolean,
        val faceMesh: Boolean,
        val imageLabeling: Boolean,
        val objectDetection: Boolean,
        val poseDetection: Boolean
    )

    data class WatermarkParams(
        val bitmap: Bitmap?,
        val location: WatermarkEffect.Location,
        val marginPx: Float,
        val sizeFraction: Float
    )

    suspend fun process(
        input: Bitmap,
        enabled: Enabled,
        wm: WatermarkParams
    ): Bitmap {
        var out = input

        if (enabled.faceDetect) out = faceOval.apply(out)
        if (enabled.blurBackground) out = blur.apply(out)
        if (enabled.faceMesh) out = faceMesh.apply(out)

        if (enabled.poseDetection) out = poseDetection.apply(out)
        if (enabled.objectDetection) out = objectDetection.apply(out)
        if (enabled.imageLabeling) out = imageLabeling.apply(out)

        if (enabled.watermark) {
            out = watermark.apply(
                out,
                WatermarkEffect.Config(
                    watermark = wm.bitmap,
                    location = wm.location,
                    marginPx = wm.marginPx,
                    sizeFraction = wm.sizeFraction
                )
            )
        }

        return out
    }

    fun close() {
        faceOval.close()
        blur.close()
        faceMesh.close()
        imageLabeling.close()
        objectDetection.close()
        poseDetection.close()
    }
}
