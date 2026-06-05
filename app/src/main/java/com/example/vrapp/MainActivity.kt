package com.example.vrapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.vrapp.ui.VRApp
import com.example.vrapp.ui.theme.VRAppTheme
import com.example.vrapp.logic.IBNotificationManager
import com.example.vrapp.viewmodel.IbViewModel
import com.example.vrapp.viewmodel.StockViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 무한매수 알림 채널 초기화
        IBNotificationManager.createNotificationChannel(this)

        val stockViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[StockViewModel::class.java]

        val ibViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IbViewModel::class.java]

        setContent {
            VRAppTheme {
                VRApp(stockViewModel = stockViewModel, ibViewModel = ibViewModel)
            }
        }
    }
}
