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
import com.google.firebase.firestore.Query

class OpendStatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOpenStatisticsBinding
    lateinit var app: MyApplication
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: StatisticsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as MyApplication
        firestore = FirebaseFirestore.getInstance()

        adapter = StatisticsAdapter(app.statisticsList)
        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = LinearLayoutManager(this)

        loadStatisticsFromFireBase()
    }

    private fun loadStatisticsFromFireBase() {
        firestore.collection("openBoxTimes")
            .orderBy("time", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                app.statisticsList.clear()
                for (document in result) {
                    val timeStamp = document.getDate("time")
                    val username = document.getString("username")

                    if (timeStamp != null && username != null) {
                        app.statisticsList.add(statistics(timeStamp.toString(), username))
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.d("StoreActivity", "Error getting trgovine: ", exception)
            }
    }
}

