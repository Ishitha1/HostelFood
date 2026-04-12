package com.example.hostelfood

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hostelfood.databinding.ItemManageMenuBinding

class MenuItemsAdapter(
    private val itemsList: MutableList<String>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<MenuItemsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemManageMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManageMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemsList[position]

        holder.binding.tvItemName.text = item

        holder.binding.btnDeleteItem.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = itemsList.size
}