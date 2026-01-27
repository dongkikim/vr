package com.example.vrapp.data

import com.example.vrapp.model.Stock
import com.example.vrapp.model.TransactionHistory
import kotlinx.coroutines.flow.Flow

import com.example.vrapp.model.DailyAssetHistory

import com.example.vrapp.model.StockHistory

class StockRepository(private val stockDao: StockDao) {
    
    val allStocks: Flow<List<Stock>> = stockDao.getAllStocks()
    val dailyHistory: Flow<List<DailyAssetHistory>> = stockDao.getDailyHistory()

    suspend fun getStock(id: Long): Stock? {
        return stockDao.getStockById(id)
    }

    suspend fun addStock(stock: Stock) {
        stockDao.insertStock(stock)
    }

    suspend fun updateStock(stock: Stock) {
        stockDao.updateStock(stock)
    }
    
    suspend fun deleteStock(id: Long) {
        stockDao.deleteStock(id)
    }

    fun getHistory(stockId: Long): Flow<List<TransactionHistory>> {
        return stockDao.getHistoryForStock(stockId)
    }

    suspend fun addTransaction(transaction: TransactionHistory) {
        stockDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        stockDao.deleteTransaction(transactionId)
    }

    suspend fun saveDailyHistory(history: DailyAssetHistory) {
        stockDao.insertDailyHistory(history)
    }

    fun getStockHistory(stockId: Long): Flow<List<StockHistory>> {
        return stockDao.getStockHistory(stockId)
    }

    suspend fun saveStockHistory(history: StockHistory) {
        // 같은 날짜의 기존 스냅샷 삭제 후 새로 저장
        stockDao.deleteStockHistoryForSameDay(history.stockId, history.timestamp)
        stockDao.insertStockHistory(history)
    }

    // Backup & Restore
    suspend fun getAllStocksSnapshot() = stockDao.getAllStocksSnapshot()
    suspend fun getAllTransactionsSnapshot() = stockDao.getAllTransactionsSnapshot()
    suspend fun getAllDailyHistorySnapshot() = stockDao.getAllDailyHistorySnapshot()
    suspend fun getAllStockHistorySnapshot() = stockDao.getAllStockHistorySnapshot()

    suspend fun clearAllData() {
        stockDao.deleteAllStocks()
        stockDao.deleteAllTransactions()
        stockDao.deleteAllDailyHistory()
        stockDao.deleteAllStockHistory()
    }

    suspend fun restoreData(
        stocks: List<Stock>,
        transactions: List<TransactionHistory>,
        daily: List<DailyAssetHistory>,
        history: List<StockHistory>
    ) {
        if (stocks.isNotEmpty()) stockDao.insertStocks(stocks)
        if (transactions.isNotEmpty()) stockDao.insertTransactions(transactions)
        if (daily.isNotEmpty()) stockDao.insertDailyHistories(daily)
        if (history.isNotEmpty()) stockDao.insertStockHistories(history)
    }
}
