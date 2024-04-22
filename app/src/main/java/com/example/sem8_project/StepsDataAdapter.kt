package com.example.sem8_project

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class StepsDataAdapter(context: Context, private val stepsList: List<StepsData>) : ArrayAdapter<StepsData>(context, 0, stepsList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.list_item_steps, parent, false)
        }

        val currentItem = stepsList[position]

        val dateTextView: TextView = itemView!!.findViewById(R.id.dateTextView)
        val stepsTextView: TextView = itemView.findViewById(R.id.stepsTextView)

        dateTextView.text = currentItem.date
        stepsTextView.text = currentItem.steps.toString()

        return itemView
    }
}
