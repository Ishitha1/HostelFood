package com.example.hostelfood

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hostelfood.databinding.ItemDayMenuBinding

class WeekMenuAdapter(
    private val dayList: List<DayMenu>,
    private val onDayClick: (String) -> Unit
) : RecyclerView.Adapter<WeekMenuAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDayMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDayMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dayMenu = dayList[position]
        holder.binding.tvDayName.text = dayMenu.day
        holder.binding.tvMeals.text = dayMenu.meals.joinToString("\n")

        holder.itemView.setOnClickListener {
            onDayClick(dayMenu.day)
        }
    }

    override fun getItemCount() = dayList.size
}