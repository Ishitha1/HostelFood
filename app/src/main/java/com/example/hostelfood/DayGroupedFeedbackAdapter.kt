package com.example.hostelfood

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hostelfood.databinding.ItemDayFeedbackGroupBinding

class DayGroupedFeedbackAdapter(
    private val groupedData: Map<String, List<FeedbackItem>>
) : RecyclerView.Adapter<DayGroupedFeedbackAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(val binding: ItemDayFeedbackGroupBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemDayFeedbackGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val day = groupedData.keys.toList()[position]
        val feedbacks = groupedData[day] ?: emptyList()

        holder.binding.tvDayHeader.text = day
        holder.binding.tvCount.text = "${feedbacks.size} feedbacks"

        holder.binding.rvDayFeedbacks.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.binding.rvDayFeedbacks.adapter = FeedbackAdapter(feedbacks)
    }

    override fun getItemCount() = groupedData.size
}