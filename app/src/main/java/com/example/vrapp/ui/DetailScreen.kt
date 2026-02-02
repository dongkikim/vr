package com.example.vrapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
    val transactionHistory by viewModel.transactionHistory.collectAsState()

    // Dialog States
    var showPoolDialog by remember { mutableStateOf(false) }
    var showTradeDialog by remember { mutableStateOf(false) }
    var showRecalcDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<com.example.vrapp.model.TransactionHistory?>(null) }
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
                                text = { Text("정보 수정") },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
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
                        stock = s,
                        historyList = filteredHistory,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        onValueSelected = { selectedHistoryItem = it }
                    )
                }

                // 4. 최근 거래 내역 섹션
                if (transactionHistory.isNotEmpty()) {
                    Divider()

                    Text(
                        "최근 거래 내역",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val recentTransactions = transactionHistory.take(10)

                    recentTransactions.forEachIndexed { index, transaction ->
                        val isLatest = index == 0
                        // 최신 거래가 MANUAL_EDIT이면 삭제 불가, 일반 거래면 삭제 가능
                        val canDelete = isLatest && transaction.type != "MANUAL_EDIT" && transaction.canDelete()
                        val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                            .format(Date(transaction.date))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLatest)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when (transaction.type) {
                                                "BUY" -> "매수"
                                                "SELL" -> "매도"
                                                "DEPOSIT" -> "입금"
                                                "DEPOSIT_POOL_ONLY" -> "입금(Pool)"
                                                "WITHDRAW" -> "출금"
                                                "WITHDRAW_POOL_ONLY" -> "출금(Pool)"
                                                "RECALC_V" -> "V재산출"
                                                "MANUAL_EDIT" -> "수동편집"
                                                else -> transaction.type
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = when (transaction.type) {
                                                "BUY" -> Color.Red
                                                "SELL" -> Color.Blue
                                                "DEPOSIT", "DEPOSIT_POOL_ONLY" -> Color(0xFF4CAF50)
                                                "WITHDRAW", "WITHDRAW_POOL_ONLY" -> Color(0xFFFF9800)
                                                "RECALC_V" -> Color(0xFF9C27B0)
                                                "MANUAL_EDIT" -> Color(0xFF795548)
                                                else -> Color.Unspecified
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }

                                    if (transaction.type == "BUY" || transaction.type == "SELL") {
                                        Text(
                                            "${formatQuantity(transaction.quantity, s.ticker)}주 × ${formatCurrency(transaction.price, s.currency)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (transaction.amount > 0) {
                                        Column {
                                            Text(
                                                "금액: ${formatCurrency(transaction.amount, s.currency)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            // 원금 추가 정보 표시
                                            if (transaction.type == "BUY" && transaction.amount > transaction.previousPool) {
                                                val addedPrincipal = transaction.amount - transaction.previousPool
                                                Text(
                                                    "(원금추가: ${formatCurrency(addedPrincipal, s.currency)})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFE65100)
                                                )
                                            }
                                        }
                                    }

                                    if (transaction.type == "RECALC_V" && transaction.previousV != null && transaction.newV != null) {
                                        Text(
                                            "V: ${formatCurrency(transaction.previousV, s.currency)} → ${formatCurrency(transaction.newV, s.currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF9C27B0)
                                        )
                                    }
                                }

                                // 삭제 버튼 (가장 최근 거래만, MANUAL_EDIT 제외)
                                if (canDelete) {
                                    IconButton(
                                        onClick = {
                                            transactionToDelete = transaction
                                            showDeleteConfirmDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "삭제",
                                            tint = Color.Red
                                        )
                                    }
                                } else if (isLatest) {
                                    Text(
                                        if (transaction.type == "MANUAL_EDIT") "편집기록" else "삭제불가",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
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
            onConfirm = { amount, isDeposit, applyToPrincipal ->
                viewModel.updatePool(stock!!, amount, isDeposit, applyToPrincipal)
                showPoolDialog = false
            }
        )
    }

    if (showTradeDialog && stock != null) {
        TradeDialog(
            stock = stock!!,
            onDismiss = { showTradeDialog = false },
            onConfirm = { type, price, qty, usePrincipal ->
                viewModel.executeTransaction(stock!!, type, price, qty, usePrincipal)
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

    if (showEditDialog && stock != null) {
        EditStockDialog(
            stock = stock!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, gValue, pool, quantity, principal, startDate, defaultRecalc, isVr ->
                viewModel.updateStockInfo(stock!!, name, gValue, pool, quantity, principal, startDate, defaultRecalc, isVr)
                showEditDialog = false
            }
        )
    }

    // 거래 삭제 확인 다이얼로그
    if (showDeleteConfirmDialog && transactionToDelete != null && stock != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                transactionToDelete = null
            },
            title = { Text("거래 내역 삭제") },
            text = {
                Column {
                    Text("이 거래를 삭제하고 이전 상태로 되돌리시겠습니까?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "거래 유형: ${when (transactionToDelete!!.type) {
                            "BUY" -> "매수"
                            "SELL" -> "매도"
                            "DEPOSIT" -> "입금"
                            "DEPOSIT_POOL_ONLY" -> "입금(Pool)"
                            "WITHDRAW" -> "출금"
                            "WITHDRAW_POOL_ONLY" -> "출금(Pool)"
                            "RECALC_V" -> "V재산출"
                            "MANUAL_EDIT" -> "수동편집"
                            else -> transactionToDelete!!.type
                        }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (transactionToDelete!!.type == "RECALC_V") {
                        Text(
                            "V값이 ${formatCurrency(transactionToDelete!!.previousV ?: 0.0, stock!!.currency)}로 복원됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLatestTransaction(stock!!, transactionToDelete!!)
                        showDeleteConfirmDialog = false
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    transactionToDelete = null
                }) { Text("취소") }
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
        currentPool = stock.pool,
        bands = bands, 
        range = 30, 
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
                     Text("매도 ${formatQuantity(order.quantity, stock.ticker)}주", modifier = Modifier.weight(1f), color = Color.Red, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
                     Text("매수 ${formatQuantity(order.quantity, stock.ticker)}주", modifier = Modifier.weight(1f), color = Color.Blue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                } else {
                     Text("-", modifier = Modifier.weight(1f), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Price Table
            Text("수량별 매매가 (현재 ${formatQuantity(stock.quantity, stock.ticker)}주)", style = MaterialTheme.typography.titleMedium)
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
                    // 매도 수량 / 가격
                    Text(formatQuantity(sellQty, stock.ticker), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium)
                    if (row.sellPrice > 0) {
                        Text(formatCurrency(row.sellPrice, stock.currency), modifier = Modifier.weight(1.2f), color = Color.Red, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("-", modifier = Modifier.weight(1.2f), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }

                    // 매수 수량 / 가격
                    Text(formatQuantity(buyQty, stock.ticker), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium)
                    if (row.buyPrice > 0) {
                        Text(formatCurrency(row.buyPrice, stock.currency), modifier = Modifier.weight(1.2f), color = Color.Blue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("-", modifier = Modifier.weight(1.2f), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
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
    stock: Stock,
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

    // 현재 stock 기준 상한/하한 (수평선)
    val currentHighBand = (stock.vValue * (1 + stock.gValue / 100)).toFloat()
    val currentLowBand = (stock.vValue * (1 - stock.gValue / 100)).toFloat()

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
            val entriesCurrentValuation = ArrayList<Entry>()
            val entriesHigh = ArrayList<Entry>()
            val entriesLow = ArrayList<Entry>()
            val dateLabels = ArrayList<String>()

            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

            historyList.forEachIndexed { index, item ->
                val x = index.toFloat()

                val currentTotal = (item.currentPrice * item.quantity) + item.pool
                val currentValuation = item.currentPrice * item.quantity

                entriesPrincipal.add(Entry(x, item.investedPrincipal.toFloat()))
                entriesTotal.add(Entry(x, currentTotal.toFloat()))
                entriesCurrentValuation.add(Entry(x, currentValuation.toFloat()))
                // 상한/하한은 현재 stock 기준 수평선
                entriesHigh.add(Entry(x, currentHighBand))
                entriesLow.add(Entry(x, currentLowBand))

                dateLabels.add(dateFormat.format(Date(item.timestamp)))
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

            // Y축 범위: 상한 or 투자원금/총잔고/평가액 중 높은 값 기준
            val dataYValues = entriesPrincipal.map { it.y } + entriesTotal.map { it.y } + entriesCurrentValuation.map { it.y }
            val dataMax = dataYValues.maxOrNull() ?: 0f
            val dataMin = dataYValues.minOrNull() ?: 0f
            val globalMax = maxOf(dataMax, currentHighBand)
            val globalMin = minOf(dataMin, currentLowBand)

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

            val customFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    if (entry == null) return ""
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
                setDrawCircles(true)
                setDrawCircleHole(false)
                setDrawValues(false)
            }

            val setTotal = LineDataSet(entriesTotal, "총잔고").apply {
                color = AndroidColor.BLUE
                setCircleColor(AndroidColor.BLUE)
                lineWidth = 3f
                circleRadius = 3f
                setDrawCircles(true)
                setDrawCircleHole(false)
                setDrawValues(true)
                valueFormatter = customFormatter
                valueTextSize = 10f
                valueTypeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val setCurrentValuation = LineDataSet(entriesCurrentValuation, "평가액").apply {
                color = AndroidColor.parseColor("#006400")
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
                setDrawCircles(false)
                setDrawCircleHole(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)

                setDrawFilled(true)
                fillColor = AndroidColor.parseColor("#C8E6C9")
                fillAlpha = 100
            }

            val setLow = LineDataSet(entriesLow, "하한").apply {
                color = AndroidColor.parseColor("#4CAF50")
                setCircleColor(AndroidColor.parseColor("#4CAF50"))
                lineWidth = 1f
                circleRadius = 1.5f
                setDrawCircles(false)
                setDrawCircleHole(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)

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
    onConfirm: (Double, Boolean, Boolean) -> Unit // amount, isDeposit, applyToPrincipal
) {
    var amountStr by remember { mutableStateOf("") }
    var isDeposit by remember { mutableStateOf(true) }
    var applyToPrincipal by remember { mutableStateOf(true) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyToPrincipal,
                        onCheckedChange = { applyToPrincipal = it }
                    )
                    Text(
                        text = "원금에도 적용",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = if (applyToPrincipal) {
                        if (isDeposit) "Pool과 원금이 증가합니다." else "Pool과 원금이 감소합니다."
                    } else {
                        if (isDeposit) "Pool만 증가합니다. (원금 변동 없음)" else "Pool만 감소합니다. (원금 변동 없음)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount > 0) onConfirm(amount, isDeposit, applyToPrincipal)
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
    onConfirm: (String, Double, Double, Boolean) -> Unit
) {
    var type by remember { mutableStateOf("BUY") }
    var priceStr by remember { mutableStateOf(stock.currentPrice.toString()) }
    var qtyStr by remember { mutableStateOf("") }
    var usePrincipal by remember { mutableStateOf(false) } // 원금 사용 여부

    val price = priceStr.toDoubleOrNull() ?: 0.0
    val qty = qtyStr.toDoubleOrNull() ?: 0.0
    val amount = price * qty

    // 유효성 검사
    val isPriceValid = price > 0
    val isQtyValid = qty > 0
    // 매수: 원금사용 체크 시 Pool 한도 무시, 미체크 시 Pool 한도 적용
    val isBuyValid = type != "BUY" || (usePrincipal || amount <= stock.pool)
    val isSellValid = type != "SELL" || qty <= stock.quantity
    val isValid = isPriceValid && isQtyValid && isBuyValid && isSellValid

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
                
                // 원금 체크박스 (매수일 때만 표시)
                if (type == "BUY") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = usePrincipal,
                            onCheckedChange = { usePrincipal = it }
                        )
                        Text("부족 시 원금에서 충당", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("거래 단가") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = priceStr.isNotEmpty() && !isPriceValid,
                    supportingText = if (priceStr.isNotEmpty() && !isPriceValid) {
                        { Text("0보다 큰 값을 입력하세요") }
                    } else null
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { input ->
                        val filtered = if (!isCoin(stock.ticker)) {
                            input.filter { it.isDigit() }
                        } else {
                            input.filter { it.isDigit() || it == '.' }
                        }
                        qtyStr = filtered
                    },
                    label = { Text("수량") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = qtyStr.isNotEmpty() && (!isQtyValid || !isSellValid),
                    supportingText = when {
                        qtyStr.isNotEmpty() && !isQtyValid -> {{ Text("0보다 큰 값을 입력하세요") }}
                        qtyStr.isNotEmpty() && !isSellValid -> {{ Text("보유 수량(${formatQuantity(stock.quantity, stock.ticker)})을 초과할 수 없습니다") }}
                        else -> null
                    }
                )

                Text(
                    text = "총 거래액: " + formatCurrency(amount, stock.currency),
                    fontWeight = FontWeight.Bold
                )

                // 현재 Pool 표시 및 매수 시 검증 메시지
                Text(
                    text = "현재 Pool: " + formatCurrency(stock.pool, stock.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                if (type == "BUY") {
                    if (amount > stock.pool) {
                        if (usePrincipal) {
                             val shortage = amount - stock.pool
                             Text(
                                text = "Pool 소진 후 원금 ${formatCurrency(shortage, stock.currency)} 추가됨",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100) // Orange
                            )
                        } else {
                            Text(
                                text = "Pool이 부족합니다 (필요: ${formatCurrency(amount, stock.currency)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }
                    } else {
                        Text(
                            text = "Pool이 감소하고 수량이 증가합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                } else {
                     Text(
                        text = "Pool이 증가하고 수량이 감소합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(type, price, qty, usePrincipal) },
                enabled = isValid
            ) { Text("확인") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStockDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Double, Double, Long, Double, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(stock.name) }
    var gValueStr by remember { mutableStateOf(stock.gValue.toString()) }
    var poolStr by remember { mutableStateOf(stock.pool.toString()) }
    var quantityStr by remember { mutableStateOf(stock.quantity.toString()) }
    var principalStr by remember { mutableStateOf(stock.investedPrincipal.toString()) }
    var startDate by remember { mutableStateOf(stock.startDate) }
    var defaultRecalcStr by remember { mutableStateOf(stock.defaultRecalcAmount.toString()) }
    var isVr by remember { mutableStateOf(stock.isVr) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showWarningConfirm by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (startDate > 0) startDate else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis ?: startDate
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "정보 수정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // V값 표시 (수정 불가)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("V값 (수정불가)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatCurrency(stock.vValue, stock.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Divider()

                // 종목명
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("종목명") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // G값
                OutlinedTextField(
                    value = gValueStr,
                    onValueChange = { gValueStr = it },
                    label = { Text("G값 (%)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Pool
                OutlinedTextField(
                    value = poolStr,
                    onValueChange = { poolStr = it },
                    label = { Text("Pool (예수금)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 보유 수량
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { input ->
                        val filtered = if (!isCoin(stock.ticker)) {
                            input.filter { it.isDigit() }
                        } else {
                            input.filter { it.isDigit() || it == '.' }
                        }
                        quantityStr = filtered
                    },
                    label = { Text("보유 수량") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 투자 원금
                OutlinedTextField(
                    value = principalStr,
                    onValueChange = { principalStr = it },
                    label = { Text("투자 원금") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 투자 시작일
                OutlinedTextField(
                    value = if (startDate > 0) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startDate)) else "",
                    onValueChange = { },
                    label = { Text("투자 시작일") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Text("선택", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )

                // 기본 입출금액
                OutlinedTextField(
                    value = defaultRecalcStr,
                    onValueChange = { defaultRecalcStr = it },
                    label = { Text("기본 입출금액 (V값 재산출시)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // VR 관리 여부 스위치
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("VR 투자 관리", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isVr,
                        onCheckedChange = { isVr = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 경고 메시지
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚠️ 수동 편집 후에는 모든 거래 내역 삭제가 불가능합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("취소") }

                    Button(
                        onClick = { showWarningConfirm = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("저장") }
                }
            }
        }
    }

    // 최종 확인 다이얼로그
    if (showWarningConfirm) {
        AlertDialog(
            onDismissRequest = { showWarningConfirm = false },
            title = { Text("수동 편집 확인") },
            text = {
                Text("수동 편집을 진행하면 이후 모든 거래 내역 삭제가 불가능합니다.\n\n정말 진행하시겠습니까?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWarningConfirm = false
                        val gValue = gValueStr.toDoubleOrNull() ?: stock.gValue
                        val pool = poolStr.toDoubleOrNull() ?: stock.pool
                        val quantity = quantityStr.toDoubleOrNull() ?: stock.quantity
                        val principal = principalStr.toDoubleOrNull() ?: stock.investedPrincipal
                        val defaultRecalc = defaultRecalcStr.toDoubleOrNull() ?: stock.defaultRecalcAmount

                        if (name.isNotBlank()) {
                            onConfirm(name, gValue, pool, quantity, principal, startDate, defaultRecalc, isVr)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) { Text("진행") }
            },
            dismissButton = {
                TextButton(onClick = { showWarningConfirm = false }) { Text("취소") }
            }
        )
    }
}