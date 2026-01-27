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
    val type: String, // "BUY", "SELL", "RECALC_V", "DEPOSIT", "WITHDRAW", etc.
    val price: Double,
    val quantity: Int,
    val amount: Double, // Total value of transaction
    val previousV: Double?,
    val newV: Double?,
    // 원복을 위한 이전 상태 저장 (0이면 마이그레이션 전 데이터로 삭제 불가)
    @ColumnInfo(defaultValue = "-1.0")
    val previousPool: Double = -1.0,
    @ColumnInfo(defaultValue = "-1")
    val previousQuantity: Int = -1,
    @ColumnInfo(defaultValue = "-1.0")
    val previousPrincipal: Double = -1.0
) {
    // 이전 상태 정보가 있는지 확인 (삭제 가능 여부)
    // 수량은 절대 음수가 될 수 없으므로 previousQuantity만 체크 (-1이면 마이그레이션 전 데이터)
    fun canDelete(): Boolean = previousQuantity >= 0
}
