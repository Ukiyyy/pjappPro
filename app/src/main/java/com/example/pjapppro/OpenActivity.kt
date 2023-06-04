package com.example.pjapppro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.pjapppro.databinding.ActivityOpenBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import java.util.zip.ZipFile
import kotlin.collections.HashMap




import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


class OpenActivity : AppCompatActivity() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityOpenBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var username: String


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionCode = 1


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        username = sharedPreferences.getString("username", "")?:""

        binding.button.setOnClickListener {
            val intentStatistics = Intent(this, OpendStatisticsActivity::class.java)
            startActivity(intentStatistics)
        }

        binding.mapButten.setOnClickListener {
            val intentStatistics = Intent(this, MapsActivity::class.java)
            startActivity(intentStatistics)
        }

        binding.buttonLogOut.setOnClickListener {
            val intentStatistics = Intent(this, MainActivity::class.java)
            startActivity(intentStatistics)
        }
    }

    fun btnQRscanner(view: View) {
        //onScanQRcode(view)
        val code=530
        if (code != null) {
            callOpenBoxAPI("9ea96945-3a37-4638-a5d4-22e89fbc998f", code, 2)
        }
    }

    val getData =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val scannedData = data?.getStringExtra("SCAN_RESULT")
                Log.d("SCAN", "Scan results: $scannedData")

                val url = scannedData?.let { getUrlFromQRCode(it) }
                Log.d("SCAN", "URL: $url")
                //val code = url?.let { extractCodeFromURL(it)?.toInt() }
                val code=530
                if (code != null) {
                    callOpenBoxAPI("9ea96945-3a37-4638-a5d4-22e89fbc998f", code, 2)
                }
                //Log.d("SCAN", "Extracted code: $code")
            }
        }

    private fun getUrlFromQRCode(scannedData: String): String? {
        if (scannedData.startsWith("http://") || scannedData.startsWith("https://") || scannedData.startsWith(
                "HTTP://"
            ) || scannedData.startsWith("HTTPS://")
        ) {
            return scannedData
        }
        return null
    }

    private fun extractCodeFromURL(url: String): String? {
        val pattern = "/([0-9]{6})/"
        val regex = Regex(pattern)
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }

    fun onScanQRcode(view: View) {
        try {
            val intent = Intent("com.google.zxing.client.android.SCAN")
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
            getData.launch(intent)
        } catch (e: Exception) {
            var marketUri = Uri.parse("market://details?id=com.google.zxing.client.android")
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
            startActivity(marketIntent)
        }
    }
    fun callOpenBoxAPI(apiKey: String, boxId: Int, tokenFormat: Int) {
        val url = "https://api-d4me-stage.direct4.me/sandbox/v1/Access/openbox"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val jsonObject = JSONObject()
        jsonObject.put("boxId", boxId)
        jsonObject.put("tokenFormat", tokenFormat)
        val jsonObjectRequest = @RequiresApi(Build.VERSION_CODES.O)
        object : JsonObjectRequest(Method.POST, url, jsonObject,
            Response.Listener { response ->
                val data = response.optString("data")
                Log.d("api", "zeton $data")
                if(data != "") {
                    decompresAndPlay(data)
                }
                //shrani cas v bazo
                val timestamp = FieldValue.serverTimestamp()
                firestore.collection("openBoxTimes")
                    .add(mapOf("time" to timestamp, "username" to username))
                    .addOnSuccessListener {
                        Log.d("Firebase", "Time and username stored successfully in Firebase")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Error storing time and username in Firebase: ${e.message}")
                    }
                //----------
            },
            Response.ErrorListener { error ->
                Log.e("errrrrrr", Log.getStackTraceString(error))
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $apiKey"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            15000, // 10 sekund (v milisekundah)
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(jsonObjectRequest)


        getCurrentLocation { location ->
            location?.let {
                val latitude = location.first
                val longitude = location.second
                Log.i("LOCATION", "Latitude: $latitude, Longitude: $longitude")

                val intentStatistics = Intent(this, MapsActivity::class.java)
                intentStatistics.putExtra("latitude", latitude)
                intentStatistics.putExtra("longitude", longitude)
                startActivity(intentStatistics)
            }
        }
    }
    fun decompresAndPlay(getapi: String) {
        val decodedBytes = Base64.decode(getapi, Base64.DEFAULT)
        val tempFile = File.createTempFile("temp", null, cacheDir)
        val fos = FileOutputStream(tempFile)
        fos.write(decodedBytes)
        val zipFile = ZipFile(tempFile)
        val entries = zipFile.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name
            val entryInputStream = zipFile.getInputStream(entry)

            val tempEntryFile = File.createTempFile("temp", null, cacheDir)
            val foss = FileOutputStream(tempEntryFile)
            entryInputStream.copyTo(foss)
            fos.close()

            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(tempEntryFile.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            Toast.makeText(applicationContext,"Predvajanje žetona", Toast.LENGTH_LONG).show();

            entryInputStream.close()
        }

        zipFile.close()
    }
    fun getConnection(url:String,password:String,username:String): Connection? {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance()
            return DriverManager.getConnection(url, username, password)
            //val c = DriverManager.getConnection("jdbc:mysql://${dbCredentials.url}", dbCredentials.user,dbCredentials.pass)
            //println(c.isValid(0))
        } catch (e: SQLException) {
            println("${e.javaClass.simpleName} ${e.message}")
            //e.printStackTrace()
        } catch (e: Exception) {
            println("${e.javaClass.simpleName} ${e.message}")
        }
        return null
    }


    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    getCurrentLocation { location ->
                        location?.let {
                            val latitude = location.first
                            val longitude = location.second
                            Log.i("LOCATION", "Latitude: $latitude, Longitude: $longitude")

                            val intentStatistics = Intent(this, MapsActivity::class.java)
                            intentStatistics.putExtra("latitude", latitude)
                            intentStatistics.putExtra("longitude", longitude)
                            startActivity(intentStatistics)
                        }
                    }
                } else {
                    // Permission denied
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun getCurrentLocation(callback: (Pair<Double, Double>?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            requestLocationPermission()
            callback(null)
        } else {
            // Permission already granted, get the location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        callback(Pair(latitude, longitude))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LOCATION", "Error getting location: ${e.message}")
                    callback(null)
                }
        }
    }



}