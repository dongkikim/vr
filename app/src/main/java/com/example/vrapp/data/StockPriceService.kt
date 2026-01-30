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
        JAPAN(".T", "JPY", "일본"),
        COIN(".bithumb", "KRW", "코인"),
        GOLD(".gold", "KRW", "금")
    }

    // 종목 정보 데이터 클래스
    data class StockInfo(
        val name: String,
        val price: Double,
        val currency: String,
        val ticker: String
    )

    suspend fun getStockPrice(ticker: String, currency: String = "KRW"): Double {
        return withContext(Dispatchers.IO) {
            // 코인 판별 (단순히 ticker에 숫자가 아닌 문자가 포함되어 있고 suffix가 없는 경우 등, 
            // 하지만 여기서는 호출자가 마켓 정보를 알기 어려우므로, 
            // 기존 로직 유지하되 만약 ticker가 코인 포맷이라면 빗썸 시도? 
            // -> 아키텍처상 Market 정보를 넘겨받지 못하는 구조임. 
            // -> 일단 기존 로직은 Yahoo 위주. 
            // -> 하지만 AddStockDialog에서 저장할때 ticker에 suffix를 붙이지 않기로 하면 구분 어려움.
            // -> 사용자가 입력한 Ticker가 빗썸에 있는지 확인? 
            // -> 문제: VRApp의 Stock 엔티티에 market 정보가 없음. ticker 문자열만 있음.
            // -> 해결: 저장할 때 COIN은 별도 prefix/suffix를 붙이거나, 
            // -> 기존 코드: "yahooTicker = ... else ticker". 
            // -> 빗썸 티커는 보통 "BTC", "ETH" 등. 미국 주식과 겹칠 수 있음.
            // -> ***중요***: 사용자가 "코인"을 선택해서 저장하면, ticker를 "BTC"라고 저장함.
            // -> 미국주식 "BTC" (ETF 등)와 겹침.
            // -> 해결책: 저장 시 구분자 추가. 예: "BTC.KRW-BITHUMB" ? 
            // -> 요청사항: "기존 yahoo에서 실시간으로 가져오는 로직은 절대 건드리지말고"
            // -> 그러므로 ticker가 순수 알파벳이면 미국주식으로 간주될 수 있음.
            // -> ***전략***: 코인 저장 시 ticker에 특정 식별자를 붙여야 함. 
            // -> 사용자가 "BTC" 입력 -> 저장 시 "BTC/COIN" 등으로 저장?
            // -> 아니면 getStockPrice 호출 시 빗썸 API 먼저 찔러보기? (비효율적)
            // -> AddStockDialog에서 Market을 선택하므로, 저장할 때 Ticker에 마킹을 하는 것이 좋음.
            // -> 예: "BTC" (코인) -> "BTC"로 저장하면 미국주식으로 오인됨.
            // -> ***결정***: 코인 ticker는 내부적으로 "BTC.bithumb" 처럼 저장하고, getStockPrice에서 이를 감지하여 처리.
            
            if (ticker.endsWith(".bithumb")) {
                val cleanTicker = ticker.removeSuffix(".bithumb")
                getBithumbPrice(cleanTicker)
            } else if (ticker.endsWith(".gold")) {
                getGoldPrice()
            } else {
                // 이미 suffix가 있으면 그대로 사용
                val yahooTicker = when {
                    ticker.contains(".") -> ticker  // 이미 suffix 있음 (005930.KS, 035420.KQ, 7203.T 등)
                    currency == "JPY" -> "$ticker.T"  // 일본 주식 (suffix 없는 레거시 데이터용)
                    ticker.matches(Regex("\\d{6}")) -> "$ticker.KS"  // 한국 주식 (suffix 없는 레거시 데이터용)
                    else -> ticker  // 미국 주식
                }
                Log.d(TAG, "getStockPrice: $ticker -> $yahooTicker")
                getYahooStockPrice(yahooTicker)
            }
        }
    }

    /**
     * 종목 티커와 증시로 종목명, 가격, 통화 정보를 가져옵니다.
     */
    suspend fun getStockInfo(ticker: String, market: Market): StockInfo? {
        return withContext(Dispatchers.IO) {
            try {
                if (market == Market.COIN) {
                    val price = getBithumbPrice(ticker)
                    if (price > 0) {
                         StockInfo(
                            name = ticker, // 빗썸은 종목명을 따로 주지 않으므로 티커 사용 (필요시 별도 매핑)
                            price = price,
                            currency = "KRW",
                            ticker = ticker
                        )
                    } else {
                        null
                    }
                } else if (market == Market.GOLD) {
                    val price = getGoldPrice()
                    if (price > 0) {
                        StockInfo(
                            name = "순금 (1g)",
                            price = price,
                            currency = "KRW",
                            ticker = "GOLD"
                        )
                    } else {
                        null
                    }
                } else {
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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stock info: $ticker", e)
                null
            }
        }
    }
    
    private fun getBithumbPrice(ticker: String): Double {
        try {
            val url = "https://api.bithumb.com/public/ticker/${ticker}_KRW"
            Log.d(TAG, "Fetching coin price for: $ticker from $url")
            
            val response = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(5000)
                .execute()
                .body()
                
            // JSON Parsing
            // {"status":"0000","data":{"opening_price":"...","closing_price":"167200", ...}}
            val closingPriceRegex = """"closing_price":"(\d+(\.\d+)?)"""".toRegex()
            val match = closingPriceRegex.find(response)
            
            return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching coin price: $ticker", e)
            return 0.0
        }
    }

    private fun getGoldPrice(): Double {
        val userAgent = "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
        
        try {
            // 네이버 페이 증권 국내 금 시세 페이지 (모바일)
            val url = "https://m.stock.naver.com/marketindex/metals/M04020000"
            Log.d(TAG, "Fetching gold price from Naver: $url")

            val doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(10000)
                .get()

            // HTML 본문에서 "closePrice":"253,000" 형태를 찾음 (JSON 데이터 내)
            val bodyHtml = doc.html()
            val priceRegex = """"closePrice":"([\d,]+)"""".toRegex()
            val match = priceRegex.find(bodyHtml)
            
            if (match != null) {
                val priceStr = match.groupValues[1].replace(",", "")
                val price = priceStr.toDoubleOrNull() ?: 0.0
                Log.d(TAG, "Naver Gold Price Found: $price (per 1g)")
                return price
            } else {
                Log.d(TAG, "Naver Gold Price Regex failed. Trying fallback selector...")
                // Fallback: DetailInfo_price 클래스 계열 탐색
                val priceElement = doc.select("strong[class*=price]").firstOrNull()
                if (priceElement != null) {
                    val priceStr = priceElement.ownText().replace(",", "").trim()
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    if (price > 0) {
                        Log.d(TAG, "Naver Gold Price Found via Selector: $price")
                        return price
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching gold price from Naver", e)
        }

        return 0.0
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
