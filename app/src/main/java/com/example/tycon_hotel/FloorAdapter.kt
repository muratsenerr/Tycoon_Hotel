package com.example.tycon_hotel

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class FloorAdapter(
    private val floorList: List<Floor>,
    var globalFoodStock: Double = 0.0,
    private val onUpgradeClick: (Floor) -> Unit,
    private val onHireStaffClick: (Floor) -> Unit,
    private val onTransferClick: (Floor) -> Unit,
    private val onUpgradeStorageClick: (Floor) -> Unit
) : RecyclerView.Adapter<FloorAdapter.FloorViewHolder>() {

    class FloorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val floorName: TextView = itemView.findViewById(R.id.txtFloorName)
        val earnings: TextView = itemView.findViewById(R.id.txtEarnings)
        val btnUpgrade: Button = itemView.findViewById(R.id.btnUpgrade)
        val btnHireStaff: Button = itemView.findViewById(R.id.btnHireStaff)
        val btnTransferFood: Button = itemView.findViewById(R.id.btnTransferFood)
        val btnUpgradeStorage: Button = itemView.findViewById(R.id.btnUpgradeStorage)
        val imgWorker: ImageView = itemView.findViewById(R.id.imgWorker)
        val imgBackground: ImageView = itemView.findViewById(R.id.imgFloorBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_floor, parent, false)
        return FloorViewHolder(view)
    }

    override fun onBindViewHolder(holder: FloorViewHolder, position: Int) {
        val floor = floorList[position]
        
        // Başlıkta personel sayısını göster (Sadece personel alınabilen odalarda)
        val showStaff = floor.type == RoomType.RESTAURANT || floor.type == RoomType.KITCHEN || floor.type == RoomType.LAUNDRY
        
        if (showStaff) {
            holder.floorName.text = String.format(Locale.getDefault(), "%s (Lv. %d) [%d Personel]", floor.name, floor.level, floor.staffCount)
        } else {
            holder.floorName.text = String.format(Locale.getDefault(), "%s (Lv. %d)", floor.name, floor.level)
        }
        
        // Mutfak ise üretim miktarını ve biriken yemeği göster
        if (floor.type == RoomType.KITCHEN) {
            holder.earnings.text = String.format(Locale.getDefault(), "Üretim: %.1f/sn | Depo: %d/%d", 
                floor.producesFoodPerSec * floor.staffCount, floor.internalFoodStock.toInt(), floor.maxFoodStorage)
            holder.earnings.setTextColor(Color.parseColor("#FF9800"))
        } else if (floor.type == RoomType.RESTAURANT) {
            val status = if (floor.restaurantTimer > 0) "Yemek Satılıyor: ${floor.restaurantTimer} sn" else "Bekleniyor (5 Yemek Lazım)"
            holder.earnings.text = String.format(Locale.getDefault(), "Stok: %d | %s", globalFoodStock.toInt(), status)
            holder.earnings.setTextColor(Color.parseColor("#F44336"))
        } else {
            holder.earnings.text = String.format(Locale.getDefault(), "%.2f $/sn", floor.earningsPerSecond)
            holder.earnings.setTextColor(Color.parseColor("#4CAF50"))
        }

        // Buton Görünürlükleri
        if (floor.type == RoomType.RECEPTION) {
            holder.btnUpgrade.visibility = View.GONE
            holder.btnHireStaff.visibility = View.GONE
            holder.btnTransferFood.visibility = View.GONE
        } else {
            holder.btnUpgrade.visibility = View.VISIBLE
            holder.btnUpgrade.text = String.format(Locale.getDefault(), "YÜKSELT\n$%.0f", floor.upgradeCost)
            
            if (showStaff) {
                holder.btnHireStaff.visibility = View.VISIBLE
                holder.btnHireStaff.text = String.format(Locale.getDefault(), "PERSONEL AL\n$%.0f", floor.staffCost)
            } else {
                holder.btnHireStaff.visibility = View.GONE
            }

            if (floor.type == RoomType.KITCHEN) {
                holder.btnTransferFood.visibility = View.VISIBLE
                holder.btnTransferFood.text = String.format(Locale.getDefault(), "AKTARIYOR (%d)", floor.internalFoodStock.toInt())
                holder.btnTransferFood.isEnabled = floor.internalFoodStock >= 1

                holder.btnUpgradeStorage.visibility = View.VISIBLE
                holder.btnUpgradeStorage.text = String.format(Locale.getDefault(), "DEPOYU BÜYÜT\n$%.0f", floor.upgradeStorageCost)
            } else {
                holder.btnTransferFood.visibility = View.GONE
                holder.btnUpgradeStorage.visibility = View.GONE
            }
        }

        // Oda türüne göre renk belirleme
        val color = when (floor.type) {
            RoomType.RECEPTION -> "#4CAF50"
            RoomType.HOTEL_ROOM -> "#2196F3"
            RoomType.RESTAURANT -> "#F44336"
            RoomType.SPA -> "#9C27B0"
            RoomType.BAR -> "#FF9800"
            RoomType.LAUNDRY -> "#607D8B"
            RoomType.KITCHEN -> "#795548" // Kahverengi (Mutfak)
        }
        holder.imgBackground.setBackgroundColor(Color.parseColor(color))

        holder.btnUpgrade.setOnClickListener { onUpgradeClick(floor) }
        holder.btnHireStaff.setOnClickListener { onHireStaffClick(floor) }
        holder.btnTransferFood.setOnClickListener { onTransferClick(floor) }
        holder.btnUpgradeStorage.setOnClickListener { onUpgradeStorageClick(floor) }

        // Karakter Animasyonu
        val animator = ObjectAnimator.ofFloat(holder.imgWorker, "translationX", 0f, 600f)
        animator.duration = 4000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.start()
    }

    override fun getItemCount(): Int = floorList.size
}
