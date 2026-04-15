package com.example.tycon_hotel

enum class QuestType {
    BUILD_HOTEL_ROOM, UPGRADE_ROOM, BUILD_RESTAURANT, HIRE_STAFF
}

data class Quest(
    val id: Int,
    val description: String,
    val type: QuestType,
    val target: Int,
    var progress: Int = 0,
    val reward: Double,
    var isCompleted: Boolean = false
)
