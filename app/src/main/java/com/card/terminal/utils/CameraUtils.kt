package com.card.terminal.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import androidx.core.app.ActivityCompat
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

object CameraUtils {
    private const val IMAGE_WIDTH = 640
    private const val IMAGE_HEIGHT = 480

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private lateinit var imageReader: ImageReader
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image? = reader.acquireLatestImage()
        saveImage(
            image
        )
        image?.close()
        closeCamera()
    }

    fun init(context: Context) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener(imageAvailableListener, null)
        startBackgroundThread()
    }

    fun captureImage(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Camera permission not granted
            return
        }

        val cameraId = getCameraId()
        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraDevice?.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraDevice?.close()
                    cameraDevice = null
                }
            }, null)
        } catch (e: CameraAccessException) {
            Timber.d("Failed to open camera: ${e.message}")
        }
    }

    private fun createCaptureSession() {
        try {
            captureRequestBuilder =
                cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE) ?: return
            captureRequestBuilder.addTarget(imageReader.surface)

            cameraDevice?.createCaptureSession(
                listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureImage()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Timber.d("Failed to configure capture session. | ${session}")
                    }
                }, null
            )
        } catch (e: Exception) {
            Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
        }
    }

    private fun captureImage() {
        try {
            captureSession.capture(captureRequestBuilder.build(), null, null)
        } catch (e: Exception) {
            Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
        }
    }

    private fun saveImage(image: Image?) {
        val imageBuf = image?.planes?.get(0)?.buffer ?: return
        val imageBytes = ByteArray(imageBuf.remaining())
        imageBuf.get(imageBytes)
        val output: FileOutputStream? = null
        try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val imgBytes = outputStream.toByteArray()
            val base64String = Base64.encodeToString(imgBytes, Base64.DEFAULT)
            val prefs = ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("EventImage", base64String)
            editor.commit()
        } catch (e: Exception) {
            Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
        } finally {
            output?.close()
        }
    }

    private fun closeCamera() {
        captureSession.close()
        cameraDevice?.close()
        imageReader.close()
    }

    private fun getCameraId(): String {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
        } catch (e: Exception) {
            Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
        }
        throw IllegalStateException("Failed to find a suitable camera.")
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    fun release() {
        closeCamera()
        stopBackgroundThread()
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Timber.d("Interrupted while waiting for background thread: ${e.message}")
        }
    }
}


