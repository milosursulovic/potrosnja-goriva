package com.example.potronjagoriva

data class FuelEntry(
    val liters: Double,
    val kilometers: Double,
    val consumption: Double,
    val timestamp: Long,
    val id: Long = 0L
)