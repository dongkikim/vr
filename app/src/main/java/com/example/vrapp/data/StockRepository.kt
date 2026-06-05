package com.example.vrapp.data

import com.example.vrapp.model.*
import kotlinx.coroutines.flow.Flow

class StockRepository(
    private val stockDao: StockDao,
    private val ibDao: IbDao
) {
    
    // --- VR/General Stocks ---
    val allStocks: Flow<List<Stock>> = stockDao.getAllStocks()
    val dailyHistory: Flow<List<DailyAssetHistory>> = stockDao.getDailyHistory()

    suspend fun getStock(id: Long): Stock? = stockDao.getStockById(id)
    suspend fun addStock(stock: Stock) = stockDao.insertStock(stock)
    suspend fun updateStock(stock: Stock) = stockDao.updateStock(stock)
    suspend fun deleteStock(id: Long) = stockDao.deleteStock(id)

    fun getHistory(stockId: Long): Flow<List<TransactionHistory>> = stockDao.getHistoryForStock(stockId)
    fun getRecentHistory(stockId: Long, limit: Int): Flow<List<TransactionHistory>> = stockDao.getRecentTransactions(stockId, limit)
    suspend fun addTransaction(transaction: TransactionHistory) = stockDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transactionId: Long) = stockDao.deleteTransaction(transactionId)

    suspend fun saveDailyHistory(history: DailyAssetHistory) = stockDao.insertDailyHistory(history)
    fun getStockHistory(stockId: Long): Flow<List<StockHistory>> = stockDao.getStockHistory(stockId)
    suspend fun saveStockHistory(history: StockHistory) {
        stockDao.deleteStockHistoryForSameDay(history.stockId, history.timestamp)
        stockDao.insertStockHistory(history)
    }

    // --- Infinite Buying (IB) ---
    val allIbStocks: Flow<List<IbStock>> = ibDao.getAllIbStocks()
    val allIbAccounts: Flow<List<IbAccount>> = ibDao.getAllAccounts()

    suspend fun getIbAccount(currency: String): IbAccount? = ibDao.getAccountByCurrency(currency)
    suspend fun updateIbAccount(account: IbAccount) = ibDao.updateAccount(account)
    suspend fun insertIbAccount(account: IbAccount) = ibDao.insertAccount(account)

    suspend fun getIbStock(id: Long): IbStock? = ibDao.getIbStockById(id)
    suspend fun addIbStock(stock: IbStock): Long = ibDao.insertIbStock(stock)
    suspend fun updateIbStock(stock: IbStock) = ibDao.updateIbStock(stock)
    suspend fun deleteIbStock(id: Long) = ibDao.deleteIbStock(id)

    fun getIbHistory(stockId: Long): Flow<List<IbTransaction>> = ibDao.getHistoryForIbStock(stockId)
    suspend fun addIbTransaction(transaction: IbTransaction) = ibDao.insertIbTransaction(transaction)
    suspend fun deleteIbTransaction(id: Long) = ibDao.deleteIbTransaction(id)
    suspend fun getLatestIbTransaction(stockId: Long): IbTransaction? = ibDao.getLatestTransaction(stockId)

    // Wallet History
    fun getWalletHistory(currency: String): Flow<List<IbWalletHistory>> = ibDao.getWalletHistory(currency)
    suspend fun addWalletHistory(history: IbWalletHistory) = ibDao.insertWalletHistory(history)

    // Backup & Restore
    suspend fun getAllStocksSnapshot() = stockDao.getAllStocksSnapshot()
    suspend fun getAllTransactionsSnapshot() = stockDao.getAllTransactionsSnapshot()
    suspend fun getAllDailyHistorySnapshot() = stockDao.getAllDailyHistorySnapshot()
    suspend fun getAllStockHistorySnapshot() = stockDao.getAllStockHistorySnapshot()
    
    suspend fun getAllIbAccountsSnapshot() = ibDao.getAllAccountsSnapshot()
    suspend fun getAllIbStocksSnapshot() = ibDao.getAllIbStocksSnapshot()
    suspend fun getAllIbTransactionsSnapshot() = ibDao.getAllIbTransactionsSnapshot()
    suspend fun getAllIbWalletHistorySnapshot() = ibDao.getAllIbWalletHistorySnapshot()

    suspend fun clearAllData() {
        stockDao.deleteAllStocks()
        stockDao.deleteAllTransactions()
        stockDao.deleteAllDailyHistory()
        stockDao.deleteAllStockHistory()
        ibDao.deleteAllAccounts()
        ibDao.deleteAllIbStocks()
        ibDao.deleteAllIbTransactions()
        ibDao.deleteAllIbWalletHistory()
    }

    suspend fun restoreData(
        stocks: List<Stock>,
        transactions: List<TransactionHistory>,
        daily: List<DailyAssetHistory>,
        history: List<StockHistory>,
        ibAccounts: List<IbAccount> = emptyList(),
        ibStocks: List<IbStock> = emptyList(),
        ibTransactions: List<IbTransaction> = emptyList(),
        ibWalletHistory: List<IbWalletHistory> = emptyList()
    ) {
        if (stocks.isNotEmpty()) stockDao.insertStocks(stocks)
        if (transactions.isNotEmpty()) stockDao.insertTransactions(transactions)
        if (daily.isNotEmpty()) stockDao.insertDailyHistories(daily)
        if (history.isNotEmpty()) stockDao.insertStockHistories(history)
        
        if (ibAccounts.isNotEmpty()) {
            ibAccounts.forEach { ibDao.insertAccount(it) }
        }
        if (ibStocks.isNotEmpty()) {
            ibStocks.forEach { ibDao.insertIbStock(it) }
        }
        if (ibTransactions.isNotEmpty()) {
            ibTransactions.forEach { ibDao.insertIbTransaction(it) }
        }
        if (ibWalletHistory.isNotEmpty()) {
            ibWalletHistory.forEach { ibDao.insertWalletHistory(it) }
        }
    }
}
