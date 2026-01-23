package com.example.vrapp.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile StockDao _stockDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `stocks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `ticker` TEXT NOT NULL, `vValue` REAL NOT NULL, `gValue` REAL NOT NULL, `pool` REAL NOT NULL, `quantity` INTEGER NOT NULL, `investedPrincipal` REAL NOT NULL, `currentPrice` REAL NOT NULL, `currency` TEXT NOT NULL, `startDate` INTEGER NOT NULL DEFAULT 0, `defaultRecalcAmount` REAL NOT NULL DEFAULT 0.0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stockId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `type` TEXT NOT NULL, `price` REAL NOT NULL, `quantity` INTEGER NOT NULL, `amount` REAL NOT NULL, `previousV` REAL, `newV` REAL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_asset_history` (`date` TEXT NOT NULL, `totalPrincipal` REAL NOT NULL, `totalCurrentValue` REAL NOT NULL, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `stock_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stockId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `vValue` REAL NOT NULL, `gValue` REAL NOT NULL, `currentPrice` REAL NOT NULL, `quantity` INTEGER NOT NULL, `pool` REAL NOT NULL, `investedPrincipal` REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2ddecd03d03e7da90c4fefced479bb51')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `stocks`");
        db.execSQL("DROP TABLE IF EXISTS `transactions`");
        db.execSQL("DROP TABLE IF EXISTS `daily_asset_history`");
        db.execSQL("DROP TABLE IF EXISTS `stock_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsStocks = new HashMap<String, TableInfo.Column>(12);
        _columnsStocks.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("ticker", new TableInfo.Column("ticker", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("vValue", new TableInfo.Column("vValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("gValue", new TableInfo.Column("gValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("pool", new TableInfo.Column("pool", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("investedPrincipal", new TableInfo.Column("investedPrincipal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("currentPrice", new TableInfo.Column("currentPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("currency", new TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("startDate", new TableInfo.Column("startDate", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        _columnsStocks.put("defaultRecalcAmount", new TableInfo.Column("defaultRecalcAmount", "REAL", true, 0, "0.0", TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStocks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStocks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStocks = new TableInfo("stocks", _columnsStocks, _foreignKeysStocks, _indicesStocks);
        final TableInfo _existingStocks = TableInfo.read(db, "stocks");
        if (!_infoStocks.equals(_existingStocks)) {
          return new RoomOpenHelper.ValidationResult(false, "stocks(com.example.vrapp.model.Stock).\n"
                  + " Expected:\n" + _infoStocks + "\n"
                  + " Found:\n" + _existingStocks);
        }
        final HashMap<String, TableInfo.Column> _columnsTransactions = new HashMap<String, TableInfo.Column>(9);
        _columnsTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("stockId", new TableInfo.Column("stockId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("price", new TableInfo.Column("price", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("previousV", new TableInfo.Column("previousV", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("newV", new TableInfo.Column("newV", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTransactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTransactions = new TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions);
        final TableInfo _existingTransactions = TableInfo.read(db, "transactions");
        if (!_infoTransactions.equals(_existingTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "transactions(com.example.vrapp.model.TransactionHistory).\n"
                  + " Expected:\n" + _infoTransactions + "\n"
                  + " Found:\n" + _existingTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsDailyAssetHistory = new HashMap<String, TableInfo.Column>(3);
        _columnsDailyAssetHistory.put("date", new TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyAssetHistory.put("totalPrincipal", new TableInfo.Column("totalPrincipal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyAssetHistory.put("totalCurrentValue", new TableInfo.Column("totalCurrentValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyAssetHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyAssetHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailyAssetHistory = new TableInfo("daily_asset_history", _columnsDailyAssetHistory, _foreignKeysDailyAssetHistory, _indicesDailyAssetHistory);
        final TableInfo _existingDailyAssetHistory = TableInfo.read(db, "daily_asset_history");
        if (!_infoDailyAssetHistory.equals(_existingDailyAssetHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_asset_history(com.example.vrapp.model.DailyAssetHistory).\n"
                  + " Expected:\n" + _infoDailyAssetHistory + "\n"
                  + " Found:\n" + _existingDailyAssetHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsStockHistory = new HashMap<String, TableInfo.Column>(9);
        _columnsStockHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockHistory.put("stockId", new TableInfo.Column("stockId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockHistory.put("vValue", new TableInfo.Column("vValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockHistory.put("gValue", new TableInfo.Column("gValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockHistory.put("currentPrice", new TableInfo.Column("currentPrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockHistory.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockHistory.put("pool", new TableInfo.Column("pool", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockHistory.put("investedPrincipal", new TableInfo.Column("investedPrincipal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStockHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStockHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStockHistory = new TableInfo("stock_history", _columnsStockHistory, _foreignKeysStockHistory, _indicesStockHistory);
        final TableInfo _existingStockHistory = TableInfo.read(db, "stock_history");
        if (!_infoStockHistory.equals(_existingStockHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "stock_history(com.example.vrapp.model.StockHistory).\n"
                  + " Expected:\n" + _infoStockHistory + "\n"
                  + " Found:\n" + _existingStockHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "2ddecd03d03e7da90c4fefced479bb51", "b61f36ac6df0288072e9f4f5e49011c3");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "stocks","transactions","daily_asset_history","stock_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `stocks`");
      _db.execSQL("DELETE FROM `transactions`");
      _db.execSQL("DELETE FROM `daily_asset_history`");
      _db.execSQL("DELETE FROM `stock_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(StockDao.class, StockDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    _autoMigrations.add(new AppDatabase_AutoMigration_4_5_Impl());
    _autoMigrations.add(new AppDatabase_AutoMigration_5_6_Impl());
    return _autoMigrations;
  }

  @Override
  public StockDao stockDao() {
    if (_stockDao != null) {
      return _stockDao;
    } else {
      synchronized(this) {
        if(_stockDao == null) {
          _stockDao = new StockDao_Impl(this);
        }
        return _stockDao;
      }
    }
  }
}
