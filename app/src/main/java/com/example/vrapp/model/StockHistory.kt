package com.example.vrapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_history")
data class StockHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockId: Long,
    val timestamp: Long,
    val vValue: Double,
    val gValue: Double,
    val currentPrice: Double,
    val quantity: Double,
    val pool: Double,
    val investedPrincipal: Double
)
