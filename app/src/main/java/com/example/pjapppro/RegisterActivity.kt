package com.example.pjapppro

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private var firestore: FirebaseFirestore? = null
    private var editTextUsername: EditText? = null
    private var editTextEmail: EditText? = null
    private var editTextPassword: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        firestore = FirebaseFirestore.getInstance()
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
    }

    fun onRegister(view: View?) {
        val username = editTextUsername!!.text.toString().trim { it <= ' ' }
        val email = editTextEmail!!.text.toString().trim { it <= ' ' }
        val password = editTextPassword!!.text.toString().trim { it <= ' ' }

        // Validate the input fields
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new user document in Firestore
        val user: MutableMap<String, Any> = HashMap()
        user["username"] = username
        user["email"] = email
        user["password"] = password
        firestore!!.collection("users")
            .add(user)
            .addOnSuccessListener { documentReference: DocumentReference? ->
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e: Exception ->
                Log.e("Firebase", "Error registering user: " + e.message)
                Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}