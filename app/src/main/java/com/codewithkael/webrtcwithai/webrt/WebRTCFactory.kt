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
import kotlin.coroutines.suspendCoroutine

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

    init {
        initPeerConnectionFactory(application)
        loadDefaultWatermark()
    }
    fun loadDefaultWatermark() {
        appLogoBitmap = BitmapFactory.decodeResource(
            application.resources, R.drawable.youtube_logo
        )
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

                        processed = detectFaceInBitmapAwait(processed)
                        processed = blurSegmentationAwait(processed)
                        processed = drawWatermarkCenter(processed, appLogoBitmap)


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

    suspend fun detectFaceInBitmapAwait(bitmap: Bitmap): Bitmap = suspendCoroutine { cont ->

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
                    box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat()
                )
                canvas.drawOval(ovalRect, paint)
            }

            cont.resume(annotated)
        }.addOnFailureListener {
            cont.resume(bitmap)
        }
    }


    suspend fun blurSegmentationAwait(bitmap: Bitmap): Bitmap = suspendCoroutine { cont ->

        val image = InputImage.fromBitmap(bitmap, 0)

        selfieSegmenter.process(image).addOnSuccessListener { mask ->

            val width = bitmap.width
            val height = bitmap.height

            val maskBuffer = mask.buffer
            maskBuffer.rewind()

            val blurred =
                HokoBlur.with(application).scheme(HokoBlur.SCHEME_NATIVE).mode(HokoBlur.MODE_BOX)
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
        }.addOnFailureListener { e ->
            cont.resume(bitmap)
        }
    }

    fun drawWatermarkCenter(
        frame: Bitmap,
        watermark: Bitmap?
    ): Bitmap {

        if (watermark == null) return frame

        val output = frame.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // calculate center position
        val left = (frame.width - watermark.width) / 2f
        val top = (frame.height - watermark.height) / 2f

        canvas.drawBitmap(watermark, left, top, null)

        return output
    }


}