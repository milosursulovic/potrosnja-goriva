package com.example.potronjagoriva

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FuelAdapter(
    private val entries: List<FuelEntry>,
    private val onDeleteClick: (FuelEntry) -> Unit
) : RecyclerView.Adapter<FuelAdapter.FuelViewHolder>() {
    class FuelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val infoText: TextView = view.findViewById(R.id.info_text)
        val deleteBtn: ImageButton = view.findViewById(R.id.delete_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fuel_entry, parent, false)

        return FuelViewHolder(view)
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    override fun onBindViewHolder(holder: FuelViewHolder, position: Int) {
        val entry = entries[position]
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date(entry.timestamp))
        val cost = entry.liters * entry.fuelPrice

        holder.infoText.text = String.format(
            "%s\nL: %.2f | Km: %.2f | Cena: %d RSD | Potrošnja: %.2f l/100km | Plaćeno: %.2f RSD",
            dateStr,
            entry.liters,
            entry.kilometers,
            entry.fuelPrice,
            entry.consumption,
            cost
        )

        holder.deleteBtn.setOnClickListener {
            onDeleteClick(entry)
        }
    }

}