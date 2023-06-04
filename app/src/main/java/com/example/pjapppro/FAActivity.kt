package com.example.pjapppro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class FAActivity : AppCompatActivity() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var verificationId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2fa)

        sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val etVerificationCode: EditText = findViewById(R.id.etVerificationCode)
        val btnSubmit: Button = findViewById(R.id.btnSubmit)

        val username = sharedPref.getString("username", "")
        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
        } else {
            firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val phoneNumber = document.getString("phoneNumber")
                        phoneNumber?.let {
                            sendSmsVerification(it)
                        } ?: run {
                            Toast.makeText(this, "Phone number not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error retrieving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnSubmit.setOnClickListener {
            val verificationCode = etVerificationCode.text.toString().trim()
            if (verificationCode.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSmsVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-verification completed, sign in with the credential
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // Verification failed, show error message
                    Toast.makeText(this@FAActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    // Code sent successfully, save the verification ID
                    this@FAActivity.verificationId = verificationId
                    Toast.makeText(this@FAActivity, "Verification code sent", Toast.LENGTH_SHORT).show()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@FAActivity, "Verification successful", Toast.LENGTH_SHORT).show()
                    val intentOpen = Intent(this, FAActivity::class.java)
                    startActivity(intentOpen)
                } else {
                    // Verification failed, show error message
                    Toast.makeText(this@FAActivity, "Verification failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
