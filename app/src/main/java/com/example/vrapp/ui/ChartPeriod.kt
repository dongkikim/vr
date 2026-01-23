package com.example.vrapp.ui

enum class ChartPeriod(val label: String, val days: Int) {
    ONE_WEEK("1주", 7),
    ONE_MONTH("1달", 30),
    ONE_YEAR("1년", 365),
    CUSTOM("직접설정", 0)
}