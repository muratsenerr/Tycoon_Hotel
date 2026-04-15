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
    private lateinit var txtQuestCount: TextView
    private lateinit var btnQuests: android.view.View
    private lateinit var btnBuyFloor: Button
    private val floorList = mutableListOf<Floor>()
    private var totalMoney: Double = 50.0
    private var foodStock: Double = 0.0
    
    private var nextHotelRoomCost: Double = 50.0
    private var nextRestaurantCost: Double = 2000.0
    private var nextSpaCost: Double = 4000.0
    private var nextBarCost: Double = 10000.0
    private var nextLaundryCost: Double = 12000.0
    private var nextKitchenCost: Double = 1000.0

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("TycoonHotelPrefs", Context.MODE_PRIVATE)

        txtTotalMoney = findViewById(R.id.txtTotalMoney)
        txtFoodStock = findViewById(R.id.txtFoodStock)
        txtQuestCount = findViewById(R.id.txtQuestCount)
        btnQuests = findViewById(R.id.btnQuests)
        btnBuyFloor = findViewById(R.id.btnBuyFloor)
        
        loadGameData()
        setupRecyclerView()
        
        // Dinamik görev sistemini ilk kez çalıştır
        QuestManager.evaluateGameRules(this, floorList)
        updateQuestUI()

        btnQuests.setOnClickListener {
            showQuestsDialog()
        }

        QuestManager.setMoneyCallback { reward ->
            totalMoney += reward
            updateMoneyDisplay()
        }
        
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
        editor.putString("food_stock", foodStock.toString())
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
        fun loadSafeDouble(key: String, default: Double): Double {
            return try {
                sharedPreferences.getString(key, default.toString())?.toDouble() ?: default
            } catch (e: ClassCastException) {
                try {
                    sharedPreferences.getInt(key, default.toInt()).toDouble()
                } catch (e2: ClassCastException) {
                    try {
                        sharedPreferences.getFloat(key, default.toFloat()).toDouble()
                    } catch (e3: ClassCastException) {
                        default
                    }
                }
            }
        }

        totalMoney = loadSafeDouble("total_money", 50.0)
        foodStock = loadSafeDouble("food_stock", 0.0)
        
        nextHotelRoomCost = loadSafeDouble("next_hotel_room_cost", 50.0)
        nextRestaurantCost = loadSafeDouble("next_restaurant_cost", 2000.0)
        nextSpaCost = loadSafeDouble("next_spa_cost", 4000.0)
        nextBarCost = loadSafeDouble("next_bar_cost", 10000.0)
        nextLaundryCost = loadSafeDouble("next_laundry_cost", 12000.0)
        nextKitchenCost = loadSafeDouble("next_kitchen_cost", 1000.0)

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
            hours > 0 -> String.format(Locale.getDefault(), "%d saat %d dk", hours.toInt(), minutes.toInt())
            minutes > 0 -> String.format(Locale.getDefault(), "%d dk %d sn", minutes.toInt(), remainingSeconds.toInt())
            else -> String.format(Locale.getDefault(), "%d sn", remainingSeconds.toInt())
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
        
        adapter = FloorAdapter(
            floorList = floorList,
            globalFoodStock = foodStock,
            onUpgradeClick = { floor -> handleUpgrade(floor) },
            onHireStaffClick = { floor -> handleHireStaff(floor) },
            onTransferClick = { floor -> handleTransferFood(floor) },
            onUpgradeStorageClick = { floor -> handleUpgradeStorage(floor) }
        )
        
        recyclerView.adapter = adapter
    }

    fun updateQuestUI() {
        val activeCount = QuestManager.getActiveQuestsCount()
        txtQuestCount.text = activeCount.toString()
        txtQuestCount.visibility = if (activeCount > 0) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun showQuestsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quests, null)
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.questsContainer)
        
        // Sadece tamamlanmamış ilk 2 görevi getir
        val visibleQuests = QuestManager.getVisibleQuests(2)
        
        visibleQuests.forEach { quest ->
            val itemView = layoutInflater.inflate(R.layout.item_quest, container, false)
            val desc = itemView.findViewById<TextView>(R.id.txtQuestDescription)
            val progress = itemView.findViewById<TextView>(R.id.txtQuestProgress)
            val reward = itemView.findViewById<TextView>(R.id.txtQuestReward)
            
            desc.text = quest.description
            progress.text = "İlerleme: ${quest.progress}/${quest.target}"
            reward.text = "Ödül: $${quest.reward}"

            if (quest.isCompleted) {
                itemView.alpha = 0.4f
                val strikeThrough = android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                desc.paintFlags = desc.paintFlags or strikeThrough
                progress.paintFlags = progress.paintFlags or strikeThrough
                reward.paintFlags = reward.paintFlags or strikeThrough
            }
            
            container.addView(itemView)
        }

        if (visibleQuests.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "Tüm görevler tamamlandı! Yeni görevler yakında..."
            emptyText.setPadding(20, 20, 20, 20)
            container.addView(emptyText)
        }

        AlertDialog.Builder(this)
            .setTitle("Aktif Görevler")
            .setView(dialogView)
            .setPositiveButton("Kapat") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun getFloorList(): List<Floor> = floorList

    private fun handleTransferFood(floor: Floor) {
        val hasRestaurant = floorList.any { it.type == RoomType.RESTAURANT }
        if (!hasRestaurant) {
            Toast.makeText(this, "Henüz restoran almadınız!", Toast.LENGTH_SHORT).show()
            return
        }

        if (floor.type == RoomType.KITCHEN && floor.internalFoodStock >= 1) {
            val amount = floor.internalFoodStock.toInt()
            floor.internalFoodStock -= amount
            foodStock += amount
            adapter.globalFoodStock = foodStock
            adapter.notifyDataSetChanged()
            updateFoodDisplay()
            Toast.makeText(this, "$amount yemek restorana aktarıldı!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUpgradeStorage(floor: Floor) {
        if (totalMoney >= floor.upgradeStorageCost) {
            totalMoney -= floor.upgradeStorageCost
            updateMoneyDisplay()
            
            floor.maxFoodStorage += 30 // Depoyu 30 birim büyüt
            floor.upgradeStorageCost *= 2.5 // Maliyeti artır
            
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Depo büyütüldü! Yeni Kapasite: ${floor.maxFoodStorage}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Yetersiz bakiye!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUpgrade(floor: Floor) {
        if (floor.type == RoomType.RECEPTION) return

        if (totalMoney >= floor.upgradeCost) {
            totalMoney -= floor.upgradeCost
            updateMoneyDisplay()

            floor.level++
            floor.earningsPerSecond *= 1.20
            if (floor.type == RoomType.KITCHEN) {
                floor.producesFoodPerSec *= 1.2
            }
            floor.upgradeCost = floor.baseCost * (1.30.pow(floor.level.toDouble()))

            adapter.notifyDataSetChanged()
            
            // Görev Sistemi Tetikleyici
            QuestManager.onEvent(this, QuestType.UPGRADE_ROOM)
        } else {
            Toast.makeText(this, "Yetersiz bakiye!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleHireStaff(floor: Floor) {
        if (totalMoney >= floor.staffCount * floor.staffCost) { // Not: Burada basitlik için staffCost kullandım
            // Mevcut kodunuzdaki staffCost mantığını koruyalım:
            if (totalMoney >= floor.staffCost) {
                totalMoney -= floor.staffCost
                updateMoneyDisplay()

                floor.staffCount++
                floor.staffCost *= 1.40 
                
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Personel işe alındı!", Toast.LENGTH_SHORT).show()
                
                // Görev Sistemi Tetikleyici
                QuestManager.onEvent(this, QuestType.HIRE_STAFF)
            }
        } else {
            Toast.makeText(this, "Yetersiz bakiye!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBuyFloorDialog() {
        val hotelRoomCount = floorList.count { it.type == RoomType.HOTEL_ROOM }
        val kitchenCount = floorList.count { it.type == RoomType.KITCHEN }
        
        val costs = mapOf(
            RoomType.HOTEL_ROOM to nextHotelRoomCost,
            RoomType.RESTAURANT to nextRestaurantCost,
            RoomType.SPA to nextSpaCost,
            RoomType.BAR to nextBarCost,
            RoomType.LAUNDRY to nextLaundryCost,
            RoomType.KITCHEN to nextKitchenCost
        )

        val bottomSheet = RoomSelectionBottomSheet(costs, hotelRoomCount, kitchenCount) { option ->
            if (option.isLocked) {
                if (option.type == RoomType.RESTAURANT) {
                    Toast.makeText(this, "Önce bir Mutfak inşa etmelisin!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Önce 5 otel odası inşa etmelisin! (Şu an: $hotelRoomCount)", Toast.LENGTH_SHORT).show()
                }
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
                    newFloor.producesFoodPerSec = 0.2 // 5 saniyede 1 yemek
                    newFloor.earningsPerSecond = 0.0
                    newFloor.staffCount = 1
                }
                RoomType.RESTAURANT -> {
                    newFloor.consumesFoodPerSec = 5.0 // 5 adet yemek biriktiğinde para kazandırır
                }
                else -> {}
            }
            
            floorList.add(newFloor)
            
            // Görev Sistemi Tetikleyici
            when(type) {
                RoomType.HOTEL_ROOM -> QuestManager.onEvent(this, QuestType.BUILD_HOTEL_ROOM)
                RoomType.RESTAURANT -> QuestManager.onEvent(this, QuestType.BUILD_RESTAURANT)
                else -> {}
            }
            
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
        txtFoodStock.text = String.format(Locale.getDefault(), "Yemek: %d", foodStock.toInt())
    }

    private fun startGameLoop() {
        lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                
                // 1. Mutfaklarda yemek üretimi (Hammadde biriktirme)
                val kitchens = floorList.filter { it.type == RoomType.KITCHEN }
                for (kitchen in kitchens) {
                    if (kitchen.internalFoodStock < kitchen.maxFoodStorage) {
                        kitchen.internalFoodStock += kitchen.producesFoodPerSec * kitchen.staffCount
                        // Kapasiteyi aşarsa tam kapasitede sabitle
                        if (kitchen.internalFoodStock > kitchen.maxFoodStorage) {
                            kitchen.internalFoodStock = kitchen.maxFoodStorage.toDouble()
                        }
                    }
                }

                // 2. Restoranlarda yemek hazırlama ve satma (20 saniyelik işlem)
                val restaurants = floorList.filter { it.type == RoomType.RESTAURANT }
                for (restaurant in restaurants) {
                    if (restaurant.restaurantTimer > 0) {
                        restaurant.restaurantTimer--
                        if (restaurant.restaurantTimer == 0) {
                            // 20 saniye doldu, yemek satıldı
                            totalMoney += restaurant.earningsPerSecond * restaurant.staffCount
                            updateMoneyDisplay()
                        }
                    } else if (foodStock >= 5) {
                        // Yeni bir yemek yapmaya başla
                        foodStock -= 5
                        restaurant.restaurantTimer = 20
                        updateFoodDisplay()
                    }
                }

                // 3. Diğer odalardan para kazanma
                val otherRooms = floorList.filter { 
                    it.type != RoomType.RESTAURANT && it.type != RoomType.KITCHEN && it.type != RoomType.RECEPTION 
                }
                for (room in otherRooms) {
                    totalMoney += room.earningsPerSecond
                }

                updateMoneyDisplay()
                adapter.globalFoodStock = foodStock
                adapter.notifyDataSetChanged() // Depo ve timer bilgilerini güncellemek için
            }
        }
    }
}
