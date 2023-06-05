package com.example.pjapppro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pjapppro.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        // Chaquopy integration
        Python.start(AndroidPlatform(this))
        val py = Python.getInstance()
        val pyObject = py.getModule("hello")
        val result = pyObject.callAttr("say_hello")
        val message = result.toString()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        binding.buttonLogin.setOnClickListener {
            val intentLogin = Intent(this, LoginActivity::class.java)
            startActivity(intentLogin)
        }
        binding.buttonRegister.setOnClickListener {
            val intentRegister = Intent(this, RegisterActivity::class.java)
            startActivity(intentRegister)
        }
    }
}
