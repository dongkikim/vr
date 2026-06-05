package com.example.vrapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 무한매수 통합 지갑 (Global IB Wallet)
 */
@Entity(tableName = "ib_accounts")
data class IbAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currency: String, // USD, KRW
    val balance: Double = 0.0, // 현재 지갑 가용 잔액
    val totalInvested: Double = 0.0 // 무매에 최초 투입한 총 원금 (수익률 계산용)
)
