package com.example.vrapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.vrapp.logic.VRCalculator
import com.example.vrapp.model.Stock
import com.example.vrapp.viewmodel.StockViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.graphics.Color as AndroidColor
import com.example.vrapp.model.StockHistory
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: StockViewModel,
    stockId: Long,
    onBack: () -> Unit
) {
    // Load data
    LaunchedEffect(stockId) {
        viewModel.loadStock(stockId)
    }

    val stock by viewModel.currentStock.collectAsState()
    val stockHistory by viewModel.stockHistory.collectAsState()
    
    // Dialog States
    var showPoolDialog by remember { mutableStateOf(false) }
    var showTradeDialog by remember { mutableStateOf(false) }
    var showRecalcDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPrincipalDialog by remember { mutableStateOf(false) }
    var showStartDateDialog by remember { mutableStateOf(false) }
    var showGValueDialog by remember { mutableStateOf(false) }
    var showDefaultRecalcDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stock?.name ?: "종목 상세", style = MaterialTheme.typography.titleMedium)
                        if (stock != null && stock!!.startDate > 0) {
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(stock!!.startDate))
                            Text(
                                text = " | $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("종목명 변경") },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("G값(기울기) 변경") },
                                onClick = {
                                    showMenu = false
                                    showGValueDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("기본 입출금액 설정") },
                                onClick = {
                                    showMenu = false
                                    showDefaultRecalcDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("원금 변경") },
                                onClick = {
                                    showMenu = false
                                    showPrincipalDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("투자 시작일 변경") },
                                onClick = {
                                    showMenu = false
                                    showStartDateDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (stock != null) {
            val s = stock!!
            var selectedPeriod by remember { mutableStateOf(ChartPeriod.ONE_MONTH) }
            
            // Custom Date Range State
            var showDatePicker by remember { mutableStateOf(false) }
            var startDateMillis by remember { mutableStateOf<Long?>(null) }
            var endDateMillis by remember { mutableStateOf<Long?>(null) }

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

            val filteredHistory = remember(selectedPeriod, startDateMillis, endDateMillis, stockHistory) {
                when (selectedPeriod) {
                    ChartPeriod.CUSTOM -> {
                        if (startDateMillis != null && endDateMillis != null) {
                            stockHistory.filter { it.timestamp in startDateMillis!!..endDateMillis!! }
                        } else {
                            stockHistory
                        }
                    }
                    else -> stockHistory.takeLast(selectedPeriod.days)
                }
            }

            var selectedHistoryItem by remember { mutableStateOf<StockHistory?>(null) }
            
            // Reset selection when filter changes
            LaunchedEffect(filteredHistory) {
                selectedHistoryItem = null
            }

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Top Info Card (Status + Guide)
                TopInfoCard(s)

                Divider()

                // 2. Buttons Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showPoolDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) { Text("예수금 관리") }
                        Button(
                            onClick = { showTradeDialog = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("거래 기록") }
                    }
                    
                    Button(
                        onClick = { showRecalcDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("V값 재산출 (리밸런싱)")
                    }
                }

                Divider()

                // 3. Chart Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    
                    // Selected Point Info
                    selectedHistoryItem?.let { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                val totalVal = (item.currentPrice * item.quantity) + item.pool
                                Column {
                                    Text("선택일시: $dateStr", style = MaterialTheme.typography.labelSmall)
                                    Text("총잔고: ${formatCurrency(totalVal, s.currency)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("원금: ${formatCurrency(item.investedPrincipal, s.currency)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Pool: ${formatCurrency(item.pool, s.currency)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    StockHistoryChart(
                        historyList = filteredHistory,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        onValueSelected = { selectedHistoryItem = it }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Dialogs
    if (showPoolDialog && stock != null) {
        ManagePoolDialog(
            stock = stock!!,
            onDismiss = { showPoolDialog = false },
            onConfirm = { amount, isDeposit ->
                viewModel.updatePool(stock!!, amount, isDeposit)
                showPoolDialog = false
            }
        )
    }

    if (showTradeDialog && stock != null) {
        TradeDialog(
            stock = stock!!,
            onDismiss = { showTradeDialog = false },
            onConfirm = { type, price, qty ->
                viewModel.executeTransaction(stock!!, type, price, qty)
                showTradeDialog = false
            }
        )
    }

    if (showRecalcDialog && stock != null) {
        RecalculateVDialog(
            stock = stock!!,
            onDismiss = { showRecalcDialog = false },
            onConfirm = { amount, isDeposit ->
                viewModel.recalculateVWithDeposit(stock!!, amount, isDeposit)
                showRecalcDialog = false
            }
        )
    }

    if (showRenameDialog && stock != null) {
        RenameStockDialog(
            stock = stock!!,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.updateStockName(stock!!, newName)
                showRenameDialog = false
            }
        )
    }

    if (showGValueDialog && stock != null) {
        ChangeGValueDialog(
            stock = stock!!,
            onDismiss = { showGValueDialog = false },
            onConfirm = { newG ->
                viewModel.updateStockGValue(stock!!, newG)
                showGValueDialog = false
            }
        )
    }

    if (showDefaultRecalcDialog && stock != null) {
        SetDefaultRecalcDialog(
            stock = stock!!,
            onDismiss = { showDefaultRecalcDialog = false },
            onConfirm = { amount ->
                viewModel.updateDefaultRecalcAmount(stock!!, amount)
                showDefaultRecalcDialog = false
            }
        )
    }

    if (showPrincipalDialog && stock != null) {
        ChangePrincipalDialog(
            stock = stock!!,
            onDismiss = { showPrincipalDialog = false },
            onConfirm = { newPrincipal ->
                viewModel.updateStockPrincipal(stock!!, newPrincipal)
                showPrincipalDialog = false
            }
        )
    }

    if (showStartDateDialog && stock != null) {
        ChangeStartDateDialog(
            stock = stock!!,
            onDismiss = { showStartDateDialog = false },
            onConfirm = { newDate ->
                viewModel.updateStockStartDate(stock!!, newDate)
                showStartDateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeStartDateDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    // startDate가 0이면(마이그레이션 데이터) 오늘 날짜로 초기값 설정
    val initialDate = if (stock.startDate > 0) stock.startDate else System.currentTimeMillis()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(datePickerState.selectedDateMillis ?: initialDate)
            }) { Text("변경") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun TopInfoCard(stock: Stock) {
    val currentValuation = stock.currentPrice * stock.quantity
    val currentTotal = currentValuation + stock.pool
    val totalProfitLoss = currentTotal - stock.investedPrincipal
    val totalRoi = if (stock.investedPrincipal > 0) (totalProfitLoss / stock.investedPrincipal) * 100 else 0.0
    
    val bands = VRCalculator.calculateBands(stock.vValue, stock.gValue)
    val order = VRCalculator.calculateOrder(currentValuation, stock.currentPrice, bands)
    val priceTable = VRCalculator.calculatePriceTable(
        currentQuantity = stock.quantity, 
        bands = bands, 
        range = 10, 
        ticker = stock.ticker, 
        currency = stock.currency
    )

    val annualizedRoi = remember(stock.startDate, totalRoi) {
        if (stock.startDate <= 0) 0.0 
        else {
            val now = System.currentTimeMillis()
            val diffMillis = now - stock.startDate
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toDouble()
            if (diffDays <= 0) totalRoi else totalRoi * (365.0 / diffDays)
        }
    }

    Card(border = ButtonDefaults.outlinedButtonBorder) {
        Column(modifier = Modifier.padding(12.dp)) { // 패딩 약간 축소
            // Row 1: Price | ROI | Annualized
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusItem("현재가", formatCurrency(stock.currentPrice, stock.currency), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
                StatusItem("수익률", String.format("%+.1f%%", totalRoi), color = if (totalRoi >= 0) Color.Red else Color.Blue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
                StatusItem("연평균", if (stock.startDate <= 0) "-" else String.format("%+.1f%%", annualizedRoi), color = if (annualizedRoi >= 0) Color.Red else Color.Blue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
            }
            Spacer(modifier = Modifier.height(8.dp)) // 간격 통일 (8dp)
            
            // Row 2: Total Asset | Pool | Principal
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusItem("총자산", formatCurrency(currentTotal, stock.currency), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
                StatusItem("Pool (예수금)", formatCurrency(stock.pool, stock.currency), modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
                StatusItem("투자 원금", formatCurrency(stock.investedPrincipal, stock.currency), modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
            }
            Spacer(modifier = Modifier.height(8.dp)) // 간격 통일 (8dp)

            // Row 3: Valuation | V | Status
            val diff = currentValuation - stock.vValue
            val diffText = if (diff >= 0) "+${formatCurrency(diff, stock.currency)}" else formatCurrency(diff, stock.currency)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusItem("현재평가액", formatCurrency(currentValuation, stock.currency), modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
                StatusItem("V", formatCurrency(stock.vValue, stock.currency), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
                StatusItem("상태", diffText, color = if (diff >= 0) Color.Red else Color.Blue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally)
            }

            Spacer(modifier = Modifier.height(12.dp)) // 표 시작 전 간격은 약간 더 줌
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Band Table
            Row(Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha=0.2f)).padding(6.dp)) {
                Text("구분", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("목표 평가액", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("행동", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Divider()

            // High Band
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("매도 밴드 (+${stock.gValue.toInt()}%)", modifier = Modifier.weight(1f), color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                Text(formatCurrency(bands.highValuation, stock.currency), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (order.action == VRCalculator.OrderAction.SELL) {
                     Text("매도 ${order.quantity}주", modifier = Modifier.weight(1f), color = Color.Red, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                } else {
                     Text("-", modifier = Modifier.weight(1f), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Divider()

            // Low Band
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("매수 밴드 (-${stock.gValue.toInt()}%)", modifier = Modifier.weight(1f), color = Color.Blue, style = MaterialTheme.typography.bodyMedium)
                Text(formatCurrency(bands.lowValuation, stock.currency), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (order.action == VRCalculator.OrderAction.BUY) {
                     Text("매수 ${order.quantity}주", modifier = Modifier.weight(1f), color = Color.Blue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                } else {
                     Text("-", modifier = Modifier.weight(1f), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Price Table
            Text("수량별 매매가 (현재 ${stock.quantity}주)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha=0.2f)).padding(8.dp)) {
                Text("수량", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("매도가", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Red)
                Text("수량", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("매수가", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Blue)
            }
            Divider()

            priceTable.forEach { row ->
                val sellQty = stock.quantity - row.quantity
                val buyQty = stock.quantity + row.quantity
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 8.dp)) {
                    Text("$sellQty", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium)
                    Text(formatCurrency(row.sellPrice, stock.currency), modifier = Modifier.weight(1.2f), color = Color.Red, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("$buyQty", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium)
                    Text(formatCurrency(row.buyPrice, stock.currency), modifier = Modifier.weight(1.2f), color = Color.Blue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Divider()
            }
        }
    }
}

@Composable
fun StatusItem(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = fontWeight, color = color)
    }
}

@Composable
fun StockHistoryChart(
    historyList: List<StockHistory>,
    modifier: Modifier = Modifier,
    onValueSelected: (StockHistory?) -> Unit = {}
) {
    if (historyList.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("데이터가 부족합니다.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    isGranularityEnabled = true
                    textSize = 10f
                }
                
                axisLeft.apply {
                    setDrawGridLines(true)
                    textSize = 10f
                }
                axisRight.isEnabled = false
                
                legend.isEnabled = true
                legend.textSize = 10f

                setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                        e?.let {
                            val index = it.x.toInt()
                            if (index in historyList.indices) {
                                onValueSelected(historyList[index])
                            }
                        }
                    }
                    override fun onNothingSelected() {
                        onValueSelected(null)
                    }
                })
            }
        },
        update = { chart ->
            val entriesPrincipal = ArrayList<Entry>()
            val entriesTotal = ArrayList<Entry>()
            val entriesCurrentValuation = ArrayList<Entry>() // New Dataset
            val entriesHigh = ArrayList<Entry>()
            val entriesLow = ArrayList<Entry>()
            val dateLabels = ArrayList<String>()
            
            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

            historyList.forEachIndexed { index, item ->
                val x = index.toFloat()
                
                val currentTotal = (item.currentPrice * item.quantity) + item.pool
                val currentValuation = item.currentPrice * item.quantity // Valuation only
                val highBand = item.vValue * (1 + item.gValue / 100)
                val lowBand = item.vValue * (1 - item.gValue / 100)

                entriesPrincipal.add(Entry(x, item.investedPrincipal.toFloat()))
                entriesTotal.add(Entry(x, currentTotal.toFloat()))
                entriesCurrentValuation.add(Entry(x, currentValuation.toFloat()))
                entriesHigh.add(Entry(x, highBand.toFloat()))
                entriesLow.add(Entry(x, lowBand.toFloat()))
                
                dateLabels.add(dateFormat.format(Date(item.timestamp)))
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

            // 1. 모든 데이터셋을 통틀어 전체 Min/Max 계산 (Y축 범위 설정을 위해)
            val allYValues = entriesPrincipal.map { it.y } + entriesTotal.map { it.y } + entriesHigh.map { it.y } + entriesLow.map { it.y } + entriesCurrentValuation.map { it.y }
            val globalMax = allYValues.maxOrNull() ?: 0f
            val globalMin = allYValues.minOrNull() ?: 0f

            // 2. Y축 범위 수동 설정 (여백 10% 추가)
            chart.axisLeft.apply {
                resetAxisMaximum()
                resetAxisMinimum()
                axisMaximum = if (globalMax > 0) globalMax * 1.1f else 1000f
                axisMinimum = if (globalMin > 0) globalMin * 0.9f else 0f
                setLabelCount(5, false)
            }

            // Max/Min 값 찾기 (총잔고 기준 - 라벨 표시용)
            val totalMax = entriesTotal.maxOfOrNull { it.y } ?: 0f
            val totalMin = entriesTotal.minOfOrNull { it.y } ?: 0f
            val firstMaxX = entriesTotal.firstOrNull { it.y == totalMax }?.x ?: -1f
            val firstMinX = entriesTotal.firstOrNull { it.y == totalMin }?.x ?: -1f

            // Custom Formatter
            val customFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    if (entry == null) return ""
                    // 해당 값이 Max/Min이면서 첫 번째 위치인 경우만 표시
                    return if ((entry.y == totalMax && entry.x == firstMaxX) || 
                               (entry.y == totalMin && entry.x == firstMinX)) {
                        java.text.NumberFormat.getIntegerInstance().format(entry.y.toLong())
                    } else {
                        ""
                    }
                }
            }

            val setPrincipal = LineDataSet(entriesPrincipal, "투자원금").apply {
                color = AndroidColor.RED
                setCircleColor(AndroidColor.RED)
                lineWidth = 2f
                circleRadius = 2f
                setDrawCircles(true) // 점 표시
                setDrawCircleHole(false)
                setDrawValues(false)
            }

            val setTotal = LineDataSet(entriesTotal, "총잔고").apply {
                color = AndroidColor.BLUE
                setCircleColor(AndroidColor.BLUE)
                lineWidth = 3f
                circleRadius = 3f
                setDrawCircles(true) // 점 표시
                setDrawCircleHole(false)
                setDrawValues(true)
                valueFormatter = customFormatter 
                valueTextSize = 10f
                valueTypeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val setCurrentValuation = LineDataSet(entriesCurrentValuation, "현재평가액").apply {
                color = AndroidColor.parseColor("#006400") // Dark Green
                setCircleColor(AndroidColor.parseColor("#006400"))
                lineWidth = 2f
                circleRadius = 2f
                setDrawCircles(true)
                setDrawCircleHole(false)
                setDrawValues(false)
            }

            val setHigh = LineDataSet(entriesHigh, "상한").apply {
                color = AndroidColor.parseColor("#4CAF50")
                setCircleColor(AndroidColor.parseColor("#4CAF50"))
                lineWidth = 1f
                circleRadius = 1.5f
                setDrawCircles(false) // 점 표시 제거
                setDrawCircleHole(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
                
                // Fill settings for "Shading between lines" effect (High Band)
                setDrawFilled(true)
                fillColor = AndroidColor.parseColor("#C8E6C9") // Light Green
                fillAlpha = 100
            }

            val setLow = LineDataSet(entriesLow, "하한").apply {
                color = AndroidColor.parseColor("#4CAF50")
                setCircleColor(AndroidColor.parseColor("#4CAF50"))
                lineWidth = 1f
                circleRadius = 1.5f
                setDrawCircles(false) // 점 표시 제거
                setDrawCircleHole(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
                
                // Fill settings for "Shading between lines" effect (Low Band Mask)
                // Fills with White (Background) to mask the High Band's fill below the Low Band
                setDrawFilled(true)
                fillColor = AndroidColor.WHITE 
                fillAlpha = 255
            }

            chart.data = LineData(setHigh, setLow, setPrincipal, setTotal, setCurrentValuation)
            chart.moveViewToX(historyList.size.toFloat())
            chart.invalidate()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePoolDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var isDeposit by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("예수금(Pool) 관리") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = isDeposit,
                        onClick = { isDeposit = true },
                        label = { Text("입금 (증액)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isDeposit,
                        onClick = { isDeposit = false },
                        label = { Text("출금 (감액)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("금액") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isDeposit) "Pool과 총 원금이 증가합니다." else "Pool과 총 원금이 감소합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount > 0) onConfirm(amount, isDeposit)
            }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int) -> Unit
) {
    var type by remember { mutableStateOf("BUY") }
    var priceStr by remember { mutableStateOf(stock.currentPrice.toString()) }
    var qtyStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("거래 기록") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == "BUY",
                        onClick = { type = "BUY" },
                        label = { Text("매수") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFEBEE), selectedLabelColor = Color.Red)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = type == "SELL",
                        onClick = { type = "SELL" },
                        label = { Text("매도") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE3F2FD), selectedLabelColor = Color.Blue)
                    )
                }
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("거래 단가") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("수량") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                val amount = (priceStr.toDoubleOrNull() ?: 0.0) * (qtyStr.toIntOrNull() ?: 0)
                Text(
                    text = "총 거래액: " + formatCurrency(amount, stock.currency),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if(type == "BUY") "Pool이 감소하고 수량이 증가합니다." else "Pool이 증가하고 수량이 감소합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val qty = qtyStr.toIntOrNull() ?: 0
                if (price > 0 && qty > 0) onConfirm(type, price, qty)
            }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecalculateVDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    // 기본값 설정이 있으면 그 값을 초기값으로 사용
    var amountStr by remember { mutableStateOf(if (stock.defaultRecalcAmount > 0) stock.defaultRecalcAmount.toString() else "") }
    var isDeposit by remember { mutableStateOf(true) }

    // 예상 V값 및 Pool 계산
    val additionalAmount = amountStr.toDoubleOrNull() ?: 0.0
    val depositAmount = if (isDeposit) additionalAmount else -additionalAmount
    
    // Pool Formula: NewPool = OldPool + Deposit (NOT reset to 0)
    val expectedNewPool = stock.pool + depositAmount
    
    // V Formula: NewV = OldV + (OldPool / G) + Deposit
    val expectedNewV = VRCalculator.calculateNewV(stock.vValue, stock.pool, stock.gValue, depositAmount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("V값 재산출 (리밸런싱)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "현재 V값: " + formatCurrency(stock.vValue, stock.currency),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "현재 Pool: " + formatCurrency(stock.pool, stock.currency),
                    style = MaterialTheme.typography.bodyMedium
                )

                Divider()

                Text("추가 입출금 (선택사항)", fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = isDeposit,
                        onClick = { isDeposit = true },
                        label = { Text("입금") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isDeposit,
                        onClick = { isDeposit = false },
                        label = { Text("출금") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("금액 (기본값: ${formatCurrency(stock.defaultRecalcAmount, stock.currency)})") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                Text(
                    "재산출 후 예상 Pool: " + formatCurrency(expectedNewPool, stock.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    "재산출 후 예상 V값: " + formatCurrency(expectedNewV, stock.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "※ 기존 Pool과 입출금액이 V값 및 새 Pool에 반영됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                onConfirm(amount, isDeposit)
            }) { Text("V값 재산출") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeGValueDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var gValue by remember { mutableFloatStateOf(stock.gValue.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("G값(기울기) 변경") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "현재 G값: ${stock.gValue.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column {
                    Text("새 G값: ${gValue.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Slider(
                        value = gValue, 
                        onValueChange = { gValue = it }, 
                        valueRange = 1f..50f, 
                        steps = 48
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(gValue.toDouble()) }) { Text("변경") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDefaultRecalcDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountStr by remember { mutableStateOf(if(stock.defaultRecalcAmount > 0) stock.defaultRecalcAmount.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("기본 재산출 금액 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("VR 재산출 시 팝업에 자동으로 입력될 기본 금액을 설정합니다.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("기본 금액 (0이면 설정 안 함)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                onConfirm(amount)
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameStockDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(stock.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("종목명 변경") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("현재 종목명: " + stock.name, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("새 종목명") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newName.isNotBlank()) onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) { Text("변경") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePrincipalDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var principalStr by remember { mutableStateOf(stock.investedPrincipal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("원금 변경") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("현재 원금: " + formatCurrency(stock.investedPrincipal, stock.currency), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                OutlinedTextField(
                    value = principalStr,
                    onValueChange = { principalStr = it },
                    label = { Text("새 원금") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("※ 원금만 변경되며, Pool이나 V값 등은 변경되지 않습니다.", style = MaterialTheme.typography.bodySmall, color = Color.Red)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newPrincipal = principalStr.toDoubleOrNull()
                    if (newPrincipal != null && newPrincipal >= 0) {
                        onConfirm(newPrincipal)
                    }
                }
            ) { Text("변경") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}