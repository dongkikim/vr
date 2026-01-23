package com.example.vrapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_asset_history")
data class DailyAssetHistory(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val totalPrincipal: Double,
    val totalCurrentValue: Double
)
