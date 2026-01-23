package com.example.vrapp.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.vrapp.model.DailyAssetHistory
import com.example.vrapp.viewmodel.StockViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    viewModel: StockViewModel,
    onBack: () -> Unit
) {
    val historyList by viewModel.dailyHistory.collectAsState()
    var selectedPeriod by remember { mutableStateOf(ChartPeriod.ONE_MONTH) }
    
    // Custom Date Range State
    var showDatePicker by remember { mutableStateOf(false) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    
    // Date Formatter for parsing history dates
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Selected Item for Summary Card
    var selectedHistoryItem by remember { mutableStateOf<DailyAssetHistory?>(null) }
    
    // Reset selection when historyList changes or period changes
    LaunchedEffect(historyList, selectedPeriod, startDateMillis, endDateMillis) {
        selectedHistoryItem = null
    }

    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDateMillis,
            initialSelectedEndDateMillis = endDateMillis
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDateMillis = dateRangePickerState.selectedStartDateMillis
                        endDateMillis = dateRangePickerState.selectedEndDateMillis
                        if (startDateMillis != null && endDateMillis != null) {
                            selectedPeriod = ChartPeriod.CUSTOM
                        }
                        showDatePicker = false
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DateRangePicker(state = dateRangePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("자산 추이 차트") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("아직 데이터가 없습니다. 주식을 추가하거나 거래를 시작해보세요.")
                }
            } else {
                val filteredHistory = remember(selectedPeriod, startDateMillis, endDateMillis, historyList) {
                    when (selectedPeriod) {
                        ChartPeriod.CUSTOM -> {
                            if (startDateMillis != null && endDateMillis != null) {
                                historyList.filter { 
                                    val dateMillis = dateFormatter.parse(it.date)?.time ?: 0L
                                    dateMillis in startDateMillis!!..endDateMillis!!
                                }
                            } else {
                                historyList
                            }
                        }
                        else -> historyList.takeLast(selectedPeriod.days)
                    }
                }
                
                // 상단 통계 카드 (선택된 항목 또는 마지막 항목)
                val displayItem = selectedHistoryItem ?: filteredHistory.lastOrNull() ?: historyList.last()
                ChartSummaryCard(displayItem)
                
                Spacer(modifier = Modifier.height(16.dp))

                // 기간 선택 UI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChartPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                if (period == ChartPeriod.CUSTOM) {
                                    showDatePicker = true
                                } else {
                                    selectedPeriod = period
                                }
                            },
                            label = { 
                                if (period == ChartPeriod.CUSTOM && startDateMillis != null && endDateMillis != null && selectedPeriod == ChartPeriod.CUSTOM) {
                                    val start = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(startDateMillis!!))
                                    val end = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(endDateMillis!!))
                                    Text("$start~$end", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Text(period.label, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // MPAndroidChart
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // 남은 공간 모두 차지
                    factory = { context ->
                        LineChart(context).apply {
                            // 기본 설정
                            description.isEnabled = false // 설명 텍스트 제거
                            setTouchEnabled(true) // 터치 활성화
                            isDragEnabled = true // 드래그 활성화
                            setScaleEnabled(true) // 줌 활성화
                            setPinchZoom(true) // 핀치 줌 활성화
                            
                            // X축 설정
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                granularity = 1f // 1단위로 라벨 표시
                                isGranularityEnabled = true
                            }
                            
                            // Y축 설정 (오른쪽 축 제거)
                            axisRight.isEnabled = false
                            axisLeft.apply {
                                setDrawGridLines(true)
                            }
                            
                            // 범례 설정
                            legend.apply {
                                isEnabled = true
                                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                            }

                            // 리스너 설정
                            setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                                    e?.let {
                                        val index = it.x.toInt()
                                        if (index in filteredHistory.indices) {
                                            selectedHistoryItem = filteredHistory[index]
                                        }
                                    }
                                }
                                override fun onNothingSelected() {
                                    selectedHistoryItem = null
                                }
                            })
                        }
                    },
                    update = { chart ->
                        // 데이터 변환
                        val principalEntries = filteredHistory.mapIndexed { index, item ->
                            Entry(index.toFloat(), item.totalPrincipal.toFloat())
                        }
                        val currentEntries = filteredHistory.mapIndexed { index, item ->
                            Entry(index.toFloat(), item.totalCurrentValue.toFloat())
                        }
                        
                        // 날짜 라벨 (MM/dd)
                        val dateLabels = filteredHistory.map { item ->
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
                                formatter.format(parser.parse(item.date) ?: Date())
                            } catch (e: Exception) {
                                item.date
                            }
                        }

                        // Max/Min 값의 첫 번째 인덱스 찾기 (X좌표 기준 가장 왼쪽)
                        val maxVal = currentEntries.maxOfOrNull { it.y } ?: 0f
                        val minVal = currentEntries.minOfOrNull { it.y } ?: 0f
                        val firstMaxX = currentEntries.firstOrNull { it.y == maxVal }?.x ?: -1f
                        val firstMinX = currentEntries.firstOrNull { it.y == minVal }?.x ?: -1f
                        
                        // Custom Formatter
                        val customFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                            override fun getPointLabel(entry: Entry?): String {
                                if (entry == null) return ""
                                // 해당 값이 Max/Min이면서 동시에 첫 번째로 나타난 위치인 경우에만 표시
                                return if ((entry.y == maxVal && entry.x == firstMaxX) || 
                                           (entry.y == minVal && entry.x == firstMinX)) {
                                    java.text.NumberFormat.getIntegerInstance().format(entry.y.toLong())
                                } else {
                                    ""
                                }
                            }
                        }

                        // DataSet 설정
                        val principalSet = LineDataSet(principalEntries, "총 투자원금").apply {
                            color = AndroidColor.RED
                            setCircleColor(AndroidColor.RED)
                            lineWidth = 2f
                            circleRadius = 2f // 작은 점
                            setDrawCircles(true) 
                            setDrawCircleHole(false)
                            setDrawValues(false)
                            highLightColor = AndroidColor.GRAY
                        }

                        val currentSet = LineDataSet(currentEntries, "총 잔고").apply {
                            color = AndroidColor.BLUE
                            setCircleColor(AndroidColor.BLUE)
                            lineWidth = 3f
                            circleRadius = 3f // 조금 더 큰 점
                            setDrawCircles(true)
                            setDrawCircleHole(false)
                            setDrawValues(true)
                            valueFormatter = customFormatter
                            valueTextSize = 10f
                            valueTypeface = android.graphics.Typeface.DEFAULT_BOLD
                            highLightColor = AndroidColor.GRAY
                        }

                        // X축 라벨 적용
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
                        
                        // 차트 데이터 반영
                        chart.data = LineData(principalSet, currentSet)
                        
                        // 뷰포트 이동 (마지막 데이터로)
                        chart.moveViewToX(filteredHistory.size.toFloat())
                        
                        // 갱신 (데이터셋 변경 시 notify 필요)
                        chart.notifyDataSetChanged()
                        chart.invalidate()
                    }
                )
            }
        }
    }
}

@Composable
fun ChartSummaryCard(lastItem: DailyAssetHistory) {
    val profit = lastItem.totalCurrentValue - lastItem.totalPrincipal
    val roi = if (lastItem.totalPrincipal > 0) (profit / lastItem.totalPrincipal) * 100 else 0.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("최신 자산 현황 (${lastItem.date})", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("투자원금")
                Text(formatCurrency(lastItem.totalPrincipal, "KRW"))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("총 잔고")
                Text(formatCurrency(lastItem.totalCurrentValue, "KRW"), fontWeight = FontWeight.Bold)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("손익")
                Text(
                    "${formatCurrency(profit, "KRW")} (${String.format("%.1f", roi)}%)",
                    color = if (profit >= 0) Color.Red else Color.Blue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}