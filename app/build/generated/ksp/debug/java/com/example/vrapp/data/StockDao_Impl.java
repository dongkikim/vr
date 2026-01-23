package com.example.vrapp.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.vrapp.model.DailyAssetHistory;
import com.example.vrapp.model.Stock;
import com.example.vrapp.model.StockHistory;
import com.example.vrapp.model.TransactionHistory;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StockDao_Impl implements StockDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Stock> __insertionAdapterOfStock;

  private final EntityInsertionAdapter<TransactionHistory> __insertionAdapterOfTransactionHistory;

  private final EntityInsertionAdapter<DailyAssetHistory> __insertionAdapterOfDailyAssetHistory;

  private final EntityInsertionAdapter<StockHistory> __insertionAdapterOfStockHistory;

  private final EntityDeletionOrUpdateAdapter<Stock> __updateAdapterOfStock;

  private final SharedSQLiteStatement __preparedStmtOfDeleteStock;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllStocks;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllTransactions;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllDailyHistory;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllStockHistory;

  public StockDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStock = new EntityInsertionAdapter<Stock>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `stocks` (`id`,`name`,`ticker`,`vValue`,`gValue`,`pool`,`quantity`,`investedPrincipal`,`currentPrice`,`currency`,`startDate`,`defaultRecalcAmount`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Stock entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getTicker());
        statement.bindDouble(4, entity.getVValue());
        statement.bindDouble(5, entity.getGValue());
        statement.bindDouble(6, entity.getPool());
        statement.bindLong(7, entity.getQuantity());
        statement.bindDouble(8, entity.getInvestedPrincipal());
        statement.bindDouble(9, entity.getCurrentPrice());
        statement.bindString(10, entity.getCurrency());
        statement.bindLong(11, entity.getStartDate());
        statement.bindDouble(12, entity.getDefaultRecalcAmount());
      }
    };
    this.__insertionAdapterOfTransactionHistory = new EntityInsertionAdapter<TransactionHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `transactions` (`id`,`stockId`,`date`,`type`,`price`,`quantity`,`amount`,`previousV`,`newV`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionHistory entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStockId());
        statement.bindLong(3, entity.getDate());
        statement.bindString(4, entity.getType());
        statement.bindDouble(5, entity.getPrice());
        statement.bindLong(6, entity.getQuantity());
        statement.bindDouble(7, entity.getAmount());
        if (entity.getPreviousV() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getPreviousV());
        }
        if (entity.getNewV() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getNewV());
        }
      }
    };
    this.__insertionAdapterOfDailyAssetHistory = new EntityInsertionAdapter<DailyAssetHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_asset_history` (`date`,`totalPrincipal`,`totalCurrentValue`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyAssetHistory entity) {
        statement.bindString(1, entity.getDate());
        statement.bindDouble(2, entity.getTotalPrincipal());
        statement.bindDouble(3, entity.getTotalCurrentValue());
      }
    };
    this.__insertionAdapterOfStockHistory = new EntityInsertionAdapter<StockHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `stock_history` (`id`,`stockId`,`timestamp`,`vValue`,`gValue`,`currentPrice`,`quantity`,`pool`,`investedPrincipal`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StockHistory entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStockId());
        statement.bindLong(3, entity.getTimestamp());
        statement.bindDouble(4, entity.getVValue());
        statement.bindDouble(5, entity.getGValue());
        statement.bindDouble(6, entity.getCurrentPrice());
        statement.bindLong(7, entity.getQuantity());
        statement.bindDouble(8, entity.getPool());
        statement.bindDouble(9, entity.getInvestedPrincipal());
      }
    };
    this.__updateAdapterOfStock = new EntityDeletionOrUpdateAdapter<Stock>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `stocks` SET `id` = ?,`name` = ?,`ticker` = ?,`vValue` = ?,`gValue` = ?,`pool` = ?,`quantity` = ?,`investedPrincipal` = ?,`currentPrice` = ?,`currency` = ?,`startDate` = ?,`defaultRecalcAmount` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Stock entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getTicker());
        statement.bindDouble(4, entity.getVValue());
        statement.bindDouble(5, entity.getGValue());
        statement.bindDouble(6, entity.getPool());
        statement.bindLong(7, entity.getQuantity());
        statement.bindDouble(8, entity.getInvestedPrincipal());
        statement.bindDouble(9, entity.getCurrentPrice());
        statement.bindString(10, entity.getCurrency());
        statement.bindLong(11, entity.getStartDate());
        statement.bindDouble(12, entity.getDefaultRecalcAmount());
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteStock = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM stocks WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllStocks = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM stocks";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllTransactions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllDailyHistory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_asset_history";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllStockHistory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM stock_history";
        return _query;
      }
    };
  }

  @Override
  public Object insertStock(final Stock stock, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStock.insert(stock);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertStocks(final List<Stock> stocks,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStock.insert(stocks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTransaction(final TransactionHistory transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransactionHistory.insert(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTransactions(final List<TransactionHistory> transactions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransactionHistory.insert(transactions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertDailyHistory(final DailyAssetHistory history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyAssetHistory.insert(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertDailyHistories(final List<DailyAssetHistory> histories,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyAssetHistory.insert(histories);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertStockHistory(final StockHistory history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStockHistory.insert(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertStockHistories(final List<StockHistory> histories,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStockHistory.insert(histories);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStock(final Stock stock, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfStock.handle(stock);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteStock(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteStock.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteStock.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllStocks(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllStocks.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllStocks.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllTransactions(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllTransactions.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllTransactions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllDailyHistory(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllDailyHistory.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllDailyHistory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllStockHistory(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllStockHistory.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllStockHistory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Stock>> getAllStocks() {
    final String _sql = "SELECT * FROM stocks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stocks"}, new Callable<List<Stock>>() {
      @Override
      @NonNull
      public List<Stock> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTicker = CursorUtil.getColumnIndexOrThrow(_cursor, "ticker");
          final int _cursorIndexOfVValue = CursorUtil.getColumnIndexOrThrow(_cursor, "vValue");
          final int _cursorIndexOfGValue = CursorUtil.getColumnIndexOrThrow(_cursor, "gValue");
          final int _cursorIndexOfPool = CursorUtil.getColumnIndexOrThrow(_cursor, "pool");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfInvestedPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "investedPrincipal");
          final int _cursorIndexOfCurrentPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfDefaultRecalcAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRecalcAmount");
          final List<Stock> _result = new ArrayList<Stock>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Stock _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTicker;
            _tmpTicker = _cursor.getString(_cursorIndexOfTicker);
            final double _tmpVValue;
            _tmpVValue = _cursor.getDouble(_cursorIndexOfVValue);
            final double _tmpGValue;
            _tmpGValue = _cursor.getDouble(_cursorIndexOfGValue);
            final double _tmpPool;
            _tmpPool = _cursor.getDouble(_cursorIndexOfPool);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final double _tmpInvestedPrincipal;
            _tmpInvestedPrincipal = _cursor.getDouble(_cursorIndexOfInvestedPrincipal);
            final double _tmpCurrentPrice;
            _tmpCurrentPrice = _cursor.getDouble(_cursorIndexOfCurrentPrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final double _tmpDefaultRecalcAmount;
            _tmpDefaultRecalcAmount = _cursor.getDouble(_cursorIndexOfDefaultRecalcAmount);
            _item = new Stock(_tmpId,_tmpName,_tmpTicker,_tmpVValue,_tmpGValue,_tmpPool,_tmpQuantity,_tmpInvestedPrincipal,_tmpCurrentPrice,_tmpCurrency,_tmpStartDate,_tmpDefaultRecalcAmount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getStockById(final long id, final Continuation<? super Stock> $completion) {
    final String _sql = "SELECT * FROM stocks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Stock>() {
      @Override
      @Nullable
      public Stock call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTicker = CursorUtil.getColumnIndexOrThrow(_cursor, "ticker");
          final int _cursorIndexOfVValue = CursorUtil.getColumnIndexOrThrow(_cursor, "vValue");
          final int _cursorIndexOfGValue = CursorUtil.getColumnIndexOrThrow(_cursor, "gValue");
          final int _cursorIndexOfPool = CursorUtil.getColumnIndexOrThrow(_cursor, "pool");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfInvestedPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "investedPrincipal");
          final int _cursorIndexOfCurrentPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfDefaultRecalcAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRecalcAmount");
          final Stock _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTicker;
            _tmpTicker = _cursor.getString(_cursorIndexOfTicker);
            final double _tmpVValue;
            _tmpVValue = _cursor.getDouble(_cursorIndexOfVValue);
            final double _tmpGValue;
            _tmpGValue = _cursor.getDouble(_cursorIndexOfGValue);
            final double _tmpPool;
            _tmpPool = _cursor.getDouble(_cursorIndexOfPool);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final double _tmpInvestedPrincipal;
            _tmpInvestedPrincipal = _cursor.getDouble(_cursorIndexOfInvestedPrincipal);
            final double _tmpCurrentPrice;
            _tmpCurrentPrice = _cursor.getDouble(_cursorIndexOfCurrentPrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final double _tmpDefaultRecalcAmount;
            _tmpDefaultRecalcAmount = _cursor.getDouble(_cursorIndexOfDefaultRecalcAmount);
            _result = new Stock(_tmpId,_tmpName,_tmpTicker,_tmpVValue,_tmpGValue,_tmpPool,_tmpQuantity,_tmpInvestedPrincipal,_tmpCurrentPrice,_tmpCurrency,_tmpStartDate,_tmpDefaultRecalcAmount);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TransactionHistory>> getHistoryForStock(final long stockId) {
    final String _sql = "SELECT * FROM transactions WHERE stockId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, stockId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionHistory>>() {
      @Override
      @NonNull
      public List<TransactionHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStockId = CursorUtil.getColumnIndexOrThrow(_cursor, "stockId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPreviousV = CursorUtil.getColumnIndexOrThrow(_cursor, "previousV");
          final int _cursorIndexOfNewV = CursorUtil.getColumnIndexOrThrow(_cursor, "newV");
          final List<TransactionHistory> _result = new ArrayList<TransactionHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionHistory _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStockId;
            _tmpStockId = _cursor.getLong(_cursorIndexOfStockId);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Double _tmpPreviousV;
            if (_cursor.isNull(_cursorIndexOfPreviousV)) {
              _tmpPreviousV = null;
            } else {
              _tmpPreviousV = _cursor.getDouble(_cursorIndexOfPreviousV);
            }
            final Double _tmpNewV;
            if (_cursor.isNull(_cursorIndexOfNewV)) {
              _tmpNewV = null;
            } else {
              _tmpNewV = _cursor.getDouble(_cursorIndexOfNewV);
            }
            _item = new TransactionHistory(_tmpId,_tmpStockId,_tmpDate,_tmpType,_tmpPrice,_tmpQuantity,_tmpAmount,_tmpPreviousV,_tmpNewV);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<DailyAssetHistory>> getDailyHistory() {
    final String _sql = "SELECT * FROM daily_asset_history ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_asset_history"}, new Callable<List<DailyAssetHistory>>() {
      @Override
      @NonNull
      public List<DailyAssetHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPrincipal");
          final int _cursorIndexOfTotalCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCurrentValue");
          final List<DailyAssetHistory> _result = new ArrayList<DailyAssetHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyAssetHistory _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final double _tmpTotalPrincipal;
            _tmpTotalPrincipal = _cursor.getDouble(_cursorIndexOfTotalPrincipal);
            final double _tmpTotalCurrentValue;
            _tmpTotalCurrentValue = _cursor.getDouble(_cursorIndexOfTotalCurrentValue);
            _item = new DailyAssetHistory(_tmpDate,_tmpTotalPrincipal,_tmpTotalCurrentValue);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<StockHistory>> getStockHistory(final long stockId) {
    final String _sql = "SELECT * FROM stock_history WHERE stockId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, stockId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stock_history"}, new Callable<List<StockHistory>>() {
      @Override
      @NonNull
      public List<StockHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStockId = CursorUtil.getColumnIndexOrThrow(_cursor, "stockId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfVValue = CursorUtil.getColumnIndexOrThrow(_cursor, "vValue");
          final int _cursorIndexOfGValue = CursorUtil.getColumnIndexOrThrow(_cursor, "gValue");
          final int _cursorIndexOfCurrentPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPrice");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPool = CursorUtil.getColumnIndexOrThrow(_cursor, "pool");
          final int _cursorIndexOfInvestedPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "investedPrincipal");
          final List<StockHistory> _result = new ArrayList<StockHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StockHistory _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStockId;
            _tmpStockId = _cursor.getLong(_cursorIndexOfStockId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpVValue;
            _tmpVValue = _cursor.getDouble(_cursorIndexOfVValue);
            final double _tmpGValue;
            _tmpGValue = _cursor.getDouble(_cursorIndexOfGValue);
            final double _tmpCurrentPrice;
            _tmpCurrentPrice = _cursor.getDouble(_cursorIndexOfCurrentPrice);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final double _tmpPool;
            _tmpPool = _cursor.getDouble(_cursorIndexOfPool);
            final double _tmpInvestedPrincipal;
            _tmpInvestedPrincipal = _cursor.getDouble(_cursorIndexOfInvestedPrincipal);
            _item = new StockHistory(_tmpId,_tmpStockId,_tmpTimestamp,_tmpVValue,_tmpGValue,_tmpCurrentPrice,_tmpQuantity,_tmpPool,_tmpInvestedPrincipal);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllStocksSnapshot(final Continuation<? super List<Stock>> $completion) {
    final String _sql = "SELECT * FROM stocks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Stock>>() {
      @Override
      @NonNull
      public List<Stock> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTicker = CursorUtil.getColumnIndexOrThrow(_cursor, "ticker");
          final int _cursorIndexOfVValue = CursorUtil.getColumnIndexOrThrow(_cursor, "vValue");
          final int _cursorIndexOfGValue = CursorUtil.getColumnIndexOrThrow(_cursor, "gValue");
          final int _cursorIndexOfPool = CursorUtil.getColumnIndexOrThrow(_cursor, "pool");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfInvestedPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "investedPrincipal");
          final int _cursorIndexOfCurrentPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfDefaultRecalcAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRecalcAmount");
          final List<Stock> _result = new ArrayList<Stock>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Stock _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTicker;
            _tmpTicker = _cursor.getString(_cursorIndexOfTicker);
            final double _tmpVValue;
            _tmpVValue = _cursor.getDouble(_cursorIndexOfVValue);
            final double _tmpGValue;
            _tmpGValue = _cursor.getDouble(_cursorIndexOfGValue);
            final double _tmpPool;
            _tmpPool = _cursor.getDouble(_cursorIndexOfPool);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final double _tmpInvestedPrincipal;
            _tmpInvestedPrincipal = _cursor.getDouble(_cursorIndexOfInvestedPrincipal);
            final double _tmpCurrentPrice;
            _tmpCurrentPrice = _cursor.getDouble(_cursorIndexOfCurrentPrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final double _tmpDefaultRecalcAmount;
            _tmpDefaultRecalcAmount = _cursor.getDouble(_cursorIndexOfDefaultRecalcAmount);
            _item = new Stock(_tmpId,_tmpName,_tmpTicker,_tmpVValue,_tmpGValue,_tmpPool,_tmpQuantity,_tmpInvestedPrincipal,_tmpCurrentPrice,_tmpCurrency,_tmpStartDate,_tmpDefaultRecalcAmount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllTransactionsSnapshot(
      final Continuation<? super List<TransactionHistory>> $completion) {
    final String _sql = "SELECT * FROM transactions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TransactionHistory>>() {
      @Override
      @NonNull
      public List<TransactionHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStockId = CursorUtil.getColumnIndexOrThrow(_cursor, "stockId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPreviousV = CursorUtil.getColumnIndexOrThrow(_cursor, "previousV");
          final int _cursorIndexOfNewV = CursorUtil.getColumnIndexOrThrow(_cursor, "newV");
          final List<TransactionHistory> _result = new ArrayList<TransactionHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionHistory _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStockId;
            _tmpStockId = _cursor.getLong(_cursorIndexOfStockId);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Double _tmpPreviousV;
            if (_cursor.isNull(_cursorIndexOfPreviousV)) {
              _tmpPreviousV = null;
            } else {
              _tmpPreviousV = _cursor.getDouble(_cursorIndexOfPreviousV);
            }
            final Double _tmpNewV;
            if (_cursor.isNull(_cursorIndexOfNewV)) {
              _tmpNewV = null;
            } else {
              _tmpNewV = _cursor.getDouble(_cursorIndexOfNewV);
            }
            _item = new TransactionHistory(_tmpId,_tmpStockId,_tmpDate,_tmpType,_tmpPrice,_tmpQuantity,_tmpAmount,_tmpPreviousV,_tmpNewV);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllDailyHistorySnapshot(
      final Continuation<? super List<DailyAssetHistory>> $completion) {
    final String _sql = "SELECT * FROM daily_asset_history";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyAssetHistory>>() {
      @Override
      @NonNull
      public List<DailyAssetHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPrincipal");
          final int _cursorIndexOfTotalCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCurrentValue");
          final List<DailyAssetHistory> _result = new ArrayList<DailyAssetHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyAssetHistory _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final double _tmpTotalPrincipal;
            _tmpTotalPrincipal = _cursor.getDouble(_cursorIndexOfTotalPrincipal);
            final double _tmpTotalCurrentValue;
            _tmpTotalCurrentValue = _cursor.getDouble(_cursorIndexOfTotalCurrentValue);
            _item = new DailyAssetHistory(_tmpDate,_tmpTotalPrincipal,_tmpTotalCurrentValue);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllStockHistorySnapshot(
      final Continuation<? super List<StockHistory>> $completion) {
    final String _sql = "SELECT * FROM stock_history";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<StockHistory>>() {
      @Override
      @NonNull
      public List<StockHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStockId = CursorUtil.getColumnIndexOrThrow(_cursor, "stockId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfVValue = CursorUtil.getColumnIndexOrThrow(_cursor, "vValue");
          final int _cursorIndexOfGValue = CursorUtil.getColumnIndexOrThrow(_cursor, "gValue");
          final int _cursorIndexOfCurrentPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPrice");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPool = CursorUtil.getColumnIndexOrThrow(_cursor, "pool");
          final int _cursorIndexOfInvestedPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "investedPrincipal");
          final List<StockHistory> _result = new ArrayList<StockHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StockHistory _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStockId;
            _tmpStockId = _cursor.getLong(_cursorIndexOfStockId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpVValue;
            _tmpVValue = _cursor.getDouble(_cursorIndexOfVValue);
            final double _tmpGValue;
            _tmpGValue = _cursor.getDouble(_cursorIndexOfGValue);
            final double _tmpCurrentPrice;
            _tmpCurrentPrice = _cursor.getDouble(_cursorIndexOfCurrentPrice);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final double _tmpPool;
            _tmpPool = _cursor.getDouble(_cursorIndexOfPool);
            final double _tmpInvestedPrincipal;
            _tmpInvestedPrincipal = _cursor.getDouble(_cursorIndexOfInvestedPrincipal);
            _item = new StockHistory(_tmpId,_tmpStockId,_tmpTimestamp,_tmpVValue,_tmpGValue,_tmpCurrentPrice,_tmpQuantity,_tmpPool,_tmpInvestedPrincipal);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
