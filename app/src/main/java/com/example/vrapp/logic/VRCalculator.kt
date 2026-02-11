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
        currency: String = "KRW",
        vrPool: Double = -1.0,
        netTradeAmount: Double = 0.0,
        vrQuantity: Double = -1.0
    ): List<PriceByQuantity> {
        val result = mutableListOf<PriceByQuantity>()
        val market = getMarketType(ticker, currency)
        
        // 1. 기준 시점 데이터 확정
        val referencePoolForLimit = if (vrPool >= 0) vrPool else (currentPool + netTradeAmount)
        val referenceQuantity = if (vrQuantity >= 0) vrQuantity else currentQuantity
        
        // 2. 수량 기준 한도 계산
        // 기준 시점 매수가 (Low Valuation / referenceQuantity + 1) -> 1주를 살 때의 당시 가격으로 추정
        // 단, 더 정확하게는 VR 시작 시점의 실제 밴드 가격을 사용하는 것이 좋으나, 
        // 여기서는 기준 시점의 Valuation Band를 현재 수량으로 나누어 수량 한도를 산출함.
        val referenceBuyPrice = if (referenceQuantity > 0) bands.lowValuation / (referenceQuantity + 1) else 0.0
        
        // 25% 금액으로 살 수 있었던 최대 수량 (고정 기준)
        val safeBuyQuantityLimit = if (referenceBuyPrice > 0) {
            floor((referencePoolForLimit * 0.25) / referenceBuyPrice)
        } else {
            0.0
        }

        val totalBuyLimit = currentPool       // 최대 매수 한도 (현재 가용 Pool의 100%)

        // 현재까지 이미 늘어난 수량 (VR 시점 대비)
        val currentAddedQuantity = maxOf(0.0, currentQuantity - referenceQuantity)

        for (i in 1..range) {
            val sellQty = currentQuantity - i  // 매도 후 수량
            val buyQty = currentQuantity + i   // 매수 후 수량
            
            var sellPrice = 0.0
            var buyPrice = 0.0
            var isOver = false

            // 1. 매도 가격 계산
            if (sellQty > 0) {
                val rawSellPrice = bands.highValuation / sellQty
                sellPrice = adjustPrice(rawSellPrice, false, market)
            }

            // 2. 매수 가격 계산
            if (buyQty > 0) {
                val rawBuyPrice = bands.lowValuation / buyQty
                val adjustedBuyPrice = adjustPrice(rawBuyPrice, true, market)
                val purchaseAmount = adjustedBuyPrice * i

                if (adjustedBuyPrice > 0 && purchaseAmount <= totalBuyLimit) {
                    buyPrice = adjustedBuyPrice
                    
                    // 총 늘어나는 수량 = (현재 이미 늘어난 수량) + (이번에 추가할 수량 i)
                    val totalAddedQuantity = currentAddedQuantity + i
                    
                    // 수량 기준으로 25% 한도 초과 여부 판단 (변하지 않는 기준)
                    if (totalAddedQuantity > safeBuyQuantityLimit) {
                        isOver = true
                    }
                }
            }

            if (sellPrice == 0.0 && buyPrice == 0.0) {
                if (i > range) break 
            }

            result.add(PriceByQuantity(i.toDouble(), sellPrice, buyPrice, isOver))
        }

        return result
    }
}
