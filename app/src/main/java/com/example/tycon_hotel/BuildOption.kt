package com.example.tycon_hotel

data class BuildOption(
    val type: RoomType,
    val name: String,
    val baseCost: Double,
    val baseEarnings: Double,
    val iconResId: Int,
    var isLocked: Boolean = false
)
