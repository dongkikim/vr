package com.example.vrapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 무한매수 지갑 변동 내역
 */
@Entity(tableName = "ib_wallet_history")
data class IbWalletHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currency: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "DEPOSIT"(입금), "WITHDRAW"(출금), "ALLOCATE"(종목할당), "RETURN"(사이클/삭제 반환)
    val amount: Double, // 변동 금액 (음수/양수)
    val balanceAfter: Double, // 변동 후 지갑 잔액
    val description: String // 내역 설명 (예: "수동 충전", "SOXL 1회차 정산 반환")
)
