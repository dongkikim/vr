package com.example.vrapp.logic

import kotlin.math.ceil
import kotlin.math.floor

object VRCalculator {

    data class VRBands(
        val lowPrice: Double,
        val highPrice: Double,
        val lowValuation: Double,
        val highValuation: Double
    )

    data class OrderGuide(
        val action: OrderAction,
        val quantity: Double
    )

    enum class OrderAction {
        BUY, SELL, HOLD
    }

    /**
     * Calculates the new V value.
     * Formula: V_new = V_old + (Pool / G) + poolDeposit
     * @param currentV: Current V value
     * @param pool: Current Pool value (before deposit/withdraw is applied to it, but actually the formula uses the Pool used for calculation)
     *              Wait, user said: "V=현재V + (POOL / G) + Pool입금액".
     *              If "POOL" in (POOL/G) is the *current* pool, then we pass current pool.
     * @param g: Gradient (%)
     * @param poolDeposit: Amount added/subtracted from Pool. Positive for Deposit, Negative for Withdraw.
     */
    fun calculateNewV(currentV: Double, pool: Double, g: Double, poolDeposit: Double = 0.0): Double {
        if (g == 0.0) return currentV
        return currentV + (pool / (g / 1.0)) + poolDeposit
    }

    /**
     * Calculates the Low and High bands based on Value (V) and Gradient (G).
     * Low Valuation = V * (1 - G/100)
     * High Valuation = V * (1 + G/100)
     */
    fun calculateBands(v: Double, g: Double): VRBands {
        val ratio = g / 100.0
        val lowVal = v * (1 - ratio)
        val highVal = v * (1 + ratio)
        
        // Prices are derived. If we want price bands, we need to divide by Quantity, 
        // but bands are usually defined by Total Valuation in VR.
        // We will return Valuation bands. Price bands depend on current quantity.
        return VRBands(
            lowPrice = 0.0, // Dependent on qty, calculated in UI or elsewhere if needed
            highPrice = 0.0,
            lowValuation = lowVal,
            highValuation = highVal
        )
    }

    /**
     * Calculates required Buy/Sell quantity.
     * Buy Qty = (Low - CurrentValuation) / Price (Ceil)
     * Sell Qty = (CurrentValuation - High) / Price (Floor)
     */
    fun calculateOrder(
        currentValuation: Double,
        currentPrice: Double,
        bands: VRBands
    ): OrderGuide {
        if (currentPrice <= 0.0) return OrderGuide(OrderAction.HOLD, 0.0)

        if (currentValuation <= bands.lowValuation) {
            val diff = bands.lowValuation - currentValuation
            // Safety check: if difference is tiny, ignore
            if (diff <= 0) return OrderGuide(OrderAction.HOLD, 0.0)

            // For stocks, we usually want ceil. For coins, maybe exact? 
            // Keeping it simple: Just return the raw amount needed to reach band.
            // But previous logic was ceil/floor for integer stocks.
            // Let's keep it generally precise for now, UI can round if needed.
            // Or better: Use ceil for BUY (to ensure we reach band) and floor for SELL (to ensure we reach band? No, to be safe).
            // Actually, for VR, usually:
            // Buy: (LowVal - CurrentVal) / Price. Result is float.
            // Sell: (CurrentVal - HighVal) / Price. Result is float.
            val qty = diff / currentPrice
            return OrderGuide(OrderAction.BUY, qty)
        } else if (currentValuation >= bands.highValuation) {
            val diff = currentValuation - bands.highValuation
            if (diff <= 0) return OrderGuide(OrderAction.HOLD, 0.0)

            val qty = diff / currentPrice
            return OrderGuide(OrderAction.SELL, qty)
        }

        return OrderGuide(OrderAction.HOLD, 0.0)
    }

    /**
     * 수량별 매매가 계산
     * 수량이 줄어들 때 (매도) = 매도밴드(High Valuation) / qty
     * 수량이 늘어날 때 (매수) = 매수밴드(Low Valuation) / qty
     */
    data class PriceByQuantity(
        val quantity: Double,
        val sellPrice: Double,  // 수량 감소 시 (매도가)
        val buyPrice: Double,    // 수량 증가 시 (매수가)
        val isOverLimit: Boolean = false // 매수 금액이 Pool 25% 초과 여부
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
            currency == "KRW" -> MarketType.KOSPI // Default to KOSPI for KRW
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
                price < 500000 -> 500.0
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

    fun calculatePriceTable(
        currentQuantity: Double,
        currentPool: Double,
        bands: VRBands,
        range: Int = 30,
        ticker: String = "",
        currency: String = "KRW"
    ): List<PriceByQuantity> {
        val result = mutableListOf<PriceByQuantity>()
        val market = getMarketType(ticker, currency)
        val safeBuyLimit = currentPool * 0.25 // 안전 매수 한도 (25%)
        val totalBuyLimit = currentPool       // 최대 매수 한도 (100%)

        for (i in 1..range) {
            val sellQty = currentQuantity - i  // 매도 후 수량
            val buyQty = currentQuantity + i   // 매수 후 수량
            
            var sellPrice = 0.0
            var buyPrice = 0.0
            var isOver = false

            // 1. 매도 가격 계산 (보유 수량이 남아야 가능)
            if (sellQty >= 0) { // 0개도 매도는 가능(전량 매도)하므로 >= 0
                val rawSellPrice = bands.highValuation / sellQty
                // sellQty가 0이면 Infinity가 나오므로 처리 필요하지만, 
                // 전량 매도 시점의 가격을 의미하므로 무한대가 맞음 -> 현실적으로 표시 불가 -> 0 처리?
                // 아니면 전량 매도 시 bands.highValuation 이상이면 됨.
                // 하지만 여기선 테이블 형식이므로 sellQty > 0 일때만 계산
                if (sellQty > 0) {
                     sellPrice = adjustPrice(rawSellPrice, false, market) // 매도: 내림
                }
            }

            // 2. 매수 가격 계산
            val rawBuyPrice = bands.lowValuation / buyQty
            val adjustedBuyPrice = adjustPrice(rawBuyPrice, true, market)   // 매수: 올림
            
            // 매수 총액 = 단가 * 수량(i)
            // 단, adjustedBuyPrice가 0보다 크고, 전체 Pool(100%) 이내여야 함
            if (adjustedBuyPrice > 0 && (adjustedBuyPrice * i) <= totalBuyLimit) {
                buyPrice = adjustedBuyPrice
                // 25% 초과 여부 체크
                if ((adjustedBuyPrice * i) > safeBuyLimit) {
                    isOver = true
                }
            }

            // 둘 다 표시할 수 없으면 루프 종료? -> 아니오, 한쪽이라도 있으면 표시
            if (sellPrice == 0.0 && buyPrice == 0.0) {
                // 하지만 range만큼은 돌리는게 표의 일관성을 위해 좋을수도 있음.
                // 일단 기존 로직 유지 (둘다 0이면 break)
                // 단, i가 작을때는 매수가가 안나와도 매도가는 나올 수 있음.
                if (i > range) break 
            }

            result.add(PriceByQuantity(i.toDouble(), sellPrice, buyPrice, isOver))
        }

        return result
    }
}
