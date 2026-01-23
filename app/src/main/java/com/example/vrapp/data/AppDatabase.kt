package com.example.vrapp.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.OnConflictStrategy
import androidx.room.AutoMigration
import com.example.vrapp.model.Stock
import com.example.vrapp.model.TransactionHistory
import com.example.vrapp.model.DailyAssetHistory
import com.example.vrapp.model.StockHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<Stock>>

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getStockById(id: Long): Stock?

    @Insert
    suspend fun insertStock(stock: Stock)

    @Insert
    suspend fun insertStocks(stocks: List<Stock>)

    @Update
    suspend fun updateStock(stock: Stock)

    @Query("DELETE FROM stocks WHERE id = :id")
    suspend fun deleteStock(id: Long)
    
    // History
    @Insert
    suspend fun insertTransaction(transaction: TransactionHistory)
    
    @Insert
    suspend fun insertTransactions(transactions: List<TransactionHistory>)
    
    @Query("SELECT * FROM transactions WHERE stockId = :stockId ORDER BY date DESC")
    fun getHistoryForStock(stockId: Long): Flow<List<TransactionHistory>>

    // Daily Asset History
    @Query("SELECT * FROM daily_asset_history ORDER BY date ASC")
    fun getDailyHistory(): Flow<List<DailyAssetHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyHistory(history: DailyAssetHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyHistories(histories: List<DailyAssetHistory>)

    // Stock History (VR Snapshot)
    @Insert
    suspend fun insertStockHistory(history: StockHistory)

    @Insert
    suspend fun insertStockHistories(histories: List<StockHistory>)

    @Query("SELECT * FROM stock_history WHERE stockId = :stockId ORDER BY timestamp ASC")
    fun getStockHistory(stockId: Long): Flow<List<StockHistory>>

    // --- Backup & Restore ---
    @Query("SELECT * FROM stocks")
    suspend fun getAllStocksSnapshot(): List<Stock>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSnapshot(): List<TransactionHistory>

    @Query("SELECT * FROM daily_asset_history")
    suspend fun getAllDailyHistorySnapshot(): List<DailyAssetHistory>
    
    @Query("SELECT * FROM stock_history")
    suspend fun getAllStockHistorySnapshot(): List<StockHistory>

    @Query("DELETE FROM stocks")
    suspend fun deleteAllStocks()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM daily_asset_history")
    suspend fun deleteAllDailyHistory()

    @Query("DELETE FROM stock_history")
    suspend fun deleteAllStockHistory()
}

@Database(
    entities = [Stock::class, TransactionHistory::class, DailyAssetHistory::class, StockHistory::class],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vr_investment_db"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}