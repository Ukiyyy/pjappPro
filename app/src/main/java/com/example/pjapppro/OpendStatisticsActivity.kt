package com.example.pjapppro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pjapppro.databinding.ActivityOpenStatisticsBinding
import com.google.firebase.firestore.FirebaseFirestore

class OpenStatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOpenStatisticsBinding
    lateinit var app: MyApplication
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as MyApplication
        firestore = FirebaseFirestore.getInstance()

        binding = ActivityOpenStatisticsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val adapter = StatisticsAdapter(app.statisticsList)
        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = LinearLayoutManager(this)

        loadStatisticsFromFireBase()

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                val timeStamp = data?.getStringExtra("time")
                val user = data?.getStringExtra("user")

                app.statisticsList.add(statistics(timeStamp!!, user!!))
                binding.recycler.adapter?.notifyDataSetChanged()
            }
        }

    }
    private fun loadStatisticsFromFireBase() {
        firestore.collection("openBoxTimes")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val timeStamp = document.getString("time")
                    val username = document.getString("username")


                    if (timeStamp != null && username != null) {
                        app.statisticsList.add(statistics(timeStamp, username))
                    }
                }
                binding.recycler.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.d("StoreActivity", "Error getting trgovine: ", exception)
            }
    }
}