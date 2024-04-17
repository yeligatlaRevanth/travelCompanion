package com.example.sem8_project

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.RecyclerView

class A4_C_RecyclerViewAdapter (private val trips: List<A4_C_Trip_Details>) : RecyclerView.Adapter<A4_C_RecyclerViewAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.a4_item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]
        holder.bind(trip)
    }

    override fun getItemCount(): Int {
        return trips.size
    }



    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripImage: ImageView = itemView.findViewById(R.id.tripImage)
        private val tripLocation: TextView = itemView.findViewById(R.id.tripLocation)
        private val tripCreatedBy: TextView = itemView.findViewById(R.id.tripCreatedBy)
        private val joinButton: Button = itemView.findViewById(R.id.joinButton)

        fun bind(trip: A4_C_Trip_Details) {
            tripLocation.text = trip.location
            tripCreatedBy.text = "Created by: ${trip.createdBy}"

            // Load image using Glide
            Glide.with(itemView.context)
                .load(trip.imageUrl)
                .into(tripImage)

            joinButton.setOnClickListener {
                val intent = Intent(itemView.context, A7_TripPage::class.java)
                intent.putExtra("tripId", trip.uniqueId) // Replace "uniqueId" with the actual field in A4_C_Trip_Details
                itemView.context.startActivity(intent)
            }
        }
    }
}