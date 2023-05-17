package com.example.pjapppro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.lang.Exception
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun btnQRscanner(view: View) {
        callOpenBoxAPI("9ea96945-3a37-4638-a5d4-22e89fbc998f",530, 2)
        onScanQRcode(view)
    }
    val getData = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val scannedData = data?.getStringExtra("SCAN_RESULT")
            Log.d("SCAN", "Scan results: $scannedData")

            val url = scannedData?.let { getUrlFromQRCode(it) }
            Log.d("SCAN", "URL: $url")
            val code = url?.let { extractCodeFromURL(it)?.toInt() }
            Log.d("SCAN", "Extracted code: $code")
        }
    }
    private fun getUrlFromQRCode(scannedData: String): String? {
        if (scannedData.startsWith("http://") || scannedData.startsWith("https://") || scannedData.startsWith("HTTP://") || scannedData.startsWith("HTTPS://")) {
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
        val url = "https://api-d4me-stage.direct4.me/sandbox/v1/Access/openbox" // Zamenjajte z dejanskim URL-om API-ja
        val requestQueue: RequestQueue = Volley.newRequestQueue(this) // Če kličete iz aktivnosti, prenesite kontekst
        val jsonObject = JSONObject()
        jsonObject.put("boxId", boxId)
        jsonObject.put("tokenFormat", tokenFormat)
        val jsonObjectRequest = object : JsonObjectRequest(Method.POST, url, jsonObject,
            Response.Listener { response ->
                var result = response.optInt("result")
                //val data = response.optString("data")
                //var errNumber = response.optInt("errNumber")
                Log.d("api", "supercaaaa $result")
                println("Odgovor: $response")
                Log.d("api", "supercaaaa $response")
                // Tukaj lahko nadaljujete z obdelavo odgovora API-ja
            },
            Response.ErrorListener { error ->
                // Napaka pri izvajanju zahtevka
                Log.e("errrrrrr", Log.getStackTraceString(error))
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $apiKey"
                headers["Content-type"] = "application/json"
                return headers
            }
        }

        requestQueue.add(jsonObjectRequest)
    }

}