package com.example.hostelfood

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hostelfood.databinding.ItemFeedbackBinding
import java.text.SimpleDateFormat
import java.util.Locale

class FeedbackAdapter(private val feedbackList: List<FeedbackItem>) :
    RecyclerView.Adapter<FeedbackAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFeedbackBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = feedbackList[position]

        holder.binding.tvRollNo.text = "Roll No: ${item.rollNumber}"
        holder.binding.tvMeal.text = "${item.mealType} • ${formatTimestamp(item.timestamp)}"
        holder.binding.tvComment.text = item.comment   // Comment is guaranteed to have text

        //holder.binding.tvRatings.visibility = View.GONE
    }

    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        return if (timestamp != null) {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp.toDate())
        } else {
            "Unknown"
        }
    }

    override fun getItemCount() = feedbackList.size
}