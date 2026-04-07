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
    var producesFoodPerSec: Int = 0,
    var consumesFoodPerSec: Int = 0
)
