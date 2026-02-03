package com.example.vrapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.vrapp.data.StockPriceService
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vrapp.logic.VRCalculator
import com.example.vrapp.model.Stock
import com.example.vrapp.viewmodel.StockViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 유틸리티 포맷 함수
fun formatCurrency(amount: Double, currency: String): String {
    val locale = when (currency) {
        "USD" -> Locale.US
        "JPY" -> Locale.JAPAN
        else -> Locale.KOREA
    }
    val format = NumberFormat.getCurrencyInstance(locale)
    if (currency == "KRW" || currency == "JPY") {
        format.maximumFractionDigits = 0
    } else {
        format.maximumFractionDigits = 2
    }
    return format.format(amount)
}

fun isCoin(ticker: String): Boolean {
    return ticker.endsWith(".bithumb")
}

fun formatQuantity(quantity: Double, ticker: String): String {
    return if (isCoin(ticker)) {
        String.format("%.6f", quantity).trimEnd('0').trimEnd('.')
    } else {
        String.format("%.0f", quantity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: StockViewModel,
    onStockClick: (Long) -> Unit,
    onChartClick: () -> Unit
) {
    // 기존 allStocks 대신 필터/정렬된 리스트 구독
    val stocks by viewModel.displayStocks.collectAsState()
    val assetStatus by viewModel.assetStatus.collectAsState()
    val yesterdayAssetStatus by viewModel.yesterdayAssetStatus.collectAsState()
    val yesterdayStockValuations by viewModel.yesterdayStockValuations.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // 필터/정렬 상태 구독
    val currentFilter by viewModel.filterOption.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var stockToDelete by remember { mutableStateOf<Stock?>(null) }
    var showSortMenu by remember { mutableStateOf(false) } // 정렬 메뉴 표시 여부

    Scaffold(
        floatingActionButton = {
           // FAB Removed (Moved to Header)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            // ... (Header Box) ... (Skipping Header Box Content for brevity in replace, keeping existing)
            // 1. 상단 Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // ... (Existing Header Content Code) ...
                val totalProfitLoss = assetStatus.totalCurrent - assetStatus.totalPrincipal
                val assetDiff = if (yesterdayAssetStatus != null) {
                    assetStatus.totalCurrent - yesterdayAssetStatus!!.totalCurrent
                } else {
                    0.0
                }
                val assetDiffPercent = if (yesterdayAssetStatus != null && yesterdayAssetStatus!!.totalCurrent > 0) {
                    (assetDiff / yesterdayAssetStatus!!.totalCurrent) * 100
                } else {
                    0.0
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Row 1: 전일대비(자산) | 종목추가버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val diffText = if (assetDiff >= 0) "+${formatCurrency(assetDiff, "KRW")}" else formatCurrency(assetDiff, "KRW")
                        val diffPercentText = if (yesterdayAssetStatus != null && yesterdayAssetStatus!!.totalCurrent > 0) {
                            String.format("(%+.1f%%)", assetDiffPercent)
                        } else ""

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("전일대비(자산) : ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "$diffText $diffPercentText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (assetDiff >= 0) Color.Red else Color.Blue
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 새로고침 버튼
                            Surface(
                                onClick = { viewModel.refreshAllPrices() },
                                color = if (isRefreshing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondary,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "새로고침",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                            // 종목 추가 버튼
                            Surface(
                                onClick = { showAddDialog = true },
                                color = MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "종목 추가",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: 총원금(손익) | 총자산(수익률)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: 총원금(손익)
                        Column(modifier = Modifier.weight(1f)) {
                             Text("총원금(손익)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                             Row(verticalAlignment = Alignment.Bottom) {
                                 Text(
                                     formatCurrency(assetStatus.totalPrincipal, "KRW"),
                                     style = MaterialTheme.typography.bodyMedium,
                                     fontWeight = FontWeight.Bold,
                                     color = MaterialTheme.colorScheme.onPrimaryContainer
                                 )
                                 Text(
                                     "(${if (totalProfitLoss >= 0) "+" else ""}${formatCurrency(totalProfitLoss, "KRW")})",
                                     style = MaterialTheme.typography.bodySmall,
                                     color = if (totalProfitLoss >= 0) Color.Red else Color.Blue,
                                     maxLines = 1
                                 )
                             }
                        }
                        
                        // Right: 총자산(수익률)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                             Text("총자산(수익률)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                             Row(verticalAlignment = Alignment.Bottom) {
                                 Text(
                                     formatCurrency(assetStatus.totalCurrent, "KRW"),
                                     style = MaterialTheme.typography.bodyMedium,
                                     fontWeight = FontWeight.Bold,
                                     color = MaterialTheme.colorScheme.onPrimaryContainer
                                 )
                                 Text(
                                     "(${String.format("%+.1f", assetStatus.totalROI)}%)",
                                     style = MaterialTheme.typography.bodySmall,
                                     color = if (assetStatus.totalROI >= 0) Color.Red else Color.Blue,
                                     maxLines = 1
                                 )
                             }
                        }
                    }
                }
            }

            // 2. 필터 및 정렬 바 (신규 추가)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 필터 칩 리스트
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(StockViewModel.StockFilter.entries) { filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label) },
                            leadingIcon = if (currentFilter == filter) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // 정렬 버튼
                Box {
                    TextButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "정렬")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(currentSort.label, style = MaterialTheme.typography.bodySmall)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        StockViewModel.StockSort.entries.forEach { sortOption ->
                            DropdownMenuItem(
                                text = { Text(sortOption.label) },
                                onClick = {
                                    viewModel.setSort(sortOption)
                                    showSortMenu = false
                                },
                                trailingIcon = if (currentSort == sortOption) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }
            
            Divider()

            // 3. 중단 Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (stocks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("표시할 종목이 없습니다.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stocks, key = { it.id }) { stock ->
                            val yesterdayVal = yesterdayStockValuations[stock.id]
                            StockCard(
                                stock = stock,
                                yesterdayValuation = yesterdayVal,
                                onClick = { onStockClick(stock.id) },
                                onLongClick = { stockToDelete = stock }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, ticker, v, g, pool, qty, price, currency, principal, startDate ->
                viewModel.addStock(name, ticker, v, g, pool, qty, price, currency, principal, startDate)
                showAddDialog = false
            }
        )
    }

    // 삭제 확인 다이얼로그
    stockToDelete?.let { stock ->
        AlertDialog(
            onDismissRequest = { stockToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
            title = { Text("종목 삭제") },
            text = { Text("'${stock.name}' 종목을 삭제하시겠습니까?\n\n삭제된 데이터는 복구할 수 없습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStock(stock)
                        stockToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { stockToDelete = null }) { Text("취소") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockCard(stock: Stock, yesterdayValuation: Double? = null, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val currentValuation = stock.currentPrice * stock.quantity
    val currentTotal = currentValuation + stock.pool
    val profitLoss = currentTotal - stock.investedPrincipal
    val roi = if (stock.investedPrincipal > 0) (profitLoss / stock.investedPrincipal) * 100 else 0.0
    
    // Calculate Yesterday Diff (Asset)
    val diffFromYesterday = if (yesterdayValuation != null) {
        currentTotal - yesterdayValuation
    } else {
        0.0 
    }

    // Calculate VR Order for Background Color
    val bands = VRCalculator.calculateBands(stock.vValue, stock.gValue)
    val order = VRCalculator.calculateOrder(currentValuation, stock.currentPrice, bands)
    
    val cardColor = when {
        !stock.isVr -> MaterialTheme.colorScheme.surface // VR 아님: 기본색
        order.action == VRCalculator.OrderAction.BUY -> Color(0xFFFFEBEE)   // 엷은 붉은색
        order.action == VRCalculator.OrderAction.SELL -> Color(0xFFE3F2FD)  // 옅은 파란색
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box { // Box to place VR tag
            Column(modifier = Modifier.padding(16.dp)) {
                // 1줄: 종목명|VR태그 --------- 현재가
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.name, 
                            style = MaterialTheme.typography.bodyMedium, 
                            fontWeight = FontWeight.Bold
                        )
                        if (stock.isVr) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "VR",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = formatCurrency(stock.currentPrice, stock.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 2줄: 전일대비변동(금액,%) --------- 총손익금액(금액,%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: 전일대비변동
                    if (yesterdayValuation != null) {
                        val diffText = if (diffFromYesterday >= 0) "+${formatCurrency(diffFromYesterday, stock.currency)}" else formatCurrency(diffFromYesterday, stock.currency)
                        val diffPercent = if (yesterdayValuation > 0) (diffFromYesterday / yesterdayValuation) * 100 else 0.0
                        Text(
                            text = "$diffText (${String.format("%+.1f", diffPercent)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (diffFromYesterday >= 0) Color.Red else Color.Blue
                        )
                    } else {
                        Text("-", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    // Right: 총손익금액
                    Text(
                        text = "${formatCurrency(profitLoss, stock.currency)} (${String.format("%+.1f", roi)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (profitLoss >= 0) Color.Red else Color.Blue
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3줄: 투자원금 --------- 총자산
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "투자원금 : ${formatCurrency(stock.investedPrincipal, stock.currency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "총자산 : ${formatCurrency(currentTotal, stock.currency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 4줄: 현재평가액 --------- Pool
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "현재평가액 : ${formatCurrency(currentValuation, stock.currency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Pool : ${formatCurrency(stock.pool, stock.currency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
    } // End of Box
    }
}

// ... (existing imports)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockDialog(
    viewModel: StockViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Double, Double, Double, String, Double?, Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 종목 정보
    var tickerInput by remember { mutableStateOf("") }
    var stockName by remember { mutableStateOf("") }
    var selectedMarket by remember { mutableStateOf(StockPriceService.Market.KOSPI) }
    var selectedCurrency by remember { mutableStateOf("KRW") }
    var priceStr by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 투자 정보
    var vValueStr by remember { mutableStateOf("") }
    var gValue by remember { mutableFloatStateOf(10f) }
    var poolStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var principalStr by remember { mutableStateOf("") }
    
    // 시작일 정보
    var startDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
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

    // 종목 조회 함수
    fun searchStock() {
        if (tickerInput.isBlank() || isLoading) return
        isLoading = true
        coroutineScope.launch {
            val stockInfo = viewModel.fetchStockInfo(tickerInput, selectedMarket)
            if (stockInfo != null) {
                stockName = stockInfo.name
                priceStr = stockInfo.price.toString()
                selectedCurrency = stockInfo.currency
            } else {
                Toast.makeText(context, "종목을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                stockName = ""
                priceStr = ""
            }
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 종목 추가") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 증시 선택
                Text("증시 선택", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StockPriceService.Market.entries.forEach { market ->
                        FilterChip(
                            selected = selectedMarket == market,
                            onClick = {
                                selectedMarket = market
                                selectedCurrency = market.currency
                                // 금인 경우 티커 자동 입력, 그 외에는 초기화
                                if (market == StockPriceService.Market.GOLD) {
                                    tickerInput = "GOLD"
                                } else if (tickerInput == "GOLD") {
                                    tickerInput = ""
                                }
                                // 증시 변경 시 기존 조회 결과 초기화
                                stockName = ""
                                priceStr = ""
                            },
                            label = { Text(market.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                // 티커 입력 + 조회 버튼
                OutlinedTextField(
                    value = tickerInput,
                    onValueChange = { if (selectedMarket != StockPriceService.Market.GOLD) tickerInput = it.trim().uppercase() },
                    label = { Text(if (selectedMarket == StockPriceService.Market.GOLD) "종목 (고정)" else "종목 티커") },
                    readOnly = selectedMarket == StockPriceService.Market.GOLD,
                    placeholder = {
                        Text(
                            when (selectedMarket) {
                                StockPriceService.Market.KOSPI, StockPriceService.Market.KOSDAQ -> "예: 005930"
                                StockPriceService.Market.US -> "예: AAPL"
                                StockPriceService.Market.JAPAN -> "예: 7203"
                                StockPriceService.Market.COIN -> "예: BTC"
                                StockPriceService.Market.GOLD -> "한국금거래소 시세 적용"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { searchStock() },
                            enabled = tickerInput.isNotBlank() && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "시세 조회")
                            }
                        }
                    }
                )

                // 종목명 표시 (조회 결과)
                if (stockName.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("종목명 : $stockName", fontWeight = FontWeight.Bold)
                            Text("현재가 : ${formatCurrency(priceStr.toDoubleOrNull() ?: 0.0, selectedCurrency)} ($selectedCurrency)")
                        }
                    }
                }

                Divider()

                // 투자 정보 입력
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { input ->
                            // 코인이 아니면 숫자만, 코인이면 소수점 허용
                            val filtered = if (selectedMarket != StockPriceService.Market.COIN) {
                                input.filter { it.isDigit() }
                            } else {
                                input.filter { it.isDigit() || it == '.' }
                            }
                            qtyStr = filtered
                        },
                        label = { Text("수량") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = poolStr,
                        onValueChange = { poolStr = it },
                        label = { Text("Pool") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = principalStr,
                    onValueChange = { principalStr = it },
                    label = { Text("초기 원금 (비워두면 자동계산)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("G값: ${gValue.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Slider(value = gValue, onValueChange = { gValue = it }, valueRange = 1f..20f, steps = 18)
                }

                OutlinedTextField(
                    value = vValueStr,
                    onValueChange = { vValueStr = it },
                    label = { Text("초기 V값 (비워두면 자동계산)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 시작일 선택 버튼
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Date(
                        startDateMillis
                    )
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("투자 시작일: $dateStr")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val pool = poolStr.toDoubleOrNull() ?: 0.0
                    val v = vValueStr.toDoubleOrNull() ?: (price * qty)
                    val principal = principalStr.toDoubleOrNull()
                    val name = stockName.ifBlank { tickerInput }
                    // 티커에 증시 suffix 포함하여 저장
                    val fullTicker = tickerInput + selectedMarket.suffix
                    if (tickerInput.isNotBlank() && price > 0) {
                        onConfirm(name, fullTicker, v, gValue.toDouble(), pool, qty, price, selectedCurrency, principal, startDateMillis)
                    } else {
                        Toast.makeText(context, "종목을 먼저 조회해주세요", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = stockName.isNotBlank() && priceStr.isNotBlank()
            ) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
