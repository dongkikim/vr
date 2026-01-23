package com.example.vrapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.vrapp.ui.VRApp
import com.example.vrapp.ui.theme.VRAppTheme
import com.example.vrapp.viewmodel.StockViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[StockViewModel::class.java]

        setContent {
            VRAppTheme {
                VRApp(viewModel = viewModel)
            }
        }
    }
}
