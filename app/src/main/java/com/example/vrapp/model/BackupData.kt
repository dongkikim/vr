package com.example.vrapp.model

data class BackupData(
    val version: Int = 2, // Increment version for IB support
    val timestamp: Long = System.currentTimeMillis(),
    val stocks: List<Stock> = emptyList(),
    val transactions: List<TransactionHistory> = emptyList(),
    val dailyHistory: List<DailyAssetHistory> = emptyList(),
    val stockHistory: List<StockHistory> = emptyList(),
    
    // 무한매수(IB) 관련 데이터 추가
    val ibAccounts: List<IbAccount> = emptyList(),
    val ibStocks: List<IbStock> = emptyList(),
    val ibTransactions: List<IbTransaction> = emptyList(),
    val ibWalletHistory: List<IbWalletHistory> = emptyList()
)
