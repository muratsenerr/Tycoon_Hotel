package com.example.tycon_hotel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class BuildOptionAdapter(
    private val options: List<BuildOption>,
    private val onBuildClick: (BuildOption) -> Unit
) : RecyclerView.Adapter<BuildOptionAdapter.BuildViewHolder>() {

    class BuildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgRoomIcon)
        val txtName: TextView = itemView.findViewById(R.id.txtBuildRoomName)
        val txtEarnings: TextView = itemView.findViewById(R.id.txtBuildRoomEarnings)
        val txtCost: TextView = itemView.findViewById(R.id.txtBuildRoomCost)
        val btnBuild: Button = itemView.findViewById(R.id.btnBuildNow)
        val cardView: View = itemView.findViewById(R.id.cardBuildRoom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_build_room, parent, false)
        return BuildViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuildViewHolder, position: Int) {
        val option = options[position]
        holder.txtName.text = option.name
        holder.txtEarnings.text = String.format(Locale.getDefault(), "+%.2f $ / sn", option.baseEarnings)
        holder.txtCost.text = String.format(Locale.getDefault(), "Fiyat: %.0f $", option.baseCost)
        holder.imgIcon.setImageResource(option.iconResId)
        
        if (option.isLocked) {
            holder.cardView.alpha = 0.5f
            holder.btnBuild.text = "KİLİTLİ"
        } else {
            holder.cardView.alpha = 1.0f
            holder.btnBuild.text = "İNŞA ET"
        }

        holder.btnBuild.setOnClickListener {
            onBuildClick(option)
        }
    }

    override fun getItemCount(): Int = options.size
}
