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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    
    @Query("SELECT * FROM transactions WHERE stockId = :stockId ORDER BY date DESC, id DESC")
    fun getHistoryForStock(stockId: Long): Flow<List<TransactionHistory>>

    @Query("SELECT * FROM transactions WHERE stockId = :stockId ORDER BY date DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(stockId: Long, limit: Int): Flow<List<TransactionHistory>>

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)

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

    @Query("DELETE FROM stock_history WHERE stockId = :stockId AND date(timestamp / 1000, 'unixepoch', 'localtime') = date(:timestamp / 1000, 'unixepoch', 'localtime')")
    suspend fun deleteStockHistoryForSameDay(stockId: Long, timestamp: Long)

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

// 동일 날짜의 중복 VR 스냅샷 제거 (마지막 것만 유지)
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            DELETE FROM stock_history WHERE id NOT IN (
                SELECT MAX(id)
                FROM stock_history
                GROUP BY stockId, date(timestamp / 1000, 'unixepoch', 'localtime')
            )
        """)
    }
}

// vrPool 초기화 마이그레이션 (기존 데이터를 현재 pool 값으로 채움)
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 컬럼 추가
        database.execSQL("ALTER TABLE stocks ADD COLUMN vrPool REAL NOT NULL DEFAULT -1.0")
        database.execSQL("ALTER TABLE transactions ADD COLUMN previousVrPool REAL NOT NULL DEFAULT -1.0")
        
        // 2. 기존 데이터 업데이트 (vrPool이 -1.0인 경우 현재 pool로 업데이트)
        database.execSQL("UPDATE stocks SET vrPool = pool WHERE vrPool = -1.0")
    }
}

// netTradeAmount 추가 마이그레이션
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE stocks ADD COLUMN netTradeAmount REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE transactions ADD COLUMN previousNetTradeAmount REAL NOT NULL DEFAULT 0.0")
    }
}

@Database(
    entities = [Stock::class, TransactionHistory::class, DailyAssetHistory::class, StockHistory::class],
    version = 12,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 7, to = 8), // TransactionHistory에 previousPool, previousQuantity, previousPrincipal 추가
        AutoMigration(from = 8, to = 9), // Change quantity to Double
        AutoMigration(from = 9, to = 10) // Stock에 isVr 추가
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
                .addMigrations(MIGRATION_6_7, MIGRATION_10_11, MIGRATION_11_12)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}