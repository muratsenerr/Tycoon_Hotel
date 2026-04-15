package com.example.tycon_hotel

import android.content.Context
import android.widget.Toast

object QuestManager {
    private val quests = mutableListOf<Quest>()
    private var onMoneyEarned: ((Double) -> Unit)? = null
    
    // Daha önce verilmiş/tamamlanmış özel görevleri takip eden set
    private val triggeredUniqueQuests = mutableSetOf<String>()

    fun setMoneyCallback(callback: (Double) -> Unit) {
        onMoneyEarned = callback
    }

    fun getAllQuests(): List<Quest> = quests

    fun getVisibleQuests(limit: Int = 2): List<Quest> {
        val completed = quests.filter { it.isCompleted }
        val uncompleted = quests.filter { !it.isCompleted }.take(limit)
        return completed + uncompleted
    }

    fun getActiveQuestsCount(): Int = quests.count { !it.isCompleted }

    fun evaluateGameRules(context: Context, floorList: List<Floor>) {
        val hotelRoomCount = floorList.count { it.type == RoomType.HOTEL_ROOM }
        val restaurantCount = floorList.count { it.type == RoomType.RESTAURANT }
        val laundryCount = floorList.count { it.type == RoomType.LAUNDRY }

        // Kural 1: Kademeli Otel Odası Görevleri (Early, Mid, Late Game)
        val activeHotelQuest = quests.find { it.type == QuestType.BUILD_HOTEL_ROOM && !it.isCompleted }
        if (activeHotelQuest == null) {
            val targetCount: Int
            val totalReward: Double
            val description: String

            when {
                hotelRoomCount >= 30 -> {
                    targetCount = hotelRoomCount + 10
                    totalReward = 50000.0 // İleri Safha: Toplam 50.000$
                    description = "+10 Otel Odası Daha İnşa Et (İleri Safha)"
                }
                hotelRoomCount >= 10 && restaurantCount >= 2 && laundryCount >= 1 -> {
                    targetCount = hotelRoomCount + 5
                    totalReward = 6000.0 // Orta Safha: Toplam 6000$
                    description = "+5 Otel Odası Daha İnşa Et (Orta Safha)"
                }
                hotelRoomCount < 1 -> {
                    targetCount = 1
                    totalReward = 100.0 // İlk oda ödülü
                    description = "İlk Otel Odasını İnşa Et"
                }
                hotelRoomCount < 5 -> {
                    targetCount = 5
                    totalReward = 1500.0 // 5. oda ödülü (Gelişime göre belirlendi)
                    description = "5 Otel Odasına Ulaş"
                }
                else -> {
                    // 5-9 oda arası veya 10+ olup diğer şartları sağlamayanlar için
                    targetCount = hotelRoomCount + 1
                    totalReward = 400.0
                    description = "${targetCount}. Otel Odasını İnşa Et"
                }
            }
            
            addQuest(Quest(
                id = quests.size + 1,
                description = description,
                type = QuestType.BUILD_HOTEL_ROOM,
                target = targetCount,
                progress = hotelRoomCount,
                reward = totalReward
            ))
        }

        // Kural 2: Çamaşırhane Görevi (5 Oda Şartı)
        if (hotelRoomCount >= 5 && laundryCount == 0 && !triggeredUniqueQuests.contains("LAUNDRY_QUEST")) {
            triggeredUniqueQuests.add("LAUNDRY_QUEST")
            addQuest(Quest(
                id = quests.size + 1,
                description = "İlk Çamaşırhaneni İnşa Et",
                type = QuestType.BUILD_HOTEL_ROOM,
                target = 1,
                progress = 0,
                reward = 1000.0
            ))
        }

        // Kural 3: Personel Görevi (Restoran Şartı)
        if (restaurantCount >= 1 && !triggeredUniqueQuests.contains("CHEF_QUEST")) {
            triggeredUniqueQuests.add("CHEF_QUEST")
            addQuest(Quest(
                id = quests.size + 1,
                description = "Restoranın için ilk Aşçını işe al",
                type = QuestType.HIRE_STAFF,
                target = 1,
                progress = 0,
                reward = 300.0
            ))
        }
        
        if (context is MainActivity) {
            context.updateQuestUI()
        }
    }

    private fun addQuest(quest: Quest) {
        quests.add(quest)
    }

    fun onEvent(context: Context, type: QuestType, amount: Int = 1) {
        var anyCompleted = false
        quests.filter { it.type == type && !it.isCompleted }.forEach { quest ->
            quest.progress += amount
            if (quest.progress >= quest.target) {
                quest.isCompleted = true
                anyCompleted = true
                onMoneyEarned?.invoke(quest.reward)
                Toast.makeText(context, "Görev Tamamlandı: ${quest.description}! +$${quest.reward}", Toast.LENGTH_LONG).show()
            }
        }
        
        if (context is MainActivity) {
            // Bir olay olduğunda kuralları tekrar değerlendir (yeni görev açılabilir)
            evaluateGameRules(context, (context as MainActivity).getFloorList())
            context.updateQuestUI()
        }
    }
}
