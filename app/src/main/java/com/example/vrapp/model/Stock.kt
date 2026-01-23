package com.example.vrapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ticker: String, // e.g., "AAPL", "005930"
    
    // VR Core Variables
    val vValue: Double, // V: Value
    val gValue: Double, // G: Gradient (%)
    val pool: Double,   // Pool: Cash reserve for this stock
    val quantity: Int,  // Held quantity
    
    // Asset Tracking
    val investedPrincipal: Double = 0.0, // Total Invested Capital (Won-geum)
    
    // Display/Tracking
    val currentPrice: Double = 0.0, // Last known price
    val currency: String = "KRW", // KRW, USD, JPY
    
    @ColumnInfo(defaultValue = "0")
    val startDate: Long = System.currentTimeMillis(), // 투자 시작일

    @ColumnInfo(defaultValue = "0.0")
    val defaultRecalcAmount: Double = 0.0 // VR 재계산시 기본 입출금액
)

@Entity(tableName = "transactions")
data class TransactionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockId: Long,
    val date: Long = System.currentTimeMillis(),
    val type: String, // "BUY", "SELL", "RECALC_V", "INIT"
    val price: Double,
    val quantity: Int,
    val amount: Double, // Total value of transaction
    val previousV: Double?,
    val newV: Double?
)
