package com.example.pjapppro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
    }
    private fun loadCitiesFromFile() {
        val jsonString: String
        val cities = mutableListOf<CityJson>()

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
                runTSPAndLogResults()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private suspend fun findBestTSPPath(): List<TSP.City> {
        var bestTour: TSP.Tour? = null
        for (i in 0..100) {
            val eilTsp = TSP("distance_matrix.txt", 1000)
            val ga = GA(100, 0.8, 0.1)
            val currentTour = ga.execute(eilTsp)
            if (bestTour == null || (currentTour != null && currentTour.distance < bestTour.distance)) {
                bestTour = currentTour
            }
        }
        // Assuming bestTour's path is an array of TSP.City
        return bestTour?.getPath()?.filterNotNull() ?: emptyList()
    }

    private fun runTSPAndLogResults() {
        // Running in a background thread
        GlobalScope.launch(Dispatchers.IO) {
            val bestPath = findBestTSPPath()
            // Log the results
            logTSPResults(bestPath)
        }
    }

    private fun logTSPResults(path: List<TSP.City>) {
        // Log the results
        for (city in path) {
            Log.d("TSPResult", "City ${city.index}: x = ${city.x}, y = ${city.y}")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        Log.d("MapsActivity", "Map is ready.")

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
