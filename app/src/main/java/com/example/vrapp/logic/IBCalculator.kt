package com.example.vrapp.logic

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object IBCalculator {

    data class DailyGuide(
        val buyAmountPerSlot: Double,
        val starPercentage: Double,
        val starPoint: Double,
        val avgPrice: Double,
        val limitSellPrice: Double, // 목표 수익 지정가 매도 가격
        val bigNumberPrice: Double, // 증권사 거부 방지용 큰수
        
        // 실전 주문 수량
        val buy1AvgPrice: Double,
        val buy1Qty: Double,
        val buy2StarPrice: Double,
        val buy2Qty: Double,
        
        val sell1StarPrice: Double,
        val sell1Qty: Double,
        val sell2LimitPrice: Double,
        val sell2Qty: Double,

        val isReverseMode: Boolean,
        val firstHalf: Boolean,
        val message: String = ""
    )

    private enum class MarketType {
        KOSPI, KOSDAQ, US, JAPAN, OTHER
    }

    private fun getMarketType(ticker: String, currency: String): MarketType {
        return when {
            ticker.uppercase().endsWith(".KS") -> MarketType.KOSPI
            ticker.uppercase().endsWith(".KQ") -> MarketType.KOSDAQ
            ticker.uppercase().endsWith(".T") -> MarketType.JAPAN
            currency == "USD" -> MarketType.US
            currency == "JPY" -> MarketType.JAPAN
            currency == "KRW" -> MarketType.KOSPI
            else -> MarketType.OTHER
        }
    }

    private fun getTickSize(price: Double, market: MarketType): Double {
        return when (market) {
            MarketType.KOSPI -> when {
                price < 1000 -> 1.0
                price < 5000 -> 5.0
                price < 10000 -> 10.0
                price < 50000 -> 50.0
                price < 100000 -> 100.0
                price < 500000 -> 50.0
                else -> 1000.0
            }
            MarketType.KOSDAQ -> when {
                price < 1000 -> 1.0
                price < 5000 -> 5.0
                price < 10000 -> 10.0
                price < 50000 -> 50.0
                else -> 100.0
            }
            MarketType.US -> 0.01
            MarketType.JAPAN -> when {
                price < 3000 -> 1.0
                price < 5000 -> 5.0
                price < 30000 -> 10.0
                price < 50000 -> 10.0 
                price < 300000 -> 100.0
                price < 500000 -> 1000.0
                else -> 1000.0
            }
            MarketType.OTHER -> if (price > 500) 1.0 else 0.01
        }
    }

    private fun adjustPrice(rawPrice: Double, isBuy: Boolean, market: MarketType): Double {
        val tick = getTickSize(rawPrice, market)
        if (tick == 0.0) return rawPrice
        
        return if (isBuy) {
            ceil(rawPrice / tick) * tick
        } else {
            floor(rawPrice / tick) * tick
        }
    }

    /**
     * 별% 계산 (일반화 수식)
     */
    fun calculateStarPercentage(volatility: Double, divisions: Int, tValue: Double): Double {
        if (divisions <= 0) return 0.0
        return volatility - ((volatility * 2.0) / divisions.toDouble()) * tValue
    }

    /**
     * 오늘의 1회 매수 시도 금액 산출
     */
    fun calculateDailyBuyAmount(remainingPool: Double, divisions: Int, tValue: Double): Double {
        val divisor = max(1.0, divisions.toDouble() - tValue)
        return remainingPool / divisor
    }

    /**
     * 오늘의 매매 가이드 생성
     */
    fun generateDailyGuide(
        ticker: String,
        currency: String,
        avgPrice: Double,
        currentPrice: Double,
        remainingPool: Double,
        divisions: Int,
        volatility: Double,
        tValue: Double,
        totalQuantity: Double,
        isReverseMode: Boolean,
        sma5Day: Double? = null
    ): DailyGuide {
        val market = getMarketType(ticker, currency)
        
        // 1. 기본 수치 계산
        val starPct = if (isReverseMode) 0.0 else calculateStarPercentage(volatility, divisions, tValue)
        val rawStarPoint = if (isReverseMode) (sma5Day ?: currentPrice) else avgPrice * (1.0 + (starPct / 100.0))
        val starPoint = adjustPrice(rawStarPoint, false, market)
        
        val limitSellPrice = adjustPrice(avgPrice * (1.0 + (volatility / 100.0)), false, market)
        val bigNumber = adjustPrice(currentPrice * 1.13, true, market)
        val dailyBudget = calculateDailyBuyAmount(remainingPool, divisions, tValue)

        // 2. 매수 수량 상세 계산
        var b1Price = 0.0; var b1Qty = 0.0; var b2Price = 0.0; var b2Qty = 0.0
        
        if (!isReverseMode) {
            if (tValue < (divisions / 2.0)) {
                // 전반전: 평단 반, 별지점 반
                b1Price = adjustPrice(avgPrice, true, market)
                b1Qty = floor((dailyBudget / 2.0) / max(0.01, b1Price))
                
                b2Price = adjustPrice(starPoint - 0.01, true, market)
                b2Qty = floor((dailyBudget / 2.0) / max(0.01, b2Price))
                
                // 보정: 둘 다 0주면 평단으로 1주라도 사기 시도 (소액 계좌 대응)
                if (b1Qty == 0.0 && b2Qty == 0.0 && dailyBudget > b1Price && b1Price > 0) {
                    b1Qty = 1.0
                }
            } else {
                // 후반전: 별지점 올인
                b1Price = adjustPrice(starPoint - 0.01, true, market)
                b1Qty = floor(dailyBudget / max(0.01, b1Price))
                b2Price = 0.0; b2Qty = 0.0
            }
        } else {
            // 리버스: 가용 잔액의 1/4을 별지점 아래 LOC
            b1Price = adjustPrice(starPoint - 0.01, true, market)
            b1Qty = floor((remainingPool / 4.0) / max(0.01, b1Price))
        }

        // 3. 매도 수량 상세 계산
        var s1Price = 0.0; var s1Qty = 0.0; var s2Price = 0.0; var s2Qty = 0.0
        
        if (!isReverseMode) {
            // 일반: 쿼터 매도(별지점 LOC), 나머지 올매도(지정가)
            s1Price = starPoint
            s1Qty = floor(totalQuantity * 0.25)
            
            s2Price = limitSellPrice
            s2Qty = totalQuantity - s1Qty
        } else {
            // 리버스: 직전 수량의 5~10% 비중 무조건 매도 (MOC 권장하나 편의상 별지점 위 LOC로 계산 지원)
            s1Price = starPoint
            s1Qty = floor(totalQuantity / (divisions / 2.0)) // 40분할이면 5%, 20분할이면 10%
            s2Price = 0.0; s2Qty = 0.0
        }

        return DailyGuide(
            buyAmountPerSlot = dailyBudget,
            starPercentage = starPct,
            starPoint = starPoint,
            avgPrice = avgPrice,
            limitSellPrice = limitSellPrice,
            bigNumberPrice = bigNumber,
            buy1AvgPrice = b1Price, buy1Qty = b1Qty,
            buy2StarPrice = b2Price, buy2Qty = b2Qty,
            sell1StarPrice = s1Price, sell1Qty = s1Qty,
            sell2LimitPrice = s2Price, sell2Qty = s2Qty,
            isReverseMode = isReverseMode,
            firstHalf = tValue < (divisions / 2.0)
        )
    }

    /**
     * 거래 후 다음 T값 계산
     */
    fun calculateNextT(
        currentT: Double,
        divisions: Int,
        isReverseMode: Boolean,
        actionType: String
    ): Double {
        return if (!isReverseMode) {
            when (actionType) {
                "BUY_FULL" -> currentT + 1.0
                "BUY_HALF" -> currentT + 0.5
                "SELL_QUARTER" -> currentT * 0.75
                "SELL_LIMIT_LOC_BUY_FULL" -> (currentT * 0.25) + 1.0
                "SELL_LIMIT_LOC_BUY_HALF" -> (currentT * 0.25) + 0.5
                else -> currentT
            }
        } else {
            when (actionType) {
                "REVERSE_SELL" -> currentT * (1.0 - (2.0 / divisions.toDouble()))
                "REVERSE_BUY" -> currentT + (divisions.toDouble() - currentT) * 0.25
                else -> currentT
            }
        }
    }

    /**
     * 초기 T값 자동 추정
     */
    fun estimateInitialT(principal: Double, divisions: Int, currentInvestedAmount: Double): Double {
        if (principal <= 0 || divisions <= 0) return 0.0
        val perSlot = principal / divisions.toDouble()
        return currentInvestedAmount / perSlot
    }
}
