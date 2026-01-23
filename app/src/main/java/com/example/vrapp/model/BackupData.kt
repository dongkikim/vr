package com.example.vrapp.model

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val stocks: List<Stock> = emptyList(),
    val transactions: List<TransactionHistory> = emptyList(),
    val dailyHistory: List<DailyAssetHistory> = emptyList(),
    val stockHistory: List<StockHistory> = emptyList()
)
