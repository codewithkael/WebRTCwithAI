package com.codewithkael.webrtcwithai.webrt

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.codewithkael.simplecall.newUtils.BitmapToVideoFrameConverter
import com.codewithkael.simplecall.newUtils.YuvFrame
import com.codewithkael.webrtcwithai.R
import com.codewithkael.webrtcwithai.utils.MyApplication
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.hoko.blur.HokoBlur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.CapturerObserver
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import androidx.core.graphics.scale
import com.codewithkael.webrtcwithai.utils.FilterStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import androidx.core.net.toUri


@Singleton
class WebRTCFactory @Inject constructor(
    private val application: Application
) {
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val selfieSegmenter by lazy {
        val options =
            SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
                .build()

        Segmentation.getClient(options)
    }

//    private val iceServer = listOf<IceServer>(
//        IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer()
//    )
private val faceMeshDetector: FaceMeshDetector by lazy {
    FaceMeshDetection.getClient()
}

    suspend fun detectFaceMeshInBitmapAwait(bitmap: Bitmap): Bitmap =
        suspendCancellableCoroutine { cont ->

            val inputImage = InputImage.fromBitmap(bitmap, 0)

            faceMeshDetector.process(inputImage)
                .addOnSuccessListener { meshes: List<FaceMesh> ->
                    if (meshes.isEmpty()) {
                        cont.resume(bitmap); return@addOnSuccessListener
                    }

                    val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(out)

                    val pointPaint = Paint().apply {
                        color = Color.GREEN
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }

                    // You can tune these
                    val r = maxOf(1f, bitmap.width / 360f) // scale dot size with resolution

                    for (mesh in meshes) {
                        // FaceMesh provides points; we’ll draw dots for a nice “mesh” effect
                        for (p: FaceMeshPoint in mesh.allPoints) {
                            canvas.drawCircle(p.position.x, p.position.y, r, pointPaint)
                        }
                    }

                    cont.resume(out)
                }
                .addOnFailureListener {
                    cont.resume(bitmap)
                }
        }


    private val iceServer = listOf(
        IceServer.builder("turn:95.217.13.89:3478").setUsername("user")
            .setPassword("password").createIceServer(),
        IceServer.builder("stun:95.217.13.89:3478").createIceServer(),
        IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer(),
        IceServer.builder("turn:164.92.142.241:3478?transport=udp")
            .setUsername("username1").setPassword("key1").createIceServer(),
        // TURN server with TLS
        IceServer.builder("turns:164.92.142.241:5349?transport=tcp")
            .setUsername("username1").setPassword("key1").createIceServer(),
        IceServer.builder("turn:164.92.142.251:3478").setUsername("username1")
            .setPassword("key1").createIceServer(), //
        IceServer.builder("turn:global.relay.metered.ca:80")
            .setUsername("0da9dc3f3ca0b8aef7388ca9").setPassword("KuuHVTmXU80Q1WMO")
            .createIceServer(),
        IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
            .setUsername("0da9dc3f3ca0b8aef7388ca9").setPassword("KuuHVTmXU80Q1WMO")
            .createIceServer(),
        IceServer.builder("turn:global.relay.metered.ca:443")
            .setUsername("0da9dc3f3ca0b8aef7388ca9").setPassword("KuuHVTmXU80Q1WMO")
            .createIceServer(),
        IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
            .setUsername("0da9dc3f3ca0b8aef7388ca9").setPassword("KuuHVTmXU80Q1WMO")
            .createIceServer(),
        IceServer.builder("turn:global.relay.metered.ca:80")
            .setUsername("your-username").setPassword("your-password").createIceServer(),
        IceServer.builder("turn:13.250.13.83:3478?transport=udp")
            .setUsername("YzYNCouZM1mhqhmseWk6").setPassword("YzYNCouZM1mhqhmseWk6")
            .createIceServer(),
        IceServer.builder("turn:numb.viagenie.ca").setUsername("webrtc@live.com")
            .setPassword("muazkh").createIceServer(),
        IceServer.builder("turn:freestun.net:3478").setUsername("free")
            .setPassword("free").createIceServer()
    )


    private var videoCapture: CameraVideoCapturer? = null
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }

    private val streamId = "${MyApplication.UserID}_stream"
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var appLogoBitmap: Bitmap? = null
    private var watermarkLocation: WatermarkLocation = WatermarkLocation.BOTTOM_LEFT
    private var watermarkMarginDp: Float = 12f
    private var watermarkSizeFraction: Float = 0.20f
    private var filterFaceDetect: Boolean = false
    private var filterBlurBackground: Boolean = false
    private var filterWatermark: Boolean = false
    @Volatile private var filterFaceMesh: Boolean = false


    init {
        initPeerConnectionFactory(application)
        reloadWatermarkConfig()
        reloadFiltersConfig()
    }

    fun reloadWatermarkConfig() {
        val cfg = com.codewithkael.webrtcwithai.utils.WatermarkStorage.load(application)

        watermarkLocation = cfg.location
        watermarkMarginDp = cfg.marginDp
        watermarkSizeFraction = cfg.sizeFraction

        // load bitmap from persisted uri if exists; fallback to default resource
        val uriStr = cfg.uri
        if (uriStr.isNullOrBlank()) {
            appLogoBitmap = BitmapFactory.decodeResource(application.resources, R.drawable.youtube_logo)
            return
        }

        val uri = uriStr.toUri()
        val bmp = runCatching {
            application.contentResolver.openInputStream(uri).use { input ->
                if (input != null) BitmapFactory.decodeStream(input) else null
            }
        }.getOrNull()

        appLogoBitmap = bmp ?: BitmapFactory.decodeResource(application.resources, R.drawable.youtube_logo)
    }

    fun reloadFiltersConfig() {
        val cfg = FilterStorage.load(application)
        filterFaceDetect = cfg.faceDetect
        filterBlurBackground = cfg.blurBackground
        filterWatermark = cfg.watermark
        filterFaceMesh = cfg.faceMesh
    }


    fun prepareLocalStream(
        view: SurfaceViewRenderer,
    ) {
        initSurfaceView(view)
        startLocalVideo(view)
    }

    fun initSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
    }

    private fun initPeerConnectionFactory(application: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder().setVideoDecoderFactory(
            DefaultVideoDecoderFactory(eglBaseContext)
        ).setVideoEncoderFactory(
            DefaultVideoEncoderFactory(
                eglBaseContext, true, true
            )
        ).setOptions(PeerConnectionFactory.Options().apply {
            disableEncryption = false
            disableNetworkMonitor = false
        }).createPeerConnectionFactory()
    }

    private fun startLocalVideo(surface: SurfaceViewRenderer) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, eglBaseContext)
        videoCapture = getVideoCapture()
//        videoCapture?.initialize(
//            surfaceTextureHelper, surface.context, localVideoSource.capturerObserver
//        )
        videoCapture?.initialize(
            surfaceTextureHelper, surface.context, object : CapturerObserver {
                override fun onCapturerStarted(success: Boolean) {}

                override fun onCapturerStopped() {}

                override fun onFrameCaptured(frame: VideoFrame) {

                    val yuv = YuvFrame(frame, YuvFrame.PROCESSING_NONE, frame.timestampNs)
                    val bitmap = yuv.bitmap ?: return

                    CoroutineScope(Dispatchers.Default).launch {

                        var processed = bitmap

                        if (filterFaceDetect) {
                            processed = detectFaceInBitmapAwait(processed)
                        }

                        if (filterBlurBackground) {
                            processed = blurSegmentationAwait(processed)
                        }

                        if (filterWatermark) {
                            val density = application.resources.displayMetrics.density
                            val marginPx = watermarkMarginDp * density

                            processed = drawWatermark(
                                frame = processed,
                                watermark = appLogoBitmap,
                                location = watermarkLocation,
                                marginPx = marginPx,
                                sizeFraction = watermarkSizeFraction
                            )
                        }

                        if (filterFaceMesh) {
                            processed = detectFaceMeshInBitmapAwait(processed)
                        }

                        val videoFrame =
                            BitmapToVideoFrameConverter.convert(processed, 0, System.nanoTime())

                        withContext(Dispatchers.Main) {
                            localVideoSource.capturerObserver.onFrameCaptured(videoFrame)
                        }
                    }
                }

            })

        videoCapture?.startCapture(720, 480, 10)
        localVideoTrack =
            peerConnectionFactory.createVideoTrack(streamId + "_video", localVideoSource)
        localVideoTrack?.addSink(surface)
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(streamId + "_audio", localAudioSource)
    }

    private fun getVideoCapture(): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }


    fun onDestroy() {
        runCatching {
            // Stop capturing video
            videoCapture?.stopCapture()
            videoCapture?.dispose()

            // Mute and dispose of the audio track
            localAudioTrack?.let {
                it.setEnabled(false) // Disable the track to stop mic input
                it.dispose()
            }

            // Dispose of the video track
            localVideoTrack?.dispose()

            // Dispose of the local media stream
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun createRTCClient(
        observer: PeerConnection.Observer, listener: RTCClientImpl.TransferDataToServerCallback
    ): RTCClient? {
        val connection = peerConnectionFactory.createPeerConnection(
            PeerConnection.RTCConfiguration(iceServer), observer
        )
        localVideoTrack?.let {
            connection?.addTrack(it)
        }
        localAudioTrack?.let {
            connection?.addTrack(it)
        }
        return connection?.let { RTCClientImpl(it, listener) }
    }


    private val faceDetector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE).build()
        )
    }

    suspend fun detectFaceInBitmapAwait(bitmap: Bitmap): Bitmap =
        suspendCancellableCoroutine { cont ->

            val inputImage = InputImage.fromBitmap(bitmap, 0)

            faceDetector.process(inputImage).addOnSuccessListener { faces ->

                if (faces.isEmpty()) {
                    cont.resume(bitmap)
                    return@addOnSuccessListener
                }

                val annotated = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(annotated)

                val paint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    isAntiAlias = true
                }

                for (face in faces) {
                    val box = face.boundingBox
                    val ovalRect = RectF(
                        box.left.toFloat(),
                        box.top.toFloat(),
                        box.right.toFloat(),
                        box.bottom.toFloat()
                    )
                    canvas.drawOval(ovalRect, paint)
                }

                cont.resume(annotated)
            }.addOnFailureListener {
                cont.resume(bitmap)
            }
        }


    suspend fun blurSegmentationAwait(bitmap: Bitmap): Bitmap =
        suspendCancellableCoroutine { cont ->

            val image = InputImage.fromBitmap(bitmap, 0)

            selfieSegmenter.process(image).addOnSuccessListener { mask ->

                val width = bitmap.width
                val height = bitmap.height

                val maskBuffer = mask.buffer
                maskBuffer.rewind()

                val blurred =
                    HokoBlur.with(application).scheme(HokoBlur.SCHEME_NATIVE)
                        .mode(HokoBlur.MODE_BOX)
                        .radius(width / 32).sampleFactor(2f).forceCopy(true).blur(bitmap)

                val output = createBitmap(width, height)

                val total = width * height
                val maskArray = FloatArray(total)
                val finalPixels = IntArray(total)

                maskBuffer.asFloatBuffer().get(maskArray)

                val origPix = IntArray(total)
                val blurPix = IntArray(total)

                bitmap.getPixels(origPix, 0, width, 0, 0, width, height)
                blurred.getPixels(blurPix, 0, width, 0, 0, width, height)

                for (i in 0 until total) {
                    val c = maskArray[i]
                    finalPixels[i] = if (c > 0.6f) origPix[i] else blurPix[i]
                }

                output.setPixels(finalPixels, 0, width, 0, 0, width, height)
                cont.resume(output)
            }.addOnFailureListener { _ ->
                cont.resume(bitmap)
            }
        }

    fun drawWatermark(
        frame: Bitmap,
        watermark: Bitmap?,
        location: WatermarkLocation,
        marginPx: Float = 0f,        // distance from edges; for CENTER this becomes "drop down"
        sizeFraction: Float = 0.10f  // 0.10f => fits inside 10% of frame width & 10% of frame height
    ): Bitmap {
        if (watermark == null) return frame

        val clampedSize = sizeFraction.coerceIn(0.01f, 1.0f)

        // Target bounding box for watermark
        val targetW = (frame.width * clampedSize).toInt().coerceAtLeast(1)
        val targetH = (frame.height * clampedSize).toInt().coerceAtLeast(1)

        // Scale watermark to FIT inside targetW x targetH (keeps aspect ratio)
        val scale = minOf(
            targetW.toFloat() / watermark.width.toFloat(),
            targetH.toFloat() / watermark.height.toFloat()
        )

        val scaledW = (watermark.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (watermark.height * scale).toInt().coerceAtLeast(1)

        val scaledWatermark =
            if (scaledW == watermark.width && scaledH == watermark.height) watermark
            else watermark.scale(scaledW, scaledH)

        val output = frame.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val left: Float
        val top: Float

        when (location) {
            WatermarkLocation.TOP_LEFT -> {
                left = marginPx
                top = marginPx
            }
            WatermarkLocation.TOP_RIGHT -> {
                left = frame.width - scaledW - marginPx
                top = marginPx
            }
            WatermarkLocation.BOTTOM_LEFT -> {
                left = marginPx
                top = frame.height - scaledH - marginPx
            }
            WatermarkLocation.BOTTOM_RIGHT -> {
                left = frame.width - scaledW - marginPx
                top = frame.height - scaledH - marginPx
            }
            WatermarkLocation.CENTER -> {
                left = (frame.width - scaledW) / 2f
                // "drop" the center watermark down by marginPx
                top = (frame.height - scaledH) / 2f + marginPx
            }
        }

        // Clamp so it never draws outside the frame
        val safeLeft = left.coerceIn(0f, (frame.width - scaledW).toFloat())
        val safeTop = top.coerceIn(0f, (frame.height - scaledH).toFloat())

        canvas.drawBitmap(scaledWatermark, safeLeft, safeTop, null)
        return output
    }


    enum class WatermarkLocation {
        TOP_LEFT,
        TOP_RIGHT,
        CENTER,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }


}