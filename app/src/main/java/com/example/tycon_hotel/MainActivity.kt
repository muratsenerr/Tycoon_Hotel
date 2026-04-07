package com.example.tycon_hotel

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FloorAdapter
    private lateinit var txtTotalMoney: TextView
    private lateinit var txtFoodStock: TextView
    private lateinit var btnBuyFloor: Button
    private val floorList = mutableListOf<Floor>()
    private var totalMoney: Double = 50.0
    private var foodStock: Int = 0
    
    private var nextHotelRoomCost: Double = 50.0
    private var nextRestaurantCost: Double = 750.0
    private var nextSpaCost: Double = 4000.0
    private var nextBarCost: Double = 10000.0
    private var nextLaundryCost: Double = 12000.0
    private var nextKitchenCost: Double = 500.0

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("TycoonHotelPrefs", Context.MODE_PRIVATE)

        txtTotalMoney = findViewById(R.id.txtTotalMoney)
        txtFoodStock = findViewById(R.id.txtFoodStock)
        btnBuyFloor = findViewById(R.id.btnBuyFloor)
        
        loadGameData()
        setupRecyclerView()
        startGameLoop()

        btnBuyFloor.setOnClickListener {
            showBuyFloorDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        calculateOfflineEarnings()
    }

    override fun onPause() {
        super.onPause()
        saveGameData()
    }

    private fun saveGameData() {
        val editor = sharedPreferences.edit()
        editor.putLong("last_exit_time", System.currentTimeMillis())
        editor.putString("total_money", totalMoney.toString())
        editor.putInt("food_stock", foodStock)
        editor.putString("next_hotel_room_cost", nextHotelRoomCost.toString())
        editor.putString("next_restaurant_cost", nextRestaurantCost.toString())
        editor.putString("next_spa_cost", nextSpaCost.toString())
        editor.putString("next_bar_cost", nextBarCost.toString())
        editor.putString("next_laundry_cost", nextLaundryCost.toString())
        editor.putString("next_kitchen_cost", nextKitchenCost.toString())
        
        val floorListJson = gson.toJson(floorList)
        editor.putString("floor_list", floorListJson)
        
        editor.apply()
    }

    private fun loadGameData() {
        val savedMoneyStr = sharedPreferences.getString("total_money", "50.0")
        totalMoney = savedMoneyStr?.toDouble() ?: 50.0
        foodStock = sharedPreferences.getInt("food_stock", 0)
        
        nextHotelRoomCost = sharedPreferences.getString("next_hotel_room_cost", "50.0")?.toDouble() ?: 50.0
        nextRestaurantCost = sharedPreferences.getString("next_restaurant_cost", "750.0")?.toDouble() ?: 750.0
        nextSpaCost = sharedPreferences.getString("next_spa_cost", "4000.0")?.toDouble() ?: 4000.0
        nextBarCost = sharedPreferences.getString("next_bar_cost", "10000.0")?.toDouble() ?: 10000.0
        nextLaundryCost = sharedPreferences.getString("next_laundry_cost", "12000.0")?.toDouble() ?: 12000.0
        nextKitchenCost = sharedPreferences.getString("next_kitchen_cost", "500.0")?.toDouble() ?: 500.0

        val floorListJson = sharedPreferences.getString("floor_list", null)
        if (floorListJson != null) {
            val type = object : TypeToken<List<Floor>>() {}.type
            val savedFloors: List<Floor> = gson.fromJson(floorListJson, type)
            floorList.clear()
            floorList.addAll(savedFloors)
        } else {
            floorList.add(Floor(1, "Resepsiyon", 1, 0.0, 50.0, 50.0, RoomType.RECEPTION))
        }

        updateMoneyDisplay()
        updateFoodDisplay()
    }

    private fun calculateOfflineEarnings() {
        val lastExitTime = sharedPreferences.getLong("last_exit_time", 0L)
        if (lastExitTime != 0L) {
            val currentTime = System.currentTimeMillis()
            val differenceInSeconds = (currentTime - lastExitTime) / 1000

            if (differenceInSeconds > 0) {
                // Basit offline kazanç (Yemek stoğu bitmiş olabilir, bu yüzden sadece otel odalarını sayalım veya basitleştirelim)
                val hotelEarnings = floorList.filter { it.type == RoomType.HOTEL_ROOM }.sumOf { it.earningsPerSecond }
                val offlineEarnings = differenceInSeconds * hotelEarnings

                if (offlineEarnings > 0) {
                    totalMoney += offlineEarnings
                    updateMoneyDisplay()
                    showOfflineEarningsDialog(offlineEarnings, differenceInSeconds)
                }
            }
        }
    }

    private fun showOfflineEarningsDialog(earnings: Double, seconds: Long) {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        val timeString = when {
            hours > 0 -> String.format(Locale.getDefault(), "%d saat %d dk", hours, minutes)
            minutes > 0 -> String.format(Locale.getDefault(), "%d dk %d sn", minutes, remainingSeconds)
            else -> String.format(Locale.getDefault(), "%d sn", remainingSeconds)
        }

        AlertDialog.Builder(this)
            .setTitle("Hoş Geldin!")
            .setMessage(String.format(Locale.getDefault(), "Yokluğunda (%s) toplamda %.2f $ kazandın.", timeString, earnings))
            .setPositiveButton("Harika!") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewFloors)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        
        adapter = FloorAdapter(floorList, 
            onUpgradeClick = { floor -> handleUpgrade(floor) },
            onHireStaffClick = { floor -> handleHireStaff(floor) }
        )
        
        recyclerView.adapter = adapter
    }

    private fun handleUpgrade(floor: Floor) {
        if (floor.type == RoomType.RECEPTION) return

        if (totalMoney >= floor.upgradeCost) {
            totalMoney -= floor.upgradeCost
            updateMoneyDisplay()

            floor.level++
            floor.earningsPerSecond *= 1.20
            if (floor.type == RoomType.KITCHEN) {
                floor.producesFoodPerSec = (floor.producesFoodPerSec * 1.2).toInt().coerceAtLeast(floor.producesFoodPerSec + 1)
            }
            floor.upgradeCost = floor.baseCost * (1.30.pow(floor.level.toDouble()))

            adapter.notifyDataSetChanged()
        } else {
            Toast.makeText(this, "Yetersiz bakiye!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleHireStaff(floor: Floor) {
        if (totalMoney >= floor.staffCost) {
            totalMoney -= floor.staffCost
            updateMoneyDisplay()

            floor.staffCount++
            floor.staffCost *= 1.40 // Personel maliyeti %40 artar
            
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Personel işe alındı!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Yetersiz bakiye!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBuyFloorDialog() {
        val hotelRoomCount = floorList.count { it.type == RoomType.HOTEL_ROOM }
        
        val costs = mapOf(
            RoomType.HOTEL_ROOM to nextHotelRoomCost,
            RoomType.RESTAURANT to nextRestaurantCost,
            RoomType.SPA to nextSpaCost,
            RoomType.BAR to nextBarCost,
            RoomType.LAUNDRY to nextLaundryCost,
            RoomType.KITCHEN to nextKitchenCost
        )

        val bottomSheet = RoomSelectionBottomSheet(costs, hotelRoomCount) { option ->
            if (option.isLocked) {
                Toast.makeText(this, "Önce 5 otel odası inşa etmelisin! (Şu an: $hotelRoomCount)", Toast.LENGTH_SHORT).show()
            } else {
                createFloor(option.type, option.name, option.baseCost, option.baseEarnings)
            }
        }
        bottomSheet.show(supportFragmentManager, "RoomSelectionBottomSheet")
    }

    private fun createFloor(type: RoomType, name: String, cost: Double, baseEarnings: Double) {
        if (totalMoney >= cost) {
            totalMoney -= cost
            
            val newFloor = Floor(
                id = floorList.size + 1,
                name = name,
                level = 1,
                earningsPerSecond = baseEarnings,
                upgradeCost = cost * 1.5,
                baseCost = cost,
                type = type
            )
            
            // Özel Oda Tipleri İçin Başlangıç Değerleri
            when(type) {
                RoomType.KITCHEN -> {
                    newFloor.producesFoodPerSec = 5
                    newFloor.earningsPerSecond = 0.0
                }
                RoomType.RESTAURANT -> {
                    newFloor.consumesFoodPerSec = 3
                }
                else -> {}
            }
            
            floorList.add(newFloor)
            
            when(type) {
                RoomType.HOTEL_ROOM -> nextHotelRoomCost *= 1.4
                RoomType.RESTAURANT -> nextRestaurantCost *= 1.5
                RoomType.SPA -> nextSpaCost *= 1.6
                RoomType.BAR -> nextBarCost *= 1.7
                RoomType.LAUNDRY -> nextLaundryCost *= 1.8
                RoomType.KITCHEN -> nextKitchenCost *= 1.5
                else -> {}
            }
            
            adapter.notifyItemInserted(floorList.size - 1)
            recyclerView.scrollToPosition(floorList.size - 1)
            updateMoneyDisplay()
            
            Toast.makeText(this, "$name inşa edildi!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Yetersiz bakiye!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMoneyDisplay() {
        txtTotalMoney.text = String.format(Locale.getDefault(), "Kasa: %.2f $", totalMoney)
    }

    private fun updateFoodDisplay() {
        txtFoodStock.text = String.format(Locale.getDefault(), "Yemek Stoğu: %d", foodStock)
    }

    private fun startGameLoop() {
        lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                
                // 1. Mutfaklarda yemek üretimi
                val kitchens = floorList.filter { it.type == RoomType.KITCHEN }
                for (kitchen in kitchens) {
                    foodStock += kitchen.producesFoodPerSec * kitchen.staffCount
                }

                // 2. Restoranlarda yemek tüketimi ve para kazanma
                val restaurants = floorList.filter { it.type == RoomType.RESTAURANT }
                for (restaurant in restaurants) {
                    if (foodStock >= restaurant.consumesFoodPerSec) {
                        foodStock -= restaurant.consumesFoodPerSec
                        totalMoney += restaurant.earningsPerSecond * restaurant.staffCount
                    }
                }

                // 3. Diğer odalardan para kazanma
                val otherRooms = floorList.filter { 
                    it.type != RoomType.RESTAURANT && it.type != RoomType.KITCHEN && it.type != RoomType.RECEPTION 
                }
                totalMoney += otherRooms.sumOf { it.earningsPerSecond }

                updateMoneyDisplay()
                updateFoodDisplay()
            }
        }
    }
}
