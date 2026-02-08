package com.codewithkael.webrtcwithai.utils//package com.codewithkael.simplecall.newUtils
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.face.Face
//import com.google.mlkit.vision.face.FaceDetection
//import com.google.mlkit.vision.face.FaceDetectorOptions
//import kotlinx.coroutines.*
//import kotlinx.coroutines.tasks.await
//import org.webrtc.VideoFrame
//
//
//class VideoProcessor(private val context: Context) {
//
//    private val faceDetectorOptions = FaceDetectorOptions.Builder()
//        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
//        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
//        .build()
//
//    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)
//
//    private var processingJob: Job? = null
//    private var lastDetectedFaces: List<Face> = emptyList()
//
//    fun bitmapToNV21(bitmap: Bitmap): ByteArray {
//        val width = bitmap.width
//        val height = bitmap.height
//        val argb = IntArray(width * height)
//
//        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
//
//        val yuv = ByteArray(
//            height * width + 2 * Math.ceil(height / 2.0).toInt() * Math.ceil(width / 2.0).toInt()
//        )
//        encodeYUV420SP(yuv, argb, width, height)
//
//        return yuv
//    }
//
//    fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
//        val frameSize = width * height
//        var yIndex = 0
//        var uvIndex = frameSize
//        var a: Int
//        var R: Int
//        var G: Int
//        var B: Int
//        var Y: Int
//        var U: Int
//        var V: Int
//        var index = 0
//        for (j in 0 until height) {
//            for (i in 0 until width) {
//                a = argb[index] and -0x1000000 shr 24 // a is not used obviously
//                R = argb[index] and 0xff0000 shr 16
//                G = argb[index] and 0xff00 shr 8
//                B = argb[index] and 0xff shr 0
//                // well known RGB to YUV algorithm
//                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
//                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
//                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
//
//                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
//                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
//                //    pixel AND every other scanline.
//                yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
//                if (j % 2 == 0 && index % 2 == 0) {
//                    yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
//                    yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
//                }
//                index++
//            }
//        }
//    }
//
//    fun processVideoFrame(frame: VideoFrame, callback: (Bitmap) -> Unit) {
//        val yuvFrame = YuvFrame(
//            frame,
//            YuvFrame.Companion.PROCESSING_NONE,
//            frame.timestampNs
//        )
//        val inputFrameBitmap = yuvFrame.bitmap ?: return
//
//        if (processingJob?.isActive != true) {
//            processingJob = CoroutineScope(Dispatchers.IO).launch {
//                val nv21 = bitmapToNV21(inputFrameBitmap)
//                val inputImage = InputImage.fromByteArray(
//                    nv21,
//                    inputFrameBitmap.width,
//                    inputFrameBitmap.height,
//                    0,
//                    InputImage.IMAGE_FORMAT_NV21
//                )
//                //val inputImage = InputImage.fromBitmap(inputFrameBitmap, 0)
//                try {
//                    lastDetectedFaces = detectFaces(inputImage)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        }
//
//        if (lastDetectedFaces.isNotEmpty()) {
//            val annotatedBitmap = drawFaceAnnotations(inputFrameBitmap, lastDetectedFaces)
//            callback(annotatedBitmap)
//        } else {
//            callback(inputFrameBitmap)
//        }
//    }
//
//    private suspend fun detectFaces(image: InputImage): List<Face> =
//        faceDetector.process(image).await()
//
//    private val paint = Paint().apply {
//        color = Color.RED
//        strokeWidth = 1f
//    }
//
////    private fun drawFaceAnnotations(bitmap: Bitmap, faces: List<Face>): Bitmap {
////        val annotatedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
////        val canvas = Canvas(annotatedBitmap)
////        for (face in faces) {
////            val landmarks = face.allLandmarks
////            for (landmark in landmarks) {
////                val position = landmark.position
////                canvas.drawCircle(position.x, position.y, 10f, paint)
////            }
////        }
////        return annotatedBitmap
////    }
////    private fun drawFaceAnnotations(bitmap: Bitmap, faces: List<Face>): Bitmap {
////        val annotatedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
////        val canvas = Canvas(annotatedBitmap)
////
////        val boxPaint = Paint().apply {
////            color = Color.RED
////            style = Paint.Style.STROKE
////            strokeWidth = 1f
////            isAntiAlias = true
////        }
////
////        for (face in faces) {
////            val box = face.boundingBox
////            canvas.drawRect(box, boxPaint)
////        }
////
////        return annotatedBitmap
////    }
//
//    private fun drawFaceAnnotations(bitmap: Bitmap, faces: List<Face>): Bitmap {
//        val annotatedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//        val canvas = Canvas(annotatedBitmap)
//
//        val paint = Paint().apply {
//            color = Color.RED
//            style = Paint.Style.STROKE
//            strokeWidth = 3f
//            isAntiAlias = true
//        }
//
//        for (face in faces) {
//            val box = face.boundingBox
//
//            val ovalRect = android.graphics.RectF(
//                box.left.toFloat(),
//                box.top.toFloat(),
//                box.right.toFloat(),
//                box.bottom.toFloat()
//            )
//
//            canvas.drawOval(ovalRect, paint)
//        }
//
//        return annotatedBitmap
//    }
//
//
//    fun cancelProcessing() {
//        processingJob?.cancel()
//    }
//}