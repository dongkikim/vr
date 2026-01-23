package com.example.vrapp.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object StockPriceService {

    private const val TAG = "StockPriceService"

    // 증시 종류
    enum class Market(val suffix: String, val currency: String, val label: String) {
        KOSPI(".KS", "KRW", "코스피"),
        KOSDAQ(".KQ", "KRW", "코스닥"),
        US("", "USD", "미국"),
        JAPAN(".T", "JPY", "일본")
    }

    // 종목 정보 데이터 클래스
    data class StockInfo(
        val name: String,
        val price: Double,
        val currency: String,
        val ticker: String
    )

    suspend fun getStockPrice(ticker: String): Double {
        return withContext(Dispatchers.IO) {
            // 이미 suffix가 있으면 그대로 사용, 없으면 숫자 6자리는 .KS 추가
            val yahooTicker = when {
                ticker.contains(".") -> ticker  // 이미 suffix 있음
                ticker.matches(Regex("\\d{6}")) -> "$ticker.KS"  // 한국 주식 (기본 코스피)
                else -> ticker  // 미국 주식
            }
            getYahooStockPrice(yahooTicker)
        }
    }

    /**
     * 종목 티커와 증시로 종목명, 가격, 통화 정보를 가져옵니다.
     */
    suspend fun getStockInfo(ticker: String, market: Market): StockInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val yahooTicker = ticker + market.suffix
                // Yahoo Finance API 사용 (더 안정적)
                val url = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooTicker?modules=price"
                Log.d(TAG, "Fetching stock info for: $yahooTicker from $url")

                val response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .execute()
                    .body()

                // JSON에서 종목명 추출 (shortName 또는 longName)
                var stockName = ""
                val shortNameRegex = """"shortName":\s*"([^"]+)"""".toRegex()
                val longNameRegex = """"longName":\s*"([^"]+)"""".toRegex()

                longNameRegex.find(response)?.let {
                    stockName = it.groupValues[1]
                }
                if (stockName.isBlank()) {
                    shortNameRegex.find(response)?.let {
                        stockName = it.groupValues[1]
                    }
                }
                if (stockName.isBlank()) {
                    stockName = ticker
                }

                // JSON에서 regularMarketPrice 추출
                val priceRegex = """"regularMarketPrice":\s*(\d+\.?\d*)""".toRegex()
                val price = priceRegex.find(response)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

                Log.d(TAG, "Stock info: name=$stockName, price=$price")

                // 가격이 0이거나 종목명이 비어있으면 null 반환 (종목 없음)
                if (price <= 0 || stockName.isBlank()) {
                    Log.w(TAG, "Stock not found: $yahooTicker")
                    null
                } else {
                    StockInfo(
                        name = stockName,
                        price = price,
                        currency = market.currency,
                        ticker = ticker
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stock info: $ticker", e)
                null
            }
        }
    }

    /**
     * 환율 조회 (Yahoo Finance)
     * USDKRW=X, JPYKRW=X 형태의 티커 사용
     */
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String = "KRW"): Double {
        if (fromCurrency == toCurrency) return 1.0
        return withContext(Dispatchers.IO) {
            val ticker = "$fromCurrency$toCurrency=X"
            getYahooStockPrice(ticker)
        }
    }

    private fun getYahooStockPrice(ticker: String): Double {
        try {
            // Yahoo Finance API 사용 (더 안정적)
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker"
            Log.d(TAG, "Fetching price for: $ticker from $url")

            val response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .ignoreContentType(true)
                .timeout(10000)
                .execute()
                .body()

            // JSON에서 regularMarketPrice 추출
            val priceRegex = """"regularMarketPrice":\s*(\d+\.?\d*)""".toRegex()
            val matchResult = priceRegex.find(response)
            val price = matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

            Log.d(TAG, "Price for $ticker: $price")
            return price
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stock price: $ticker", e)
            return 0.0
        }
    }
}
