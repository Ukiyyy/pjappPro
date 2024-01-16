package com.example.pjapppro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.example.pjapppro.databinding.ActivityCameraBinding
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.*

class CameraActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraView: JavaCameraView

    private var isRecording = false
    private lateinit var videoRecorder: VideoRecorder

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val permissionRequestCode = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        OpenCVLoader.initDebug()
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionsIfNecessary()

        cameraView = findViewById(R.id.camera_view)
        cameraView.setCvCameraViewListener(this)

        initializeOpenCV()
        configureCamera()

    }

    private fun initializeOpenCV() {
        // Initialize OpenCV or any other necessary initialization steps
    }

    private fun configureCamera() {
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT)
        cameraView.enableView()
    }

    private fun requestPermissionsIfNecessary() {
        val permissionsToRequest = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission(s) denied!", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            }
            // All permissions granted, continue with the app initialization
        }
    }

    override fun onResume() {
        super.onResume()
        cameraView.enableView()
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        videoRecorder = VideoRecorder()
    }

    override fun onCameraViewStopped() {
        videoRecorder.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val frame = inputFrame?.rgba()

        if (!::videoRecorder.isInitialized) {
            videoRecorder = VideoRecorder()
        }

        if (isRecording) {
            if (frame != null) {
                videoRecorder.record(frame)
            }
        }

        return frame!!
    }

    private inner class VideoRecorder {
        private val videoFrames = mutableListOf<ByteArray>()
        private val maxRecordingTime: Long = 5000 // 5 seconds in milliseconds
        private var recordingStartTime: Long = 0

        fun record(frame: Mat) {
            if (!isRecording) {
                recordingStartTime = System.currentTimeMillis()
                isRecording = true

                // Schedule a task to stop recording after the specified time
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    stopRecording()
                }, maxRecordingTime)
            }

            val frameData = ByteArray((frame.total() * frame.elemSize().toInt()).toInt())
            frame.get(0, 0, frameData)
            videoFrames.add(frameData.clone())

            Toast.makeText(this@CameraActivity, "Recording started", Toast.LENGTH_SHORT).show()
        }

        private fun stopRecording() {
            isRecording = false
            val videoByteArray = concatenateByteArrays(videoFrames)
            Toast.makeText(this@CameraActivity, "Recording stopped", Toast.LENGTH_SHORT).show()

            // Pass the videoByteArray to your processing logic
            processVideoByteArray(videoByteArray)
        }

        private fun processVideoByteArray(videoByteArray: ByteArray) {
            val py = Python.getInstance()
            val pyObject = py.getModule("hello")
            val result = pyObject.callAttr("executeFaceRecognition",videoByteArray)
            val message = result.toString()
            Toast.makeText(this@CameraActivity, message, Toast.LENGTH_SHORT).show()
        }

        private fun concatenateByteArrays(byteArrays: List<ByteArray>): ByteArray {
            val totalSize = byteArrays.sumOf { it.size }
            val concatenatedArray = ByteArray(totalSize)
            var currentIndex = 0

            for (byteArray in byteArrays) {
                System.arraycopy(byteArray, 0, concatenatedArray, currentIndex, byteArray.size)
                currentIndex += byteArray.size
            }

            return concatenatedArray
        }

        fun release() {
            videoFrames.clear()
        }
    }
}
