package com.example.vrapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.OnConflictStrategy
import com.example.vrapp.model.IbAccount
import com.example.vrapp.model.IbStock
import com.example.vrapp.model.IbTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IbDao {
    // --- IbAccount (통합 지갑) ---
    @Query("SELECT * FROM ib_accounts")
    fun getAllAccounts(): Flow<List<IbAccount>>

    @Query("SELECT * FROM ib_accounts WHERE currency = :currency LIMIT 1")
    suspend fun getAccountByCurrency(currency: String): IbAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: IbAccount)

    @Update
    suspend fun updateAccount(account: IbAccount)

    // --- IbStock (무매 종목) ---
    @Query("SELECT * FROM ib_stocks")
    fun getAllIbStocks(): Flow<List<IbStock>>

    @Query("SELECT * FROM ib_stocks WHERE id = :id")
    suspend fun getIbStockById(id: Long): IbStock?

    @Insert
    suspend fun insertIbStock(stock: IbStock): Long

    @Update
    suspend fun updateIbStock(stock: IbStock)

    @Query("DELETE FROM ib_stocks WHERE id = :id")
    suspend fun deleteIbStock(id: Long)

    // --- IbTransaction (무매 거래 내역) ---
    @Query("SELECT * FROM ib_transactions WHERE ibStockId = :stockId ORDER BY date DESC, id DESC")
    fun getHistoryForIbStock(stockId: Long): Flow<List<IbTransaction>>

    @Query("SELECT * FROM ib_transactions WHERE ibStockId = :stockId AND cycleNumber = :cycleNumber ORDER BY date ASC")
    suspend fun getTransactionsForCycle(stockId: Long, cycleNumber: Int): List<IbTransaction>

    @Insert
    suspend fun insertIbTransaction(transaction: IbTransaction)

    @Query("DELETE FROM ib_transactions WHERE id = :id")
    suspend fun deleteIbTransaction(id: Long)

    @Query("SELECT * FROM ib_transactions WHERE ibStockId = :stockId ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLatestTransaction(stockId: Long): IbTransaction?

    // --- IbWalletHistory (지갑 변동 내역) ---
    @Query("SELECT * FROM ib_wallet_history WHERE currency = :currency ORDER BY timestamp DESC, id DESC")
    fun getWalletHistory(currency: String): Flow<List<com.example.vrapp.model.IbWalletHistory>>

    @Insert
    suspend fun insertWalletHistory(history: com.example.vrapp.model.IbWalletHistory)

    // --- Snapshot for Backup ---
    @Query("SELECT * FROM ib_accounts")
    suspend fun getAllAccountsSnapshot(): List<IbAccount>

    @Query("SELECT * FROM ib_stocks")
    suspend fun getAllIbStocksSnapshot(): List<IbStock>

    @Query("SELECT * FROM ib_transactions")
    suspend fun getAllIbTransactionsSnapshot(): List<IbTransaction>

    @Query("SELECT * FROM ib_wallet_history")
    suspend fun getAllIbWalletHistorySnapshot(): List<com.example.vrapp.model.IbWalletHistory>

    @Query("DELETE FROM ib_accounts")
    suspend fun deleteAllAccounts()

    @Query("DELETE FROM ib_stocks")
    suspend fun deleteAllIbStocks()

    @Query("DELETE FROM ib_transactions")
    suspend fun deleteAllIbTransactions()

    @Query("DELETE FROM ib_wallet_history")
    suspend fun deleteAllIbWalletHistory()
}
