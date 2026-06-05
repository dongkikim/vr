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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vrapp.data.StockPriceService
import com.example.vrapp.logic.VRCalculator
import com.example.vrapp.model.IbStock
import com.example.vrapp.model.Stock
import com.example.vrapp.viewmodel.IbViewModel
import com.example.vrapp.viewmodel.StockViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 통합 리스트 아이템 인터페이스 (정렬 및 렌더링용)
sealed class UnifiedStockItem {
    abstract val id: Long
    abstract val name: String
    abstract val ticker: String
    abstract val currentPrice: Double
    abstract val quantity: Double
    abstract val pool: Double
    abstract val principal: Double
    abstract val currency: String

    data class VrItem(val stock: Stock) : UnifiedStockItem() {
        override val id = stock.id
        override val name = stock.name
        override val ticker = stock.ticker
        override val currentPrice = stock.currentPrice
        override val quantity = stock.quantity
        override val pool = stock.pool
        override val principal = stock.investedPrincipal
        override val currency = stock.currency
    }

    data class IbItem(val stock: IbStock) : UnifiedStockItem() {
        override val id = stock.id
        override val name = stock.name
        override val ticker = stock.ticker
        override val currentPrice = stock.currentPrice
        override val quantity = stock.quantity
        override val pool = stock.pool
        override val principal = stock.principal
        override val currency = stock.currency
    }

    fun getTotalAsset() = (currentPrice * quantity) + pool
    fun getProfit() = getTotalAsset() - principal
    fun getRoi() = if (principal > 0) (getProfit() / principal) * 100 else 0.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    stockViewModel: StockViewModel,
    ibViewModel: IbViewModel,
    onStockClick: (Long) -> Unit,
    onIbStockClick: (Long) -> Unit,
    onChartClick: () -> Unit
) {
    val vrStocks by stockViewModel.allStocks.collectAsState()
    val ibStocks by ibViewModel.allIbStocks.collectAsState()
    val ibAccounts by ibViewModel.allIbAccounts.collectAsState()
    val assetStatus by stockViewModel.assetStatus.collectAsState()
    val yesterdayAssetStatus by stockViewModel.yesterdayAssetStatus.collectAsState()
    val yesterdayStockValuations by stockViewModel.yesterdayStockValuations.collectAsState()
    val isRefreshingVr by stockViewModel.isRefreshing.collectAsState()
    val isRefreshingIb by ibViewModel.isRefreshing.collectAsState()
    
    val currentFilter by stockViewModel.filterOption.collectAsState()
    val currentSort by stockViewModel.sortOption.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var stockToDelete by remember { mutableStateOf<UnifiedStockItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }

    // 통합 리스트 구성 및 필터/정렬 적용
    val combinedList = remember(vrStocks, ibStocks, currentFilter, currentSort) {
        val list = mutableListOf<UnifiedStockItem>()
        if (currentFilter == StockViewModel.StockFilter.ALL || currentFilter == StockViewModel.StockFilter.VR || currentFilter == StockViewModel.StockFilter.NON_VR) {
            val filteredVr = when(currentFilter) {
                StockViewModel.StockFilter.VR -> vrStocks.filter { it.isVr }
                StockViewModel.StockFilter.NON_VR -> vrStocks.filter { !it.isVr }
                else -> vrStocks
            }
            list.addAll(filteredVr.map { UnifiedStockItem.VrItem(it) })
        }
        if (currentFilter == StockViewModel.StockFilter.ALL || currentFilter == StockViewModel.StockFilter.IB) {
            list.addAll(ibStocks.map { UnifiedStockItem.IbItem(it) })
        }

        // 정렬 적용 (환율 무관하게 퍼센트는 그대로, 금액은 대략적 비교)
        // 실제 환율 정밀 정렬은 ViewModel 내부 displayStocks 로직 참고하나 여기서는 단순화
        when(currentSort) {
            StockViewModel.StockSort.TOTAL_ASSET -> list.sortByDescending { it.getTotalAsset() }
            StockViewModel.StockSort.PROFIT_PCT -> list.sortByDescending { it.getRoi() }
            StockViewModel.StockSort.PROFIT_AMT -> list.sortByDescending { it.getProfit() }
            else -> list.sortByDescending { it.getTotalAsset() }
        }
        list
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 1. 헤더 (기존 자산 요약)
            AssetSummaryHeader(assetStatus, yesterdayAssetStatus, isRefreshingVr || isRefreshingIb) {
                stockViewModel.refreshAllPrices()
                ibViewModel.refreshPrices()
            }

            // 2. 필터 바
            FilterAndSortBar(
                currentFilter = currentFilter,
                currentSort = currentSort,
                onFilterChange = { stockViewModel.setFilter(it) },
                onSortChange = { stockViewModel.setSort(it) },
                onAddClick = { showAddDialog = true }
            )

            // 3. 무한매수 지갑 대시보드 (IB 필터 시에만 노출)
            if (currentFilter == StockViewModel.StockFilter.IB) {
                IbWalletDashboard(ibAccounts, assetStatus) { showWalletDialog = true }
            }

            Divider()

            // 4. 종목 리스트
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                if (combinedList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("표시할 종목이 없습니다.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(combinedList, key = { "${it.javaClass.simpleName}_${it.id}" }) { item ->
                            when(item) {
                                is UnifiedStockItem.VrItem -> {
                                    StockCard(
                                        stock = item.stock,
                                        yesterdayValuation = yesterdayStockValuations[item.id],
                                        onClick = { onStockClick(item.id) },
                                        onLongClick = { stockToDelete = item }
                                    )
                                }
                                is UnifiedStockItem.IbItem -> {
                                    IbStockCard(
                                        stock = item.stock,
                                        onClick = { onIbStockClick(item.id) },
                                        onLongClick = { stockToDelete = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 다이얼로그들
    if (showAddDialog) {
        AddStockDialog(
            stockViewModel = stockViewModel,
            ibViewModel = ibViewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    if (showWalletDialog) {
        IbWalletDialog(
            viewModel = ibViewModel,
            stockViewModel = stockViewModel,
            onDismiss = { showWalletDialog = false }
        )
    }

    stockToDelete?.let { item ->
        DeleteConfirmDialog(
            itemName = item.name,
            onDismiss = { stockToDelete = null },
            onConfirm = {
                when(item) {
                    is UnifiedStockItem.VrItem -> stockViewModel.deleteStock(item.stock)
                    is UnifiedStockItem.IbItem -> ibViewModel.deleteIbStock(item.stock)
                }
                stockToDelete = null
            }
        )
    }
}

@Composable
fun AssetSummaryHeader(
    assetStatus: StockViewModel.AssetStatus,
    yesterdayAssetStatus: StockViewModel.AssetStatus?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val totalProfitLoss = assetStatus.totalCurrent - assetStatus.totalPrincipal
    val assetDiff = yesterdayAssetStatus?.let { assetStatus.totalCurrent - it.totalCurrent } ?: 0.0
    val assetDiffPercent = if (yesterdayAssetStatus != null && yesterdayAssetStatus.totalCurrent > 0) (assetDiff / yesterdayAssetStatus.totalCurrent) * 100 else 0.0

    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("전일대비(자산) : ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${if (assetDiff >= 0) "+" else ""}${formatCurrency(assetDiff, "KRW")} (${String.format("%+.1f%%", assetDiffPercent)})",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                        color = if (assetDiff >= 0) Color.Red else Color.Blue
                    )
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = if (isRefreshing) Color.Gray else MaterialTheme.colorScheme.primary)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("총원금(손익)", style = MaterialTheme.typography.labelSmall)
                    Text("${formatCurrency(assetStatus.totalPrincipal, "KRW")} (${if (totalProfitLoss >= 0) "+" else ""}${formatCurrency(totalProfitLoss, "KRW")})",
                        color = if (totalProfitLoss >= 0) Color.Red else Color.Blue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("총자산(수익률)", style = MaterialTheme.typography.labelSmall)
                    Text("${formatCurrency(assetStatus.totalCurrent, "KRW")} (${String.format("%+.1f%%", assetStatus.totalROI)})",
                        color = if (assetStatus.totalROI >= 0) Color.Red else Color.Blue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterAndSortBar(
    currentFilter: StockViewModel.StockFilter,
    currentSort: StockViewModel.StockSort,
    onFilterChange: (StockViewModel.StockFilter) -> Unit,
    onSortChange: (StockViewModel.StockSort) -> Unit,
    onAddClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StockViewModel.StockFilter.entries) { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label, fontSize = 11.sp) }
                )
            }
        }
        
        Box {
            IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Default.List, contentDescription = null) }
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                StockViewModel.StockSort.entries.forEach { sort ->
                    DropdownMenuItem(text = { Text(sort.label) }, onClick = { onSortChange(sort); showSortMenu = false })
                }
            }
        }
        
        IconButton(onClick = onAddClick) { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun IbWalletDashboard(
    accounts: List<com.example.vrapp.model.IbAccount>, 
    assetStatus: StockViewModel.AssetStatus,
    onWalletClick: () -> Unit
) {
    val ibProfit = assetStatus.ibCurrentValue - assetStatus.ibPrincipal
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onWalletClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("무한매수 통합 자산 현황", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Text("상세보기 >", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 통합 수치
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("총 투입 원금", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatCurrency(assetStatus.ibPrincipal, "KRW"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("현재 평가 자산", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatCurrency(assetStatus.ibCurrentValue, "KRW"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("누적 손익", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = "${formatCurrency(ibProfit, "KRW")} (${String.format("%+.1f%%", assetStatus.ibROI)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (ibProfit >= 0) Color.Red else Color.Blue
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f))
            Spacer(Modifier.height(8.dp))

            // 지갑 잔액 요약
            Text("지갑 잔액 (미할당 시드)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            accounts.forEach { acc ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${acc.currency}", style = MaterialTheme.typography.bodySmall)
                    Text(formatCurrency(acc.balance, acc.currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockCard(stock: Stock, yesterdayValuation: Double? = null, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val currentValuation = stock.currentPrice * stock.quantity
    val currentTotal = currentValuation + stock.pool
    val profitLoss = currentTotal - stock.investedPrincipal
    val roi = if (stock.investedPrincipal > 0) (profitLoss / stock.investedPrincipal) * 100 else 0.0
    
    val diffFromYesterday = if (yesterdayValuation != null) currentTotal - yesterdayValuation else 0.0

    val bands = VRCalculator.calculateBands(stock.vValue, stock.bandRatio)
    val order = VRCalculator.calculateOrder(currentValuation, stock.currentPrice, bands)
    
    val cardColor = when {
        !stock.isVr -> MaterialTheme.colorScheme.surface
        order.action == VRCalculator.OrderAction.BUY -> Color(0xFFFFEBEE)
        order.action == VRCalculator.OrderAction.SELL -> Color(0xFFE3F2FD)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stock.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    if (stock.isVr) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                            Text(text = "VR", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Text(text = formatCurrency(stock.currentPrice, stock.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (yesterdayValuation != null) {
                    val diffText = if (diffFromYesterday >= 0) "+${formatCurrency(diffFromYesterday, stock.currency)}" else formatCurrency(diffFromYesterday, stock.currency)
                    val diffPercent = if (yesterdayValuation > 0) (diffFromYesterday / yesterdayValuation) * 100 else 0.0
                    Text(text = "$diffText (${String.format("%+.1f", diffPercent)}%)", style = MaterialTheme.typography.bodySmall, color = if (diffFromYesterday >= 0) Color.Red else Color.Blue)
                } else {
                    Text("-", style = MaterialTheme.typography.bodySmall)
                }
                Text(text = "${formatCurrency(profitLoss, stock.currency)} (${String.format("%+.1f", roi)}%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (profitLoss >= 0) Color.Red else Color.Blue)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "투자원금 : ${formatCurrency(stock.investedPrincipal, stock.currency)}", style = MaterialTheme.typography.bodySmall)
                Text(text = "총자산 : ${formatCurrency(currentTotal, stock.currency)}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "현재평가액 : ${formatCurrency(currentValuation, stock.currency)}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Pool : ${formatCurrency(stock.pool, stock.currency)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IbStockCard(stock: IbStock, onClick: () -> Unit, onLongClick: () -> Unit) {
    val totalAsset = (stock.currentPrice * stock.quantity) + stock.pool
    val currentProfit = totalAsset - stock.principal
    val currentRoi = if (stock.principal > 0) (currentProfit / stock.principal) * 100 else 0.0
    
    val totalCumulativeProfit = currentProfit + stock.totalRealizedProfit
    val totalRoi = if (stock.principal > 0) (totalCumulativeProfit / stock.principal) * 100 else 0.0

    val progress = (stock.currentT / stock.divisions.toDouble()).coerceIn(0.0, 1.0)
    
    val statusColor = when {
        stock.isReverseMode -> Color.Red
        stock.currentT >= (stock.divisions / 2.0) -> Color(0xFFFFA000) // 주황
        stock.quantity <= 0 -> Color.Gray
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stock.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Surface(color = statusColor.copy(alpha = 0.1f), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)) {
                            Text(
                                text = when {
                                    stock.isReverseMode -> "리버스"
                                    stock.quantity <= 0 -> "대기"
                                    stock.currentT < (stock.divisions / 2.0) -> "전반전"
                                    else -> "후반전"
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall, color = statusColor
                            )
                        }
                    }
                    Text(formatCurrency(stock.currentPrice, stock.currency), fontWeight = FontWeight.Bold)
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("T: ${String.format("%.1f", stock.currentT)} / ${stock.divisions}", style = MaterialTheme.typography.bodySmall)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "현재: ${formatCurrency(currentProfit, stock.currency)} (${String.format("%+.1f%%", currentRoi)})", 
                            color = if (currentProfit >= 0) Color.Red else Color.Blue, 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.Bold
                        )
                        // [main][2026-06-04] 누적 수익률 항상 노출 (0이라도 표시하여 투자 지속성 시각화)
                        Text(
                            text = "누적: ${formatCurrency(totalCumulativeProfit, stock.currency)} (${String.format("%+.1f%%", totalRoi)})", 
                            color = if (totalCumulativeProfit >= 0) Color.Red else Color.Blue, 
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("보유: ${formatQuantity(stock.quantity, stock.ticker)}", style = MaterialTheme.typography.bodySmall)
                    Text("Pool: ${formatCurrency(stock.pool, stock.currency)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            LinearProgressIndicator(
                progress = progress.toFloat(),
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockDialog(
    stockViewModel: StockViewModel,
    ibViewModel: IbViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ibAccounts by ibViewModel.allIbAccounts.collectAsState()

    // 투자 전략 선택
    var selectedStrategy by remember { mutableStateOf("VR") } // "VR", "GENERAL", "IB"

    // 종목 정보
    var tickerInput by remember { mutableStateOf("") }
    var stockName by remember { mutableStateOf("") }
    var selectedMarket by remember { mutableStateOf(StockPriceService.Market.KOSPI) }
    var selectedCurrency by remember { mutableStateOf("KRW") }
    var priceStr by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 투자 정보 (공통/VR)
    var vValueStr by remember { mutableStateOf("") }
    var gValue by remember { mutableFloatStateOf(10f) }
    var poolStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var principalStr by remember { mutableStateOf("") }
    var bandRatio by remember { mutableFloatStateOf(15f) }
    var poolLimitRatio by remember { mutableFloatStateOf(0.25f) }
    
    // 투자 정보 (IB 전용)
    var ibDivisions by remember { mutableIntStateOf(40) }
    var ibVolatility by remember { mutableFloatStateOf(15f) }
    
    // 시작일 정보
    var startDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val walletBalance = ibAccounts.find { it.currency == selectedCurrency }?.balance ?: 0.0
    val isInsufficientBalance = selectedStrategy == "IB" && (principalStr.toDoubleOrNull() ?: 0.0) > walletBalance

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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 종목 추가") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 전략 선택
                Text("투자 전략 선택", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("VR" to "VR", "IB" to "무한매수", "GENERAL" to "일반").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedStrategy == code,
                            onClick = { selectedStrategy = code },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. 증시 선택
                Text("증시 선택", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(StockPriceService.Market.entries) { market ->
                        FilterChip(
                            selected = selectedMarket == market,
                            onClick = {
                                selectedMarket = market
                                selectedCurrency = market.currency
                                tickerInput = if (market == StockPriceService.Market.GOLD) "GOLD" else ""
                                stockName = ""; priceStr = ""
                            },
                            label = { Text(market.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                // 3. 티커 입력
                OutlinedTextField(
                    value = tickerInput,
                    onValueChange = { if (selectedMarket != StockPriceService.Market.GOLD) tickerInput = it.trim().uppercase() },
                    label = { Text("종목 티커") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (tickerInput.isNotBlank()) {
                                isLoading = true
                                coroutineScope.launch {
                                    val info = stockViewModel.fetchStockInfo(tickerInput, selectedMarket)
                                    if (info != null) {
                                        stockName = info.name; priceStr = info.price.toString(); selectedCurrency = info.currency
                                    } else {
                                        Toast.makeText(context, "종목을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                                    }
                                    isLoading = false
                                }
                            }
                        }) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Search, contentDescription = null)
                        }
                    }
                )

                if (stockName.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("종목명 : $stockName", fontWeight = FontWeight.Bold)
                            Text("현재가 : ${formatCurrency(priceStr.toDoubleOrNull() ?: 0.0, selectedCurrency)}")
                        }
                    }
                }

                Divider()

                // 4. 전략별 입력 필드
                if (selectedStrategy == "IB") {
                    // 무한매수 전용
                    OutlinedTextField(
                        value = principalStr,
                        onValueChange = { principalStr = it },
                        label = { Text("총 투자 원금 (지갑 할당)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isInsufficientBalance,
                        supportingText = {
                            if (isInsufficientBalance) Text("지갑 잔액이 부족합니다 (잔액: ${formatCurrency(walletBalance, selectedCurrency)})", color = Color.Red)
                            else Text("현재 지갑 잔액: ${formatCurrency(walletBalance, selectedCurrency)}")
                        }
                    )
                    
                    Text("분할 수 설정", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(20, 30, 40).forEach { div ->
                            FilterChip(
                                selected = ibDivisions == div,
                                onClick = { ibDivisions = div },
                                label = { Text("${div}분할") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("목표 변동성 설정", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10f, 15f, 20f).forEach { vol ->
                            FilterChip(
                                selected = ibVolatility == vol,
                                onClick = { ibVolatility = vol },
                                label = { Text("${vol.toInt()}%") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // VR 및 일반 공통
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it }, label = { Text("수량") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = poolStr, onValueChange = { poolStr = it }, label = { Text("Pool") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    if (selectedStrategy == "VR") {
                        OutlinedTextField(value = vValueStr, onValueChange = { vValueStr = it }, label = { Text("초기 V값 (비워두면 자동계산)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Text("밴드 비율: ${bandRatio.toInt()}%", fontWeight = FontWeight.Bold)
                        Slider(value = bandRatio, onValueChange = { bandRatio = it }, valueRange = 5f..30f)
                    }
                }

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("투자 시작일: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startDateMillis))}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val pool = poolStr.toDoubleOrNull() ?: 0.0
                    val principal = principalStr.toDoubleOrNull() ?: ((price * qty) + pool)
                    val fullTicker = tickerInput + selectedMarket.suffix

                    if (selectedStrategy == "IB") {
                        ibViewModel.addIbStock(stockName, fullTicker, principal, ibDivisions, ibVolatility.toDouble(), selectedCurrency, price, qty)
                    } else {
                        val v = vValueStr.toDoubleOrNull() ?: (price * qty)
                        stockViewModel.addStock(stockName, fullTicker, v, gValue.toDouble(), pool, qty, price, selectedCurrency, principal, startDateMillis, selectedStrategy == "VR", bandRatio.toDouble(), poolLimitRatio.toDouble())
                    }
                    onDismiss()
                },
                enabled = stockName.isNotBlank() && priceStr.isNotBlank() && !isInsufficientBalance
            ) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
