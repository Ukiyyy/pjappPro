package com.example.pjapppro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun btnQRscanner(view: View) {
        onScanQRcode(view)
    }
    val getData = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val scannedData = data?.getStringExtra("SCAN_RESULT")
            Log.d("SCAN", "Scan results: $scannedData")

            // Shranite povezavo v primeru, da je na voljo
            val url = scannedData?.let { getUrlFromQRCode(it) }
            Log.d("SCAN", "URL: $url")
            val code = url?.let { extractCodeFromURL(it) }
            Log.d("SCAN", "Extracted code: ${code?.toInt()}")
        }
    }
    private fun getUrlFromQRCode(scannedData: String): String? {
        // Tukaj lahko obdelate skenirane podatke in iz njih izvlečete povezavo
        // Predpostavimo, da skenirana povezava začne z "http://" ali "https://"
        if (scannedData.startsWith("http://") || scannedData.startsWith("https://") || scannedData.startsWith("HTTP://") || scannedData.startsWith("HTTPS://")) {
            return scannedData
        }
        return null
    }
    private fun extractCodeFromURL(url: String): String? {
        val pattern = "/([0-9]{6})/"  // Uporabite ustrezno vzorec glede na vašo specifikacijo
        val regex = Regex(pattern)
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }
    /*var getData =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                Log.d("SCAN","Scan results: ${data?.getStringExtra("SCAN_RESULT")}")
            //println("Scan results: ${data?.getStringExtra("SCAN_RESULT")}")
            }
        }*/

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
}