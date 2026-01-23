package com.example.vrapp.data;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.lang.Override;
import java.lang.SuppressWarnings;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
final class AppDatabase_AutoMigration_5_6_Impl extends Migration {
  public AppDatabase_AutoMigration_5_6_Impl() {
    super(5, 6);
  }

  @Override
  public void migrate(@NonNull final SupportSQLiteDatabase db) {
    db.execSQL("ALTER TABLE `stocks` ADD COLUMN `defaultRecalcAmount` REAL NOT NULL DEFAULT 0.0");
  }
}
