package com.example.pjapppro

import android.Manifest
import android.content.Intent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    lateinit var app: MyApplication

    private val handler = Handler(Looper.getMainLooper()) // Handler for UI updates
    private var iteration = 0
    private val maxIterations = 1000
    private var newPathFound = false

    val cities = mutableListOf<CityJson>()
    private var currentPolyline: Polyline? = null

    private val REQUEST_CODE_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        app = application as MyApplication

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnExit.setOnClickListener {
            finish()
        }
        loadCitiesFromFile()
        /*
        Log.d("MapsActivity", "Before initializing TSP and GA")
        val tsp = TSP(readDistanceMatrix(), 1000)
        val ga = GA(100, 0.8, 0.1)
        var currentBestPath: TSP.Tour? = null
        Log.d("MapsActivity", "TSP and GA initialized")
        */
    }

    private fun readDistanceMatrix(): String {
        return try {
            val content = assets.open("distance_matrix.txt").bufferedReader().use { it.readText() }
            Log.d("MapsActivity", "File Content: $content")  // Log the file content
            content
        } catch (e: IOException) {
            e.printStackTrace()
            "" // Return an empty string or handle the error appropriately
        }
    }

    private fun updateMap(fromIndex: Int, toIndex: Int) {
        // Clear the existing polyline
        currentPolyline?.remove()

        if (cities.size > fromIndex && cities.size > toIndex) {
            val polylineOptions = PolylineOptions().apply {
                color(Color.BLUE) // Set the color of the polyline
                width(5f)        // Set the width of the polyline
                add(LatLng(cities[fromIndex].latitude, cities[fromIndex].longitude)) // Start point
                add(LatLng(cities[toIndex].latitude, cities[toIndex].longitude))     // End point
            }

            // Draw the new polyline on the map
            currentPolyline = googleMap.addPolyline(polylineOptions)
        }
    }

    private fun loadCitiesFromFile() {
        val jsonString: String

        try {
            val jsonString = assets.open("location_data.json").bufferedReader().use { it.readText() }
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

        while (iteration < maxIterations) {
            Log.d("MapsActivity", "Inside while loop, iteration: $iteration")
            val tsp = TSP(this, "distance_matrix.txt", 1000)
            val ga = GA(100, 0.8, 0.1)
            val bestPath: TSP.Tour? = ga.execute(tsp)
            if (bestPath != null){
                newPathFound = true
                val maxX = tsp.getCities().maxOfOrNull { it.x }?.toInt() ?: 1
                val maxY = tsp.getCities().maxOfOrNull { it.y }?.toInt() ?: 1
                Log.d("MapsActivity", "New path found: $newPathFound, maxX: $maxX, maxY: $maxY")
                updateMap(maxX, maxY)
            }
            iteration++
        }
        Log.d("MapsActivity", "After while loop")

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
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeMap()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeMap() {
        app = application as MyApplication
        if (app.markerList.isNotEmpty()) {
            for (markerInfo in app.markerList) {
                val latLng = LatLng(markerInfo.latitude, markerInfo.longitude)
                val markerOptions = MarkerOptions().position(latLng).title(markerInfo.ime)
                googleMap.addMarker(markerOptions)
                Log.d("MapsActivity", "Added marker at: lat=${markerInfo.latitude}, lng=${markerInfo.longitude}")
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
            val marker = MarkerOptions().position(latLng).title("Poskus odklepanja ob $currentTime, $currentDate")
            val title:String = "Poskus odklepanja ob $currentTime, $currentDate"
            googleMap.addMarker(marker)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f))

            app.markerList.add(markerji(title,latitude,longitude))
        }
    }
}
