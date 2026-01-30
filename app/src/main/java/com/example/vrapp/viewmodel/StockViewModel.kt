package com.example.vrapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vrapp.data.AppDatabase
import com.example.vrapp.data.StockPriceService
import com.example.vrapp.data.StockRepository
import com.example.vrapp.logic.VRCalculator
import com.example.vrapp.model.Stock
import com.example.vrapp.model.TransactionHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.vrapp.model.DailyAssetHistory

import com.example.vrapp.model.StockHistory

import com.google.gson.Gson
import com.example.vrapp.model.BackupData

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StockRepository
    val allStocks: StateFlow<List<Stock>>
    
    // Daily History Flow
    val dailyHistory: StateFlow<List<DailyAssetHistory>>
    
    private val _currentStock = MutableStateFlow<Stock?>(null)
    val currentStock = _currentStock.asStateFlow()

    private val _transactionHistory = MutableStateFlow<List<TransactionHistory>>(emptyList())
    val transactionHistory = _transactionHistory.asStateFlow()

    private val _stockHistory = MutableStateFlow<List<StockHistory>>(emptyList())
    val stockHistory = _stockHistory.asStateFlow()

    // Internal mutable flow backing the public one
    private val _allStocks = MutableStateFlow<List<Stock>>(emptyList())
    
    // Autocomplete Mock Data (Expanded)
    private val knownStocks = listOf(
        "삼성전자" to "005930",
        "SK하이닉스" to "000660",
        "NAVER" to "035420",
        "카카오" to "035720",
        "현대차" to "005380",
        "Apple (애플)" to "AAPL",
        "Tesla (테슬라)" to "TSLA",
        "Microsoft (MS)" to "MSFT",
        "NVIDIA (엔비디아)" to "NVDA",
        "Google (구글)" to "GOOGL",
        "Amazon (아마존)" to "AMZN",
        "TQQQ (나스닥 3배)" to "TQQQ",
        "SOXL (반도체 3배)" to "SOXL",
        "SCHD (배당성장)" to "SCHD",
        "JEPI (월배당)" to "JEPI"
    )

    data class AssetStatus(
        val totalPrincipal: Double = 0.0,
        val totalCurrent: Double = 0.0,
        val totalROI: Double = 0.0
    )

    // 실시간 환율 (KRW 기준)
    private val _exchangeRates = MutableStateFlow(
        mapOf(
            "KRW" to 1.0,
            "USD" to 1400.0,  // 기본값 (실시간 조회 전)
            "JPY" to 9.0      // 기본값 (실시간 조회 전)
        )
    )

    // Yesterday's Valuation per Stock (Map<StockId, Valuation>)
    private val _yesterdayStockValuations = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val yesterdayStockValuations = _yesterdayStockValuations.asStateFlow()

    // ... (Existing helper methods)

    private suspend fun updateYesterdayValuations() {
        val historyList = repository.getAllStockHistorySnapshot()
        val todayStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )?.time ?: System.currentTimeMillis()

        val map = historyList
            .filter { it.timestamp < todayStart }
            .groupBy { it.stockId }
            .mapValues { (_, histories) ->
                // Get the latest entry before today
                val lastEntry = histories.maxByOrNull { it.timestamp }
                if (lastEntry != null) {
                    (lastEntry.currentPrice * lastEntry.quantity) + lastEntry.pool
                } else {
                    0.0
                }
            }
        _yesterdayStockValuations.value = map
    }

    suspend fun exportData(): String {
        val stocks = repository.getAllStocksSnapshot()
        val transactions = repository.getAllTransactionsSnapshot()
        val daily = repository.getAllDailyHistorySnapshot()
        val history = repository.getAllStockHistorySnapshot()

        val backup = BackupData(
            stocks = stocks,
            transactions = transactions,
            dailyHistory = daily,
            stockHistory = history
        )
        return Gson().toJson(backup)
    }

    suspend fun importData(json: String): Boolean {
        return try {
            val backup = Gson().fromJson(json, BackupData::class.java)
            if (backup == null) return false
            
            repository.clearAllData()
            repository.restoreData(
                backup.stocks,
                backup.transactions,
                backup.dailyHistory,
                backup.stockHistory
            )
            refreshAllPrices()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.clearAllData()
            _currentStock.value = null
            _transactionHistory.value = emptyList()
            _stockHistory.value = emptyList()
            // Force refresh to update flows (might be redundant if flows observe DB, but good for safety)
            _allStocks.value = emptyList() // Or rely on repository flow update
        }
    }

    private fun convertToKRW(amount: Double, currency: String): Double {
        val rate = _exchangeRates.value[currency] ?: 1.0
        return amount * rate
    }

    private suspend fun refreshExchangeRates() {
        try {
            val usdRate = StockPriceService.getExchangeRate("USD", "KRW")
            val jpyRate = StockPriceService.getExchangeRate("JPY", "KRW")

            _exchangeRates.value = mapOf(
                "KRW" to 1.0,
                "USD" to if (usdRate > 0) usdRate else _exchangeRates.value["USD"]!!,
                "JPY" to if (jpyRate > 0) jpyRate else _exchangeRates.value["JPY"]!!
            )
            android.util.Log.d("StockViewModel", "Exchange rates updated: USD=${_exchangeRates.value["USD"]}, JPY=${_exchangeRates.value["JPY"]}")
        } catch (e: Exception) {
            android.util.Log.e("StockViewModel", "Error fetching exchange rates", e)
        }
    }

    // 가격 갱신 중 상태
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        val stockDao = AppDatabase.getDatabase(application).stockDao()
        repository = StockRepository(stockDao)

        allStocks = _allStocks.asStateFlow()

        dailyHistory = repository.dailyHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            // 앱 기동 시 첫 번째 수집에서만 모든 종목 스냅샷 저장
            var isFirstCollect = true
            repository.allStocks.collect { list ->
                _allStocks.value = list
                updateDailySnapshot() // Update snapshot whenever stocks change
                updateYesterdayValuations() // Update yesterday's valuations

                // 앱 기동 시 모든 종목의 당일 스냅샷 저장 (같은 날짜는 마지막 값만 유지)
                if (isFirstCollect && list.isNotEmpty()) {
                    isFirstCollect = false
                    list.forEach { stock ->
                        recordStockHistory(stock)
                    }
                    // 앱 기동 시 가격 갱신
                    refreshAllPrices()
                }
            }
        }
    }

    private fun updateDailySnapshot() {
        viewModelScope.launch {
            // Wait for exchange rates/stocks to be stable? For now just use current values.
            // Using logic similar to AssetStatus
            val stocks = _allStocks.value
            val rates = _exchangeRates.value
            
            val totalPrincipal = stocks.sumOf { it.investedPrincipal * (rates[it.currency] ?: 1.0) }
            val totalCurrent = stocks.sumOf { ((it.currentPrice * it.quantity) + it.pool) * (rates[it.currency] ?: 1.0) }
            
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            val history = DailyAssetHistory(
                date = today,
                totalPrincipal = totalPrincipal,
                totalCurrentValue = totalCurrent
            )
            repository.saveDailyHistory(history)
        }
    }

    private suspend fun recordStockHistory(stock: Stock) {
        val history = StockHistory(
            stockId = stock.id,
            timestamp = System.currentTimeMillis(),
            vValue = stock.vValue,
            gValue = stock.gValue,
            currentPrice = stock.currentPrice,
            quantity = stock.quantity,
            pool = stock.pool,
            investedPrincipal = stock.investedPrincipal
        )
        repository.saveStockHistory(history)

        // Flow가 즉시 트리거되지 않을 수 있으므로, 수동으로 현재 stock의 히스토리 갱신
        if (_currentStock.value?.id == stock.id) {
            _stockHistory.value = repository.getStockHistory(stock.id).first()
        }
    }

    /**
     * 실제 네트워크를 통해 주가를 가져옵니다.
     * (Yahoo API 사용)
     */
    suspend fun fetchRealPrice(ticker: String, currency: String = "KRW"): Double {
        android.util.Log.d("StockViewModel", "Fetching price for: $ticker (currency: $currency)")
        return try {
            val price = StockPriceService.getStockPrice(ticker, currency)
            android.util.Log.d("StockViewModel", "Price fetched: $price")
            price
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("StockViewModel", "Error fetching price", e)
            0.0
        }
    }

    /**
     * 종목 티커와 증시로 종목 정보(이름, 가격, 통화)를 가져옵니다.
     */
    suspend fun fetchStockInfo(ticker: String, market: StockPriceService.Market): StockPriceService.StockInfo? {
        return try {
            StockPriceService.getStockInfo(ticker, market)
        } catch (e: Exception) {
            android.util.Log.e("StockViewModel", "Error fetching stock info", e)
            null
        }
    }

    /**
     * 등록된 모든 종목의 현재가와 환율을 최신 값으로 업데이트합니다.
     */
    fun refreshAllPrices() {
        if (_isRefreshing.value) return // 이미 갱신 중이면 무시

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 환율 갱신
                refreshExchangeRates()

                // 현재 메모리에 로드된 리스트를 순회
                _allStocks.value.forEach { stock ->
                    val latestPrice = fetchRealPrice(stock.ticker, stock.currency)
                    // 가격이 유효하고 변경되었을 경우에만 업데이트
                    if (latestPrice > 0 && latestPrice != stock.currentPrice) {
                        repository.updateStock(stock.copy(currentPrice = latestPrice))
                    }
                }
                updateDailySnapshot() // Snapshot update after price refresh
                updateYesterdayValuations() // Update yesterday's valuations
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    val assetStatus: StateFlow<AssetStatus> = combine(_allStocks, _exchangeRates) { stocks, rates ->
        val principal = stocks.sumOf { it.investedPrincipal * (rates[it.currency] ?: 1.0) }
        val current = stocks.sumOf { ((it.currentPrice * it.quantity) + it.pool) * (rates[it.currency] ?: 1.0) }
        val roi = if (principal > 0) ((current - principal) / principal) * 100 else 0.0
        AssetStatus(principal, current, roi)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AssetStatus()
    )

    fun searchStocks(query: String): List<Pair<String, String>> {
        if (query.isEmpty()) return emptyList()
        return knownStocks.filter { it.first.contains(query, ignoreCase = true) || it.second.contains(query, ignoreCase = true) }
    }

    fun loadStock(id: Long) {
        viewModelScope.launch {
            val stock = repository.getStock(id)
            _currentStock.value = stock

            // 상세화면 진입 시 당일 스냅샷 저장 (같은 날짜는 마지막 값만 유지)
            stock?.let { recordStockHistory(it) }

            launch {
                repository.getHistory(id).collect {
                    _transactionHistory.value = it
                }
            }
            launch {
                repository.getStockHistory(id).collect {
                    _stockHistory.value = it
                }
            }
        }
    }

    fun addStock(name: String, ticker: String, v: Double, g: Double, pool: Double, qty: Double, price: Double, currency: String = "KRW", principal: Double? = null, startDate: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            // 초기 원금: 입력값이 없으면 (Price * Qty) + Pool로 자동 계산
            val initialPrincipal = principal ?: ((price * qty) + pool)

            val stock = Stock(
                name = name,
                ticker = ticker,
                vValue = v,
                gValue = g,
                pool = pool,
                quantity = qty,
                currentPrice = price,
                currency = currency,
                investedPrincipal = initialPrincipal,
                startDate = startDate
            )
            repository.addStock(stock)
            updateDailySnapshot()
        }
    }

    fun updateStockStartDate(stock: Stock, newDate: Long) {
        viewModelScope.launch {
            val updatedStock = stock.copy(startDate = newDate)
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
        }
    }

    fun updatePrice(stock: Stock, newPrice: Double) {
        viewModelScope.launch {
            repository.updateStock(stock.copy(currentPrice = newPrice))
            if (_currentStock.value?.id == stock.id) {
                _currentStock.value = stock.copy(currentPrice = newPrice)
            }
        }
    }

    fun deleteStock(stock: Stock) {
        viewModelScope.launch {
            repository.deleteStock(stock.id)
            updateDailySnapshot()
        }
    }

    fun updateStockName(stock: Stock, newName: String) {
        viewModelScope.launch {
            val updatedStock = stock.copy(name = newName)
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
        }
    }

    fun updateStockPrincipal(stock: Stock, newPrincipal: Double) {
        viewModelScope.launch {
            val updatedStock = stock.copy(investedPrincipal = newPrincipal)
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
            recordStockHistory(updatedStock)
            updateDailySnapshot()
        }
    }
    
    // Manage Pool (Deposit/Withdraw)
    fun updatePool(stock: Stock, amount: Double, isDeposit: Boolean, applyToPrincipal: Boolean = true) {
        viewModelScope.launch {
            var newPool = stock.pool
            var newPrincipal = stock.investedPrincipal

            if (isDeposit) {
                newPool += amount
                if (applyToPrincipal) newPrincipal += amount
            } else {
                newPool -= amount
                if (applyToPrincipal) newPrincipal -= amount
            }

            // 원금 적용 여부에 따라 타입 구분
            val type = when {
                isDeposit && applyToPrincipal -> "DEPOSIT"
                isDeposit && !applyToPrincipal -> "DEPOSIT_POOL_ONLY"
                !isDeposit && applyToPrincipal -> "WITHDRAW"
                else -> "WITHDRAW_POOL_ONLY"
            }

            // Record History (이전 상태 저장)
            val history = TransactionHistory(
                stockId = stock.id,
                type = type,
                price = 0.0,
                quantity = 0.0,
                amount = amount,
                previousV = stock.vValue,
                newV = stock.vValue,
                previousPool = stock.pool,
                previousQuantity = stock.quantity,
                previousPrincipal = stock.investedPrincipal
            )
            repository.addTransaction(history)

            val updatedStock = stock.copy(pool = newPool, investedPrincipal = newPrincipal)
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
            recordStockHistory(updatedStock)
            updateDailySnapshot()
        }
    }

    fun recalculateV(stock: Stock) {
        val newV = VRCalculator.calculateNewV(stock.vValue, stock.pool, stock.gValue)

        viewModelScope.launch {
            // 1. Record History (이전 상태 저장)
            val history = TransactionHistory(
                stockId = stock.id,
                type = "RECALC_V",
                price = stock.currentPrice,
                quantity = 0.0,
                amount = 0.0,
                previousV = stock.vValue,
                newV = newV,
                previousPool = stock.pool,
                previousQuantity = stock.quantity,
                previousPrincipal = stock.investedPrincipal
            )
            repository.addTransaction(history)

            // 2. Update Stock (Pool은 0으로 초기화)
            val updatedStock = stock.copy(vValue = newV, pool = 0.0)
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
            recordStockHistory(updatedStock)
            updateDailySnapshot()
        }
    }

    fun updateStockGValue(stock: Stock, newG: Double) {
        viewModelScope.launch {
            val updatedStock = stock.copy(gValue = newG)
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
            recordStockHistory(updatedStock)
        }
    }

    fun updateDefaultRecalcAmount(stock: Stock, amount: Double) {
        viewModelScope.launch {
            val updatedStock = stock.copy(defaultRecalcAmount = amount)
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
        }
    }

    // 통합 정보 수정 (V값, 티커, 통화, 현재가 제외)
    // MANUAL_EDIT 기록이 추가되면 이후 모든 거래 삭제가 불가능해짐
    fun updateStockInfo(
        stock: Stock,
        name: String,
        gValue: Double,
        pool: Double,
        quantity: Double,
        investedPrincipal: Double,
        startDate: Long,
        defaultRecalcAmount: Double
    ) {
        viewModelScope.launch {
            // MANUAL_EDIT 거래 기록 추가 (이전 상태 저장하지 않음 - 삭제 불가)
            val editHistory = TransactionHistory(
                stockId = stock.id,
                type = "MANUAL_EDIT",
                price = 0.0,
                quantity = 0.0,
                amount = 0.0,
                previousV = null,
                newV = null,
                previousPool = -1.0,  // 삭제 불가 표시
                previousQuantity = -1.0,  // 삭제 불가 표시
                previousPrincipal = -1.0  // 삭제 불가 표시
            )
            repository.addTransaction(editHistory)

            val updatedStock = stock.copy(
                name = name,
                gValue = gValue,
                pool = pool,
                quantity = quantity,
                investedPrincipal = investedPrincipal,
                startDate = startDate,
                defaultRecalcAmount = defaultRecalcAmount
            )
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
            recordStockHistory(updatedStock)
            updateDailySnapshot()
        }
    }


    fun recalculateVWithDeposit(stock: Stock, amount: Double, isDeposit: Boolean) {
        viewModelScope.launch {
            // 1. 입출금액 확정
            val depositAmount = if (isDeposit) amount else -amount
            
            // 2. V값 재산출
            // 공식: V = 현재V + (기존POOL / G) + 입출금액
            val newV = VRCalculator.calculateNewV(stock.vValue, stock.pool, stock.gValue, depositAmount)

            // 3. Pool 및 원금 반영
            // 사용자의 요청: "재계산이후 현재 pool 에 입출금된 금액을 반영하고, 원금에도 반영"
            // 기존 Pool을 유지하고, 입출금액을 가감합니다.
            val newPool = stock.pool + depositAmount
            val newPrincipal = stock.investedPrincipal + depositAmount

            // 4. 입출금 기록 (이전 상태 저장)
            if (amount > 0) {
                val transactionType = if (isDeposit) "DEPOSIT" else "WITHDRAW"
                val depositHistory = TransactionHistory(
                    stockId = stock.id,
                    type = transactionType,
                    price = 0.0,
                    quantity = 0.0,
                    amount = amount,
                    previousV = stock.vValue,
                    newV = stock.vValue,
                    previousPool = stock.pool,
                    previousQuantity = stock.quantity,
                    previousPrincipal = stock.investedPrincipal
                )
                repository.addTransaction(depositHistory)
            }

            // 5. V값 재산출 기록 (이전 상태 저장 - 입출금 반영 후 상태)
            val poolBeforeRecalc = if (amount > 0) newPool else stock.pool
            val principalBeforeRecalc = if (amount > 0) newPrincipal else stock.investedPrincipal
            val recalcHistory = TransactionHistory(
                stockId = stock.id,
                type = "RECALC_V",
                price = stock.currentPrice,
                quantity = 0.0,
                amount = stock.pool,  // 재산출 시점의 Pool 기록
                previousV = stock.vValue,
                newV = newV,
                previousPool = poolBeforeRecalc,
                previousQuantity = stock.quantity,
                previousPrincipal = principalBeforeRecalc
            )
            repository.addTransaction(recalcHistory)

            // 6. Stock 업데이트
            val updatedStock = stock.copy(
                vValue = newV,
                pool = newPool,
                investedPrincipal = newPrincipal
            )
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
            recordStockHistory(updatedStock)
            updateDailySnapshot()
        }
    }

    // 전일 자산 상태 (비교용)
    val yesterdayAssetStatus: StateFlow<AssetStatus?> = dailyHistory.combine(_allStocks) { history, _ ->
        if (history.isEmpty()) null
        else {
            val sorted = history.sortedBy { it.date }
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            // 오늘이 아닌 마지막 기록(즉, 어제 최종 기록)을 찾음
            val lastEntry = sorted.lastOrNull { it.date != today }
            
            if (lastEntry != null) {
                val roi = if (lastEntry.totalPrincipal > 0) 
                    ((lastEntry.totalCurrentValue - lastEntry.totalPrincipal) / lastEntry.totalPrincipal) * 100 
                    else 0.0
                AssetStatus(lastEntry.totalPrincipal, lastEntry.totalCurrentValue, roi)
            } else {
                null
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun executeTransaction(stock: Stock, type: String, price: Double, quantity: Double) {
        viewModelScope.launch {
            val amount = price * quantity
            var newPool = stock.pool
            var newQty = stock.quantity
            
            // Trading affects Pool and Qty, NOT Principal
            if (type == "BUY") {
                newPool -= amount
                newQty += quantity
            } else if (type == "SELL") {
                newPool += amount
                newQty -= quantity
            }

            // 1. Record History (이전 상태 저장)
            val history = TransactionHistory(
                stockId = stock.id,
                type = type,
                price = price,
                quantity = quantity,
                amount = amount,
                previousV = stock.vValue,
                newV = stock.vValue,
                previousPool = stock.pool,
                previousQuantity = stock.quantity,
                previousPrincipal = stock.investedPrincipal
            )
            repository.addTransaction(history)

            // 2. Update Stock
            val updatedStock = stock.copy(pool = newPool, quantity = newQty)
            repository.updateStock(updatedStock)
            _currentStock.value = updatedStock
            recordStockHistory(updatedStock)
            updateDailySnapshot()
        }
    }

    // 가장 최근 거래 삭제 및 원복
    fun deleteLatestTransaction(stock: Stock, transaction: TransactionHistory): Boolean {
        // 이전 상태 정보가 없으면 삭제 불가
        if (!transaction.canDelete()) {
            return false
        }

        viewModelScope.launch {
            // 1. 거래 기록 삭제
            repository.deleteTransaction(transaction.id)

            // 2. 이전 상태로 원복
            val restoredStock = stock.copy(
                vValue = transaction.previousV ?: stock.vValue,
                pool = transaction.previousPool,
                quantity = transaction.previousQuantity,
                investedPrincipal = transaction.previousPrincipal
            )
            repository.updateStock(restoredStock)
            _currentStock.value = restoredStock

            // 3. 스냅샷 업데이트
            recordStockHistory(restoredStock)
            updateDailySnapshot()
        }
        return true
    }
}