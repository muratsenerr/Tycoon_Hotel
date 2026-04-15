package com.example.tycon_hotel

enum class RoomType {
    RECEPTION, HOTEL_ROOM, RESTAURANT, SPA, BAR, LAUNDRY, KITCHEN
}

data class Floor(
    val id: Int,
    val name: String,
    var level: Int,
    var earningsPerSecond: Double,
    var upgradeCost: Double,
    val baseCost: Double,
    val type: RoomType,
    var staffCount: Int = 1,
    var staffCost: Double = 100.0,
    var producesFoodPerSec: Double = 0.0,
    var consumesFoodPerSec: Double = 0.0,
    var internalFoodStock: Double = 0.0,
    var restaurantTimer: Int = 0,
    var maxFoodStorage: Int = 10,
    var upgradeStorageCost: Double = 500.0
)
