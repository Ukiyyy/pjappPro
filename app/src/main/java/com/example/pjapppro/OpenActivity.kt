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


class OpenActivity : AppCompatActivity() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityOpenBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

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
               insert()
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
    @RequiresApi(Build.VERSION_CODES.O)
    fun insert(): Boolean {
        //val jsonstr: String = File("./src/jvmMain/kotlin/data/db/config.json").readText(Charsets.UTF_8)
        //val c = Json.decodeFromString<DbCredentials>(jsonstr)
        val conn =getConnection("sql7.freemysqlhosting.net","syRmuGc8Zd","sql7620703") ?: return true
        conn.use {
            try {
                Log.e("blabla","ashdashdashldhasjkd")
                val select = it.prepareStatement("INSERT INTO logs (date,userid,paketnikid) VALUES (?,?,?)")

                select.setObject(1, LocalDate.now())
                select.setInt(2, 1)
                select.setInt(3, 1)

                var rs = select.executeUpdate() //insert update
            } catch (ex: SQLException) {
                println(ex.message)
            }
        }
        return true
    }

}