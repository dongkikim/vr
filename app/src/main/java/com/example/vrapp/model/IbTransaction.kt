package com.example.vrapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 무한매수 전용 거래 내역 엔티티
 */
@Entity(tableName = "ib_transactions")
data class IbTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ibStockId: Long,
    val date: Long = System.currentTimeMillis(),
    val type: String, // "BUY_FULL", "BUY_HALF", "SELL_QUARTER", "SELL_FULL", "SPLIT", "RECALC", etc.
    val price: Double,
    val quantity: Double,
    val amount: Double, // 총 거래 금액

    // [롤백용 과거 데이터] - 삭제 시 이전 상태로 복구하기 위함
    val previousT: Double,
    val previousPool: Double,
    val previousIsReverseMode: Boolean,
    @ColumnInfo(defaultValue = "-1")
    val previousCycleCount: Int = -1,

    val cycleNumber: Int // 해당 거래가 발생한 사이클 회차
)
