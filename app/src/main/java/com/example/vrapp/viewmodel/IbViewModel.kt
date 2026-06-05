package com.example.vrapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vrapp.data.AppDatabase
import com.example.vrapp.data.StockPriceService
import com.example.vrapp.data.StockRepository
import com.example.vrapp.logic.IBCalculator
import com.example.vrapp.model.IbAccount
import com.example.vrapp.model.IbStock
import com.example.vrapp.model.IbTransaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IbViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StockRepository
    
    // --- State Flows ---
    val allIbStocks: StateFlow<List<IbStock>>
    val allIbAccounts: StateFlow<List<IbAccount>>
    
    private val _currentIbStock = MutableStateFlow<IbStock?>(null)
    val currentIbStock = _currentIbStock.asStateFlow()

    private val _ibTransactionHistory = MutableStateFlow<List<IbTransaction>>(emptyList())
    val ibTransactionHistory = _ibTransactionHistory.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _sma5DayMap = MutableStateFlow<Map<Long, Double>>(emptyMap())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StockRepository(database.stockDao(), database.ibDao())

        allIbStocks = repository.allIbStocks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allIbAccounts = repository.allIbAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        // 초기 지갑 생성 (USD, KRW 기본)
        viewModelScope.launch {
            if (repository.getIbAccount("USD") == null) {
                repository.insertIbAccount(IbAccount(currency = "USD", balance = 0.0, totalInvested = 0.0))
            }
            if (repository.getIbAccount("KRW") == null) {
                repository.insertIbAccount(IbAccount(currency = "KRW", balance = 0.0, totalInvested = 0.0))
            }
            
            // [main][2026-06-05] 기존 종목들의 왜곡된 평단가 자동 보정 실행
            recalculateAllAveragePrices()
        }
    }

    /**
     * 기존 거래 내역을 기반으로 모든 종목의 평단가를 다시 계산하여 보정합니다.
     */
    private suspend fun recalculateAllAveragePrices() {
        val stocks = repository.allIbStocks.first()
        stocks.forEach { stock ->
            // 현재 평단가가 0이거나 데이터 왜곡 가능성이 있는 경우 재계산
            val history = repository.getTransactionsForCycle(stock.id, stock.cycleCount)
            if (history.isNotEmpty()) {
                var calculatedAvgPrice = 0.0
                var currentQty = 0.0
                
                history.forEach { trans ->
                    when {
                        trans.type.contains("BUY") -> {
                            val totalCost = (currentQty * calculatedAvgPrice) + (trans.quantity * trans.price)
                            currentQty += trans.quantity
                            calculatedAvgPrice = if (currentQty > 0) totalCost / currentQty else 0.0
                        }
                        trans.type.contains("SELL") -> {
                            currentQty -= trans.quantity
                            // 매도시 평단 유지
                            if (currentQty <= 0) {
                                calculatedAvgPrice = 0.0
                                currentQty = 0.0
                            }
                        }
                        trans.type == "SPLIT" -> {
                            // 분할은 trans.quantity가 최종 수량이라고 가정 (기존 로직 참고)
                            val ratio = if (currentQty > 0) trans.quantity / currentQty else 1.0
                            currentQty = trans.quantity
                            calculatedAvgPrice /= ratio
                        }
                    }
                }
                
                // 계산된 평단가로 업데이트 (수량은 건드리지 않음)
                if (calculatedAvgPrice != stock.averagePrice) {
                    repository.updateIbStock(stock.copy(averagePrice = calculatedAvgPrice))
                }
            }
        }
    }

    fun getWalletHistory(currency: String): Flow<List<com.example.vrapp.model.IbWalletHistory>> {
        return repository.getWalletHistory(currency)
    }

    // --- Wallet Actions ---
    fun depositToWallet(currency: String, amount: Double) {
        viewModelScope.launch {
            val account = repository.getIbAccount(currency) ?: IbAccount(currency = currency)
            val updated = account.copy(
                balance = account.balance + amount,
                totalInvested = account.totalInvested + amount
            )
            repository.insertIbAccount(updated)

            // 내역 저장
            val history = com.example.vrapp.model.IbWalletHistory(
                currency = currency,
                type = "DEPOSIT",
                amount = amount,
                balanceAfter = updated.balance,
                description = "수동 충전"
            )
            repository.addWalletHistory(history)
        }
    }

    fun withdrawFromWallet(currency: String, amount: Double) {
        viewModelScope.launch {
            val account = repository.getIbAccount(currency) ?: return@launch
            val updated = account.copy(
                balance = maxOf(0.0, account.balance - amount),
                totalInvested = maxOf(0.0, account.totalInvested - amount)
            )
            repository.insertIbAccount(updated)

            // 내역 저장
            val history = com.example.vrapp.model.IbWalletHistory(
                currency = currency,
                type = "WITHDRAW",
                amount = -amount,
                balanceAfter = updated.balance,
                description = "수동 인출"
            )
            repository.addWalletHistory(history)
        }
    }

    private var loadHistoryJob: kotlinx.coroutines.Job? = null

    // --- Stock Actions ---
    fun loadIbStock(id: Long) {
        loadHistoryJob?.cancel()
        loadHistoryJob = viewModelScope.launch {
            val stock = repository.getIbStock(id)
            _currentIbStock.value = stock
            
            repository.getIbHistory(id).collect {
                _ibTransactionHistory.value = it
            }
        }
    }

    fun addIbStock(name: String, ticker: String, principal: Double, divisions: Int, volatility: Double, currency: String, initialPrice: Double, initialQty: Double) {
        viewModelScope.launch {
            val account = repository.getIbAccount(currency) ?: return@launch
            if (account.balance < principal) return@launch // 잔액 부족 (UI에서 방지됨)

            // 1. 지갑 차감
            val newBalance = account.balance - principal
            repository.updateIbAccount(account.copy(balance = newBalance))

            // 1-1. 지갑 내역 저장
            val wHistory = com.example.vrapp.model.IbWalletHistory(
                currency = currency,
                type = "ALLOCATE",
                amount = -principal,
                balanceAfter = newBalance,
                description = "$ticker 신규 무매 시작 할당"
            )
            repository.addWalletHistory(wHistory)

            // 2. 종목 생성 (초기 T값 자동 추정 적용)
            val initialT = IBCalculator.estimateInitialT(principal, divisions, initialPrice * initialQty)
            val newStock = IbStock(
                name = name,
                ticker = ticker,
                currency = currency,
                principal = principal,
                pool = principal - (initialPrice * initialQty), // 초기 매수금 차감
                quantity = initialQty,
                divisions = divisions,
                volatility = volatility,
                currentT = initialT,
                currentPrice = initialPrice,
                // [main][2026-06-05] 초기 평단가 설정
                averagePrice = if (initialQty > 0) initialPrice else 0.0
            )
            repository.addIbStock(newStock)
        }
    }

    fun deleteIbStock(stock: IbStock) {
        viewModelScope.launch {
            val account = repository.getIbAccount(stock.currency)
            if (account != null) {
                // 남은 잔금(Pool)을 지갑으로 전액 반환
                val newBalance = account.balance + stock.pool
                repository.updateIbAccount(account.copy(balance = newBalance))

                // 지갑 내역 저장
                val wHistory = com.example.vrapp.model.IbWalletHistory(
                    currency = stock.currency,
                    type = "RETURN",
                    amount = stock.pool,
                    balanceAfter = newBalance,
                    description = "${stock.ticker} 삭제로 인한 잔금 반환"
                )
                repository.addWalletHistory(wHistory)
            }
            repository.deleteIbStock(stock.id)
        }
    }

    // --- Transaction Execution ---
    suspend fun executeIbTransaction(stock: IbStock, type: String, price: Double, quantity: Double, date: Long = System.currentTimeMillis()) {
        val amount = price * quantity
        val nextT = IBCalculator.calculateNextT(stock.currentT, stock.divisions, stock.isReverseMode, type)
        
        // 리버스 모드 진입 판별
        val willBeReverse = nextT > (stock.divisions - 1)

        // 1. 기록 저장 (롤백용)
        val transaction = IbTransaction(
            ibStockId = stock.id,
            date = date,
            type = type,
            price = price,
            quantity = quantity,
            amount = amount,
            previousT = stock.currentT,
            previousPool = stock.pool,
            previousIsReverseMode = stock.isReverseMode,
            previousCycleCount = stock.cycleCount,
            // [main][2026-06-05] 롤백을 위한 이전 평단가 기록
            previousAveragePrice = stock.averagePrice,
            cycleNumber = stock.cycleCount
        )
        repository.addIbTransaction(transaction)

        // 2. 종목 상태 갱신
        var newPool = stock.pool
        var newQty = stock.quantity
        var newAvgPrice = stock.averagePrice
        
        if (type.startsWith("BUY")) {
            // 매수: 가중 평균으로 평단가 갱신
            val totalCost = (newQty * newAvgPrice) + (quantity * price)
            newQty += quantity
            newAvgPrice = if (newQty > 0) totalCost / newQty else 0.0
            newPool -= amount
        } else if (type.startsWith("SELL") || type == "REVERSE_SELL") {
            // 매도: 수량만 감소, 평단가는 유지
            newQty -= quantity
            newPool += amount
            if (newQty <= 0) {
                newAvgPrice = 0.0
                newQty = 0.0
            }
        } else if (type == "SPLIT") {
            // 액면분할: 수량 및 평단가만 조정
            val ratio = if (newQty > 0) quantity / newQty else 1.0
            newQty = quantity 
            newAvgPrice /= ratio
        }

        val updatedStock = stock.copy(
            pool = newPool,
            quantity = newQty,
            currentT = nextT,
            isReverseMode = willBeReverse,
            averagePrice = newAvgPrice
        )
        repository.updateIbStock(updatedStock)
        _currentIbStock.value = updatedStock
        
        // 사이클 종료 체크 (수량 0 도달 시)
        if (newQty <= 0 && !type.contains("BUY") && type != "SPLIT") {
            // UI에서 사이클 종료 다이얼로그 노출 예정
        }
    }

    fun rollbackLatestTransaction(stock: IbStock, transaction: IbTransaction) {
        viewModelScope.launch {
            // 1. 거래 내역 삭제
            repository.deleteIbTransaction(transaction.id)

            // 2. 지갑 복구 (만약 사이클 종료로 지갑에 반환된 건이었다면 - 추가 로직 필요)
            // 일단 기본적인 종목 상태 복구
            val restoredStock = stock.copy(
                currentT = transaction.previousT,
                pool = transaction.previousPool,
                isReverseMode = transaction.previousIsReverseMode,
                cycleCount = if (transaction.previousCycleCount > 0) transaction.previousCycleCount else stock.cycleCount,
                quantity = if (transaction.type == "SPLIT") stock.quantity else (if (transaction.type.contains("BUY")) stock.quantity - transaction.quantity else stock.quantity + transaction.quantity),
                // [main][2026-06-05] 이전 평단가로 복구
                averagePrice = transaction.previousAveragePrice
            )
            
            // 만약 사이클이 종료되어 지갑에 합산되었던 건이라면? (구조상 사이클 종료는 별도의 입금 transaction으로 기록하는것이 안전)
            // 이 버전에서는 단순 거래 롤백 위주로 구현
            
            repository.updateIbStock(restoredStock)
            _currentIbStock.value = restoredStock
        }
    }

    fun completeCycle(stock: IbStock, actualFinalAmount: Double, shouldDelete: Boolean = false, nextPrincipal: Double = 0.0) {
        viewModelScope.launch {
            val account = repository.getIbAccount(stock.currency) ?: return@launch
            
            // 1. 지갑으로 최종 금액 반환 (수수료 등 오차 반영된 실제 금액)
            val newBalance = account.balance + actualFinalAmount
            repository.updateIbAccount(account.copy(balance = newBalance))

            // 1-1. 지갑 내역 저장
            val profit = actualFinalAmount - stock.principal
            val history = com.example.vrapp.model.IbWalletHistory(
                currency = stock.currency,
                type = "RETURN",
                amount = actualFinalAmount,
                balanceAfter = newBalance,
                description = "${stock.ticker} ${stock.cycleCount}회차 종료 반환 (수익: ${String.format("%+.2f", profit)})"
            )
            repository.addWalletHistory(history)
            
            if (shouldDelete) {
                // 2. 종목 삭제
                repository.deleteIbStock(stock.id)
                _currentIbStock.value = null
            } else if (nextPrincipal > 0 && newBalance >= nextPrincipal) {
                // 2. 즉시 다음 사이클 시작
                val finalBalance = newBalance - nextPrincipal
                repository.updateIbAccount(account.copy(balance = finalBalance))
                
                // 지갑 내역 저장 (할당)
                repository.addWalletHistory(com.example.vrapp.model.IbWalletHistory(
                    currency = stock.currency,
                    type = "ALLOCATE",
                    amount = -nextPrincipal,
                    balanceAfter = finalBalance,
                    description = "${stock.ticker} ${stock.cycleCount + 1}회차 즉시 시작 할당"
                ))

                val restartedStock = stock.copy(
                    principal = nextPrincipal,
                    pool = nextPrincipal,
                    quantity = 0.0,
                    currentT = 0.0,
                    isReverseMode = false,
                    cycleCount = stock.cycleCount + 1,
                    totalRealizedProfit = stock.totalRealizedProfit + profit
                )
                repository.updateIbStock(restartedStock)
                _currentIbStock.value = restartedStock
            } else {
                // 2. 종목 리셋 및 사이클 카운트 증가 (대기 상태)
                val resetStock = stock.copy(
                    pool = 0.0,
                    principal = 0.0, // 다음 할당을 위해 0으로
                    quantity = 0.0,
                    currentT = 0.0,
                    isReverseMode = false,
                    cycleCount = stock.cycleCount + 1,
                    totalRealizedProfit = stock.totalRealizedProfit + profit
                )
                repository.updateIbStock(resetStock)
                _currentIbStock.value = resetStock
            }
        }
    }

    fun restartIbCycle(stock: IbStock, newPrincipal: Double) {
        viewModelScope.launch {
            val account = repository.getIbAccount(stock.currency) ?: return@launch
            if (account.balance < newPrincipal) return@launch

            // 1. 지갑 차감
            val newBalance = account.balance - newPrincipal
            repository.updateIbAccount(account.copy(balance = newBalance))

            // 1-1. 지갑 내역 저장
            val history = com.example.vrapp.model.IbWalletHistory(
                currency = stock.currency,
                type = "ALLOCATE",
                amount = -newPrincipal,
                balanceAfter = newBalance,
                description = "${stock.ticker} ${stock.cycleCount}회차 새 시작 할당"
            )
            repository.addWalletHistory(history)

            // 2. 종목 리셋
            val restartedStock = stock.copy(
                principal = newPrincipal,
                pool = newPrincipal,
                quantity = 0.0,
                currentT = 0.0,
                isReverseMode = false
            )
            repository.updateIbStock(restartedStock)
            _currentIbStock.value = restartedStock
        }
    }

    fun refreshPrices() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                allIbStocks.value.forEach { stock ->
                    val price = StockPriceService.getStockPrice(stock.ticker, stock.currency)
                    if (price > 0) {
                        repository.updateIbStock(stock.copy(currentPrice = price))
                    }
                    
                    // 리버스 모드인 경우 5일 이평선도 가져옴
                    if (stock.isReverseMode) {
                        val sma = StockPriceService.get5DayMovingAverage(stock.ticker)
                        if (sma > 0) {
                            val currentMap = _sma5DayMap.value.toMutableMap()
                            currentMap[stock.id] = sma
                            _sma5DayMap.value = currentMap
                        }
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // [main][2026-06-05] 무한매수 종목명 수정 기능 추가
    fun updateIbStockName(stock: IbStock, newName: String) {
        viewModelScope.launch {
            val updatedStock = stock.copy(name = newName)
            repository.updateIbStock(updatedStock)
            _currentIbStock.value = updatedStock
        }
    }
    
    // 리버스 모드용 5일 이평선 맵 제공
    val sma5DayMap = _sma5DayMap.asStateFlow()
}
