package com.example.vrapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 무한매수 전용 종목 엔티티
 */
@Entity(tableName = "ib_stocks")
data class IbStock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ticker: String,
    val currency: String = "USD", // 기본적으로 무매는 미국장 ETF 타겟
    val currentPrice: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),

    // 자금 관련
    val principal: Double, // 지갑에서 할당받은 원금
    var pool: Double,      // 현재 남은 잔금 (할당원금 + 수익금 - 매수액 + 매도액)
    var quantity: Double = 0.0, // 현재 보유 수량

    // 설정 관련
    val divisions: Int = 40, // 20, 30, 40 분할
    val volatility: Double = 15.0, // 목표 변동성 (10, 15, 20%)

    // 진행 상태 관련
    var currentT: Double = 0.0, // 현재 진행 회차
    var isReverseMode: Boolean = false, // 리버스 모드 여부
    var cycleCount: Int = 1, // 현재 진행 중인 사이클 회차 (1회차, 2회차...)
    @ColumnInfo(defaultValue = "0.0") val totalRealizedProfit: Double = 0.0, // 과거 사이클에서 실현된 누적 손익 합계
    // [main][2026-06-05] 매도 수익이 평단가를 왜곡하는 문제 해결을 위해 명시적 평단가 필드 추가
    @ColumnInfo(defaultValue = "0.0") val averagePrice: Double = 0.0
)
