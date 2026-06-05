package com.example.vrapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_asset_history")
data class DailyAssetHistory(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val totalPrincipal: Double,
    val totalCurrentValue: Double,
    @ColumnInfo(defaultValue = "0.0") val ibPrincipal: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val ibCurrentValue: Double = 0.0
)
