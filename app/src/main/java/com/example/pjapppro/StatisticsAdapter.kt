package com.example.pjapppro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.pjapppro.databinding.CardOpenStatisticsBinding

class StatisticsAdapter(private val data: MutableList<statistics>): RecyclerView.Adapter<StatisticsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeStamp: TextView = itemView.findViewById(R.id.timeStamp)
        val user: TextView = itemView.findViewById(R.id.username)
        val cardView: CardView = itemView.findViewById(R.id.cardviewLine)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_open_statistics, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemsViewModel = data[position]

        holder.timeStamp.text = itemsViewModel.timeStamp
        holder.user.text = itemsViewModel.user

    }

    override fun getItemCount(): Int {
        return data.size
    }
}