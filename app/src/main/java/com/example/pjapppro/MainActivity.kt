package com.example.pjapppro

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.pjapppro.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var videoUri: Uri

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val REQUEST_VIDEO_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        // Chaquopy integration
        Python.start(AndroidPlatform(this))

        binding.buttonLogin.setOnClickListener {
            val intentLogin = Intent(this, LoginActivity::class.java)
            startActivity(intentLogin)
        }
        binding.buttonRegister.setOnClickListener {
            val intentRegister = Intent(this, RegisterActivity::class.java)
            startActivity(intentRegister)
        }
        binding.buttonFaceLogin.setOnClickListener {
            if (isCameraPermissionGranted()) {
                dispatchTakeVideoIntent()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakeVideoIntent()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.TITLE, "New Video")
                    put(MediaStore.Video.Media.DESCRIPTION, "From the Camera")
                }
                videoUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)!!
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
                takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 2)
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {

            val client = OkHttpClient.Builder()
                .readTimeout(240, TimeUnit.SECONDS)
                .build()
            val mediaType = "video/mp4".toMediaTypeOrNull()

            // Get the actual path of video file in device storage
            val videoPath = getRealPathFromURI(videoUri)
            val videoFile = File(videoPath)

                val requestBody: RequestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "video",
                        videoFile.name,
                        videoFile.asRequestBody(mediaType)
                    )
                    .build()

                val request = Request.Builder()
                    .url("http://192.168.0.13/rainProjekt/api.php/video")
                    .post(requestBody)
                    .build()

                Log.d("NETWORK_REQUEST", "Sending network request")
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.d("NETWORK_REQUEST", "Network request failed", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.d("NETWORK_REQUEST", "Received response from server")
                        if (!response.isSuccessful) {
                            Log.d("NETWORK_REQUEST", "Unsuccessful response, code: ${response.code}")
                            throw IOException("Unexpected code $response")
                        } else {
                            val responseBody = response.body?.string()
                            Log.d("NETWORK_REQUEST", "Response from server: $responseBody")
                            if (responseBody == "Success") {
                                runOnUiThread {
                                    val rowsDeleted = contentResolver.delete(videoUri, null, null)
                                    if (rowsDeleted > 0) {
                                        Log.d("VIDEO_CAPTURE", "Video file deleted")
                                    } else {
                                        Log.d("VIDEO_CAPTURE", "Failed to delete video file")
                                    }
                                    val intent = Intent(this@MainActivity, OpenActivity::class.java)
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                })
        }
    }

    private fun getRealPathFromURI(contentURI: Uri): String {
        val result: String
        val cursor = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.path.toString()
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

}
