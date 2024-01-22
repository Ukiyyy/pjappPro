package com.example.pjapppro

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.pjapppro.databinding.ActivityMapBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    lateinit var app: MyApplication
    lateinit var city: TSP.City

    private val handler = Handler(Looper.getMainLooper())
    private var iteration = 0
    private val maxIterations = 1000
    private var newPathFound = false

    val cities = mutableListOf<CityJson>()
    private var currentPolyline: Polyline? = null
    var decompressedDistanceFilePath=""
    var decompressedTimeFilePath=""

    private val REQUEST_CODE_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        app = application as MyApplication

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadCitiesFromFile()

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_cities)

        // Create an Adapter and set it to the RecyclerView
        val adapter = CityAdapter(cities.take(10)) // Pass the first 10 cities to the adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        checkAndRequestPermissions()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }


        copyFileFromAssets("compressed_distance_matrix.bin")
        copyFileFromAssets("compressed_time_matrix.bin")

        // distance
        val compressedDistanceFilePath = File(filesDir, "compressed_distance_matrix.bin").absolutePath
        decompressedDistanceFilePath = File(filesDir, "decompressed_distance_matrix.txt").absolutePath

        // time
        val compressedTimeFilePath = File(filesDir, "compressed_time_matrix.bin").absolutePath
        decompressedTimeFilePath = File(filesDir, "decompressed_time_matrix.txt").absolutePath

        val python = Python.getInstance()
        val pythonModule = python.getModule("Dekompresija") // Replace with your Python script name
        pythonModule.callAttr("execute", compressedDistanceFilePath, decompressedDistanceFilePath)
        pythonModule.callAttr("execute", compressedTimeFilePath, decompressedTimeFilePath)

        binding.btnCalculatePath.setOnClickListener {
            val selectedRadioButtonId = binding.radioGroupOptions.checkedRadioButtonId

            when (selectedRadioButtonId) {
                R.id.radio_time -> {
                    calculatePathBasedOnTime()
                }
                R.id.radio_length -> {
                    calculatePathBasedOnLength()
                }
                else -> {
                    Toast.makeText(this, "Please select an option.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun logFileContents(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val contents = file.readText()
                Log.d("FileContents", contents)
            } else {
                Log.e("FileError", "File does not exist: $filePath")
            }
        } catch (e: IOException) {
            Log.e("FileError", "Error reading file: ${e.message}")
        }
    }


    private fun copyFileFromAssets(fileName: String) {
        val file = File(filesDir, fileName)
        if (!file.exists()) {
            try {
                val inputStream = assets.open(fileName)
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun resetState() {
        iteration = 0
        newPathFound = false
        currentPolyline?.remove()
    }


    private fun updateMap(fromIndex: Int, toIndex: Int, path: TSP.Tour) {
        // Clear the existing polyline
        currentPolyline?.remove()


        //cities[c.index][c2.index]
        val polylineOptions = PolylineOptions().apply {
            color(Color.BLUE) // Set the color of the polyline
            width(5f)        // Set the width of the polyline
            for (i in 0 until path.getPath().size) {
                if (i + 1 >= path.getPath().size) {
                    break
                }
                var c = path.getPath()[i]
                var c2 = path.getPath()[i + 1]
                if (c != null) {
                    if (c2 != null) {
                        add(LatLng(cities[c.index].latitude, cities[c2.index].longitude))
                    }
                }
            }
            var last = path.getPath()[path.getPath().size-1]
            var first = path.getPath()[0]
            if (last != null) {
                if (first != null) {
                    add(LatLng(cities[last.index].latitude, cities[first.index].longitude))
                }
            }
        }
        currentPolyline = googleMap.addPolyline(polylineOptions)
    }

    private fun calculatePathBasedOnTime() {
        logFileContents(decompressedDistanceFilePath)
        resetState()
        while (iteration < maxIterations) {
            Log.d("MapsActivity", "Inside while loop, iteration: $iteration")
            val tsp = TSP(this, decompressedDistanceFilePath, 1000)
            val ga = GA(100, 0.8, 0.1)
            val bestPath: TSP.Tour? = ga.execute(tsp)
            if (bestPath != null) {
                newPathFound = true
                val maxX = tsp.getCities().maxOfOrNull { it.x }?.toInt() ?: 1
                val maxY = tsp.getCities().maxOfOrNull { it.y }?.toInt() ?: 1
                Log.d("MapsActivity", "New path found: $newPathFound, maxX: $maxX, maxY: $maxY")
                updateMap(maxX, maxY, bestPath)
            }
            iteration++
        }
    }

    private fun calculatePathBasedOnLength() {
        resetState()
        while (iteration < maxIterations) {
            Log.d("MapsActivity", "Inside while loop, iteration: $iteration")
            val tsp = TSP(this, decompressedTimeFilePath, 1000)
            val ga = GA(100, 0.8, 0.1)
            val bestPath: TSP.Tour? = ga.execute(tsp)
            if (bestPath != null) {
                newPathFound = true
                val maxX = tsp.getCities().maxOfOrNull { it.x }?.toInt() ?: 1
                val maxY = tsp.getCities().maxOfOrNull { it.y }?.toInt() ?: 1
                Log.d("MapsActivity", "New path found: $newPathFound, maxX: $maxX, maxY: $maxY")
                updateMap(maxX, maxY, bestPath)
            }
            iteration++
        }
    }

    private fun loadCitiesFromFile() {
        val jsonString: String

        try {
            val jsonString =
                assets.open("location_data.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val coordinates = jsonObject.getJSONObject("koordinate")
                val latitude = coordinates.getDouble("lat")
                val longitude = coordinates.getDouble("lng")
                val address = jsonObject.getString("naslov")

                cities.add(CityJson(address, latitude, longitude))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createMarkersForCities() {
        cities.take(10).forEach { city ->
            val latLng = LatLng(city.latitude, city.longitude)
            val markerOptions = MarkerOptions().position(latLng).title(city.address)
            googleMap.addMarker(markerOptions)
        }
    }

    private fun connectCitiesInOrder() {
        if (cities.size > 1) {
            val polylineOptions = PolylineOptions().apply {
                color(Color.BLUE) // Set the color of the polyline
                width(5f)        // Set the width of the polyline
            }

            // Add points to the polyline for the first 10 cities
            cities.take(10).forEach { city ->
                polylineOptions.add(LatLng(city.latitude, city.longitude))
            }

            // Add the polyline to the map
            googleMap.addPolyline(polylineOptions)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        Log.d("MapsActivity", "Map is ready.")

        createMarkersForCities()
        connectCitiesInOrder()

        if (isLocationPermissionGranted()) {
            initializeMap()
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)
            if (latitude != 0.0 && longitude != 0.0) {
                addMarkerFromIntent()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_PERMISSION
        )
    }

    private fun initializeMap() {
        app = application as MyApplication
        if (app.markerList.isNotEmpty()) {
            for (markerInfo in app.markerList) {
                val latLng = LatLng(markerInfo.latitude, markerInfo.longitude)
                val markerOptions = MarkerOptions().position(latLng).title(markerInfo.ime)
                googleMap.addMarker(markerOptions)
                Log.d(
                    "MapsActivity",
                    "Added marker at: lat=${markerInfo.latitude}, lng=${markerInfo.longitude}"
                )
            }
        } else {
            Log.d("MapsActivity", "No markers to add to the map.")
        }
        try {
            googleMap.isMyLocationEnabled = true

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {

                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))

                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

    }

    private fun addMarkerFromIntent() {
        app = application as MyApplication
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        if (latitude != 0.0 && longitude != 0.0) {
            val latLng = LatLng(latitude, longitude)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            val marker = MarkerOptions().position(latLng)
                .title("Poskus odklepanja ob $currentTime, $currentDate")
            val title: String = "Poskus odklepanja ob $currentTime, $currentDate"
            googleMap.addMarker(marker)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f))

            app.markerList.add(markerji(title, latitude, longitude))
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        deleteDecompressedFile()
    }

    private fun deleteDecompressedFile() {
        try {
            val file = File(decompressedDistanceFilePath)
            val timeFile= File(decompressedTimeFilePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d("MapsActivity", "File deleted successfully")
                } else {
                    Log.d("MapsActivity", "Failed to delete the file")
                }
            }
            if (timeFile.exists()) {
                val deleted = timeFile.delete()
                if (deleted) {
                    Log.d("MapsActivity", "TimeFile deleted successfully")
                } else {
                    Log.d("MapsActivity", "Failed to delete the TimeFile")
                }
            }
        } catch (e: Exception) {
            Log.e("MapsActivity", "Error deleting the TimeFile", e)
        }
    }
    private fun checkAndRequestPermissions() {
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val listPermissionsNeeded = ArrayList<String>()

        if (readPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permissions are granted
                    Log.d("MapsActivity", "Permissions granted.")
                } else {
                    // Permissions are denied
                    Log.d("MapsActivity", "Permissions denied.")
                }
                return
            }
        }
    }
}
