package com.example.vrapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vrapp.logic.IBCalculator
import com.example.vrapp.model.IbStock
import com.example.vrapp.model.IbTransaction
import com.example.vrapp.viewmodel.IbViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbDetailScreen(
    viewModel: IbViewModel,
    ibStockId: Long,
    onBack: () -> Unit
) {
    // 1. 데이터 로드 보장: 화면 진입 시 해당 종목 정보와 거래 내역을 확실히 fetch 하도록 수정
    val stock by viewModel.currentIbStock.collectAsState()
    val history by viewModel.ibTransactionHistory.collectAsState()
    val sma5DayMap by viewModel.sma5DayMap.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showCycleEndDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ibStockId) {
        viewModel.loadIbStock(ibStockId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stock?.name ?: "종목 상세", style = MaterialTheme.typography.titleMedium)
                        if (stock != null) {
                            Text(stock!!.ticker, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshPrices() }) { Icon(Icons.Default.Refresh, contentDescription = null) }
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
        },
        floatingActionButton = {
            if (selectedTab == 0 && stock != null && stock!!.principal > 0) {
                FloatingActionButton(onClick = { showTransactionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { padding ->
        stock?.let { currentStock ->
            // [중요 필터링] 거래 내역이 현재 종목의 ID와 정확히 일치하는지 다시 한 번 필터링 (데이터 오염 방지)
            val filteredHistory = history.filter { it.ibStockId == ibStockId }

            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // 1. 상단 확장 헤더 (상세 정보 노출)
                IbEnhancedHeader(currentStock, filteredHistory)

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("오늘의 가이드") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("누적 이력") })
                }

                when (selectedTab) {
                    0 -> IbCurrentTab(
                        stock = currentStock,
                        history = filteredHistory,
                        sma5Day = sma5DayMap[currentStock.id],
                        onDeleteTransaction = { viewModel.rollbackLatestTransaction(currentStock, it) },
                        onShowSettlementDialog = { showCycleEndDialog = true },
                        onRestartCycle = { viewModel.restartIbCycle(currentStock, it) }
                    )
                    1 -> IbHistoryTab(filteredHistory, currentStock.cycleCount)
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    // 다이얼로그 로직 (기존 유지)
    if (showTransactionDialog && stock != null) {
        IbTransactionDialog(
            viewModel = viewModel,
            stock = stock!!,
            sma5Day = sma5DayMap[stock!!.id],
            onDismiss = { showTransactionDialog = false },
            onTransactionSuccess = { finalQty ->
                showTransactionDialog = false
                if (finalQty <= 0) {
                    showCycleEndDialog = true
                }
            }
        )
    }

    if (showCycleEndDialog && stock != null) {
        IbCycleEndDialog(
            stock = stock!!,
            onConfirm = { actualAmount, shouldDelete, nextPrincipal ->
                viewModel.completeCycle(stock!!, actualAmount, shouldDelete, nextPrincipal)
                showCycleEndDialog = false
                onBack()
            },
            onDismiss = { showCycleEndDialog = false }
        )
    }

    // [main][2026-06-05] 무한매수 종목 정보 수정 다이얼로그 추가
    if (showEditDialog && stock != null) {
        IbEditStockDialog(
            stock = stock!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                viewModel.updateIbStockName(stock!!, newName)
                showEditDialog = false
            }
        )
    }
}

/**
 * 무한매수 종목 정보 수정 다이얼로그
 * 현재는 종목명 수정을 우선 지원함
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbEditStockDialog(
    stock: IbStock,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(stock.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("종목 정보 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("종목명") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "티커(${stock.ticker}) 및 주요 설정값은 현재 화면에서 직접 수정이 불가능합니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                },
                enabled = name.isNotBlank() && name != stock.name
            ) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
fun IbCycleEndDialog(
    stock: IbStock,
    onConfirm: (Double, Boolean, Double) -> Unit, // actualAmount, shouldDelete, nextPrincipal
    onDismiss: () -> Unit
) {
    val estimatedAmount = stock.pool
    var actualAmountStr by remember(stock.pool) { mutableStateOf(estimatedAmount.toString()) }
    var shouldDelete by remember { mutableStateOf(false) }
    var isRestartMode by remember { mutableStateOf(true) }
    var nextPrincipalStr by remember { mutableStateOf(stock.principal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎉 사이클 종료 및 정산") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("축하합니다! 모든 수량을 매도하여 한 사이클이 완료되었습니다.\n운용 자금을 지갑으로 반환합니다.", style = MaterialTheme.typography.bodyMedium)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Text("계산상 반환 금액", style = MaterialTheme.typography.labelSmall)
                        Text(formatCurrency(estimatedAmount, stock.currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedTextField(
                    value = actualAmountStr,
                    onValueChange = { actualAmountStr = it },
                    label = { Text("실제 계좌 입금액 (수수료/오차 보정)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    suffix = { Text(stock.currency) }
                )
                
                Divider()

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                    isRestartMode = !isRestartMode
                    if(isRestartMode) shouldDelete = false
                }) {
                    Checkbox(checked = isRestartMode, onCheckedChange = { 
                        isRestartMode = it
                        if(it) shouldDelete = false
                    })
                    Text("정산 후 바로 다음 사이클 시작", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                if (isRestartMode) {
                    OutlinedTextField(
                        value = nextPrincipalStr,
                        onValueChange = { nextPrincipalStr = it },
                        label = { Text("다음 사이클 할당 원금") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        suffix = { Text(stock.currency) }
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                        shouldDelete = !shouldDelete 
                        if(shouldDelete) isRestartMode = false
                    }) {
                        Checkbox(checked = shouldDelete, onCheckedChange = { 
                            shouldDelete = it
                            if(it) isRestartMode = false
                        })
                        Text("정산 후 이 종목 삭제 (목록에서 제거)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = actualAmountStr.toDoubleOrNull() ?: estimatedAmount
                val nextP = if(isRestartMode) (nextPrincipalStr.toDoubleOrNull() ?: 0.0) else 0.0
                onConfirm(amount, shouldDelete, nextP)
            }) { 
                Text(when {
                    isRestartMode -> "정산 및 다음 사이클 시작"
                    shouldDelete -> "지갑 반환 및 종목 삭제"
                    else -> "지갑으로 반환"
                })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun IbEnhancedHeader(stock: IbStock, history: List<IbTransaction>) {
    // 1. 현재 보유 주식의 평가 손익 계산
    val currentMarketValue = stock.currentPrice * stock.quantity
    val currentPurchaseCost = stock.averagePrice * stock.quantity
    val unrealizedProfit = if (stock.quantity > 0) currentMarketValue - currentPurchaseCost else 0.0
    val unrealizedRoi = if (currentPurchaseCost > 0) (unrealizedProfit / currentPurchaseCost) * 100 else 0.0

    // 2. 현재 사이클 내에서의 실현 손익 (매매 수익) 계산
    val currentCycleHistory = history.filter { it.cycleNumber == stock.cycleCount }
    var realizedProfit = 0.0
    var tempQty = 0.0
    var tempAvgPrice = 0.0

    // 과거 거래를 순차적으로 훑으며 매도 시점의 실현 손익 합산
    currentCycleHistory.sortedBy { it.date }.forEach { trans ->
        if (trans.type.contains("BUY")) {
            val totalCost = (tempQty * tempAvgPrice) + (trans.quantity * trans.price)
            tempQty += trans.quantity
            tempAvgPrice = if (tempQty > 0) totalCost / tempQty else 0.0
        } else if (trans.type.contains("SELL")) {
            // 매도 시: (매도가 - 당시평단) * 매도수량
            realizedProfit += (trans.price - tempAvgPrice) * trans.quantity
            tempQty -= trans.quantity
            if (tempQty <= 0) {
                tempAvgPrice = 0.0
                tempQty = 0.0
            }
        } else if (trans.type == "SPLIT") {
            val ratio = if (tempQty > 0) trans.quantity / tempQty else 1.0
            tempQty = trans.quantity
            tempAvgPrice /= ratio
        }
    }

    // 3. 사이클 총합 및 누적 지표
    val cycleTotalProfit = realizedProfit + unrealizedProfit
    val totalAsset = currentMarketValue + stock.pool
    
    // 전체 누적 수익: (과거 사이클의 최종 실현 손익 합계) + (현재 사이클의 총 수익)
    val totalCumulativeProfit = stock.totalRealizedProfit + cycleTotalProfit
    val totalRoi = if (stock.principal > 0) (totalCumulativeProfit / stock.principal) * 100 else 0.0
    
    val progress = (stock.currentT / stock.divisions.toDouble()).coerceIn(0.0, 1.0)
    val avgPrice = stock.averagePrice

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Row 1: 주요 수치 (자산)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text("총 평가 자산", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(if(stock.principal > 0) formatCurrency(totalAsset, stock.currency) else "-", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if(stock.principal > 0) {
                    Text(
                        text = "사이클 수익: ${formatCurrency(cycleTotalProfit, stock.currency)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (cycleTotalProfit >= 0) Color.Red else Color.Blue
                    )
                    Text(
                        text = "누적 합계: ${formatCurrency(totalCumulativeProfit, stock.currency)} (${String.format("%+.2f", totalRoi)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (totalCumulativeProfit >= 0) Color.Red else Color.Blue
                    )
                } else {
                    Text("새 사이클 대기 중", style = MaterialTheme.typography.titleMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        
        // 수익 세분화 (실현 vs 평가)
        if (stock.principal > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("실현 수익 (매도)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatCurrency(realizedProfit, stock.currency), 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold,
                        color = if (realizedProfit >= 0) Color.Red else Color.Blue
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("평가 수익 (보유)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${formatCurrency(unrealizedProfit, stock.currency)} (${String.format("%+.2f", unrealizedRoi)}%)", 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold,
                        color = if (unrealizedProfit >= 0) Color.Red else Color.Blue
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        Spacer(Modifier.height(16.dp))

        // Row 2: 투자 상세 설정 정보
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoBlock(label = "설정 원금", value = formatCurrency(stock.principal, stock.currency), modifier = Modifier.weight(1f))
            InfoBlock(label = "현재 잔금(Pool)", value = formatCurrency(stock.pool, stock.currency), modifier = Modifier.weight(1f))
            InfoBlock(label = "보유 수량", value = "${formatQuantity(stock.quantity, stock.ticker)}주", modifier = Modifier.weight(0.8f))
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoBlock(label = "분할 설정", value = "${stock.divisions}분할", modifier = Modifier.weight(1f))
            InfoBlock(label = "목표 수익률", value = "${stock.volatility.toInt()}%", modifier = Modifier.weight(1f))
            InfoBlock(label = "현재 평단가", value = formatCurrency(avgPrice, stock.currency), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // Row 3: 진행률 및 회차 정보
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("진행 회차: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("${String.format("%.1f", stock.currentT)} / ${stock.divisions} T", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("${(progress * 100).toInt()}% 완료", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = progress.toFloat(),
                modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.Transparent, RoundedCornerShape(4.dp)),
                color = if(stock.isReverseMode) Color.Red else if(progress > 0.5) Color(0xFFFFA000) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun InfoBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IbCurrentTab(
    stock: IbStock,
    history: List<IbTransaction>,
    sma5Day: Double?,
    onDeleteTransaction: (IbTransaction) -> Unit,
    onShowSettlementDialog: () -> Unit = {},
    onRestartCycle: (Double) -> Unit = {}
) {
    if (stock.principal <= 0) {
        IbRestartUI(stock, onRestartCycle)
    } else {
        // [main][2026-06-05] 수동 계산식 제거하고 DB의 정확한 평단가 사용
        val avgPrice = stock.averagePrice
        val guide = IBCalculator.generateDailyGuide(
            ticker = stock.ticker,
            currency = stock.currency,
            avgPrice = avgPrice,
            currentPrice = stock.currentPrice,
            remainingPool = stock.pool,
            divisions = stock.divisions,
            volatility = stock.volatility,
            tValue = stock.currentT,
            totalQuantity = stock.quantity,
            isReverseMode = stock.isReverseMode,
            sma5Day = sma5Day
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (stock.quantity <= 0 && stock.currentT > 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Text("사이클 종료 감지!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("모든 수량을 매도하였습니다. 아래 버튼을 눌러 지갑으로 수익금을 회수하고 다음 사이클을 준비하세요.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onShowSettlementDialog,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("지금 정산하기")
                            }
                        }
                    }
                }
            }
            
            item {
                IbGuideTable(guide, stock.currency)
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("최근 거래 내역 (현재 사이클)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            val currentCycleHistory = history.filter { it.cycleNumber == stock.cycleCount }
            if (currentCycleHistory.isEmpty()) {
                item { 
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Text("이번 사이치 회차의 거래 내역이 없습니다.", color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(currentCycleHistory) { tx ->
                    IbTransactionItem(tx, stock.currency, isLatest = tx == currentCycleHistory.first(), onDelete = { onDeleteTransaction(tx) })
                }
            }
            item { Spacer(Modifier.height(50.dp)) } // 하단 FAB 여백
        }
    }
}

@Composable
fun IbRestartUI(stock: IbStock, onRestart: (Double) -> Unit) {
    var principalStr by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text("${stock.cycleCount}회차 사이클 대기 중", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("정산이 완료되었습니다. 새 사이클을 위해 원금을 할당해주세요.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
        
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = principalStr,
            onValueChange = { principalStr = it },
            label = { Text("할당할 새 원금") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            suffix = { Text(stock.currency) }
        )
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { 
                val p = principalStr.toDoubleOrNull() ?: 0.0
                if (p > 0) onRestart(p)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = principalStr.isNotBlank()
        ) {
            Text("새 사이클 시작하기")
        }
    }
}

// IbGuideTable, IbGuideRow, IbTransactionItem, IbHistoryTab, IbTransactionDialog, IbCycleEndDialog 
// (기존과 동일하지만 일부 UI 디테일 개선을 위해 재생성)

@Composable
fun IbGuideTable(guide: IBCalculator.DailyGuide, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = if (guide.isReverseMode) Color(0xFFFFF1F0) else Color.White),
        border = if (guide.isReverseMode) androidx.compose.foundation.BorderStroke(1.dp, Color.Red) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (guide.isReverseMode) Icons.Default.Warning else Icons.Default.ListAlt,
                    contentDescription = null,
                    tint = if (guide.isReverseMode) Color.Red else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (guide.isReverseMode) "오늘의 리버스 매매 가이드" else "오늘의 무매 주문 가이드",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (guide.isReverseMode) Color.Red else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(vertical = 6.dp)) {
                Text("구분", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("단가", modifier = Modifier.weight(2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("수량", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("방식", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            
            IbGuideRow("매수1(평단)", guide.buy1AvgPrice, guide.buy1Qty, "LOC", currency, isBuy = true)
            if (guide.buy2Qty > 0 || guide.buy2StarPrice > 0) {
                IbGuideRow("매수2(별점)", guide.buy2StarPrice, guide.buy2Qty, "LOC", currency, isBuy = true)
            }
            
            Divider(Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
            
            IbGuideRow("매도1(쿼터)", guide.sell1StarPrice, guide.sell1Qty, "LOC", currency, isBuy = false)
            if (guide.sell2Qty > 0) {
                IbGuideRow("매도2(익절)", guide.sell2LimitPrice, guide.sell2Qty, "지정가", currency, isBuy = false)
            }
            
            Spacer(Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = "💡 큰수 가이드(주문거부 방지): ${formatCurrency(guide.bigNumberPrice, currency)}",
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun IbGuideRow(label: String, price: Double, qty: Double, method: String, currency: String, isBuy: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = if(isBuy) Color(0xFFD32F2F) else Color(0xFF1976D2), fontWeight = FontWeight.Medium)
        Text(formatCurrency(price, currency), modifier = Modifier.weight(2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
        Text("${qty.toInt()}주", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(method, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IbTransactionItem(tx: IbTransaction, currency: String, isLatest: Boolean, onDelete: () -> Unit) {
    var showDeleteIcon by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if (isLatest) showDeleteIcon = !showDeleteIcon }, onLongClick = { if (isLatest) showDeleteIcon = true }),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = when { tx.type.contains("BUY") -> Color.Red; tx.type.contains("SELL") -> Color.Blue; else -> Color.Gray }
                    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(tx.type, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(tx.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(Modifier.height(6.dp))
                Text("${formatQuantity(tx.quantity, "")}주 @ ${formatCurrency(tx.price, currency)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("총 ${formatCurrency(tx.amount, currency)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            if (showDeleteIcon && isLatest) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text("T값", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(String.format("%.1f", tx.previousT), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun IbHistoryTab(history: List<IbTransaction>, currentCycle: Int) {
    val cycleGroups = history.groupBy { it.cycleNumber }.toSortedMap(compareByDescending { it })
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cycleGroups.forEach { (cycle, txs) ->
            item {
                var expanded by remember { mutableStateOf(cycle == currentCycle) }
                val cycleProfit = txs.sumOf { if(it.type.contains("SELL")) it.amount else -it.amount }
                
                Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = if(cycle == currentCycle) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${cycle}회차 사이클", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                if(!expanded) {
                                    Text("누적 손익: ${formatCurrency(cycleProfit, "USD")}", style = MaterialTheme.typography.labelSmall, color = if(cycleProfit >= 0) Color.Red else Color.Blue)
                                }
                            }
                            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                        if (expanded) {
                            Spacer(Modifier.height(12.dp))
                            Divider(thickness = 0.5.dp)
                            Spacer(Modifier.height(8.dp))
                            txs.sortedByDescending { it.date }.forEach { tx ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(tx.type, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if(tx.type.contains("BUY")) Color.Red else Color.Blue)
                                        Text(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.date)), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.Gray)
                                    }
                                    Text("${formatQuantity(tx.quantity, "")}주 / ${formatCurrency(tx.price, "USD")}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IbTransactionDialog(
    viewModel: IbViewModel,
    stock: IbStock,
    sma5Day: Double?,
    onDismiss: () -> Unit,
    onTransactionSuccess: (Double) -> Unit = {}
) {
    var selectedAction by remember { mutableStateOf("매수") } // "매수", "매도", "분할"
    var priceStr by remember { mutableStateOf(stock.currentPrice.toString()) }
    var qtyStr by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    val avgPrice = if(stock.quantity > 0) (stock.principal - stock.pool)/stock.quantity else stock.currentPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("무한매수 거래 기록") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("액션 선택", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val actions = listOf("매수", "매도", "분할")
                    actions.forEach { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action },
                            label = { Text(action) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("체결 단가") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it }, label = { Text("체결 수량") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                
                val internalAction = when(selectedAction) {
                    "매수" -> if(stock.isReverseMode) "REVERSE_BUY" else "BUY_FULL"
                    "매도" -> if(stock.isReverseMode) "REVERSE_SELL" else "SELL_QUARTER"
                    else -> "SPLIT"
                }
                val nextT = IBCalculator.calculateNextT(stock.currentT, stock.divisions, stock.isReverseMode, internalAction)
                Text("예상 차기 T값: ${String.format("%.2f", nextT)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = priceStr.toDoubleOrNull() ?: 0.0
                val q = qtyStr.toDoubleOrNull() ?: 0.0
                if (p > 0 && q > 0) {
                    val internalAction = when(selectedAction) {
                        "매수" -> if(stock.isReverseMode) "REVERSE_BUY" else "BUY_FULL"
                        "매도" -> if(stock.isReverseMode) "REVERSE_SELL" else "SELL_QUARTER"
                        else -> "SPLIT"
                    }
                    coroutineScope.launch {
                        viewModel.executeIbTransaction(stock, internalAction, p, q)
                        val finalQty = if (selectedAction == "매수") stock.quantity + q else stock.quantity - q
                        onTransactionSuccess(finalQty)
                    }
                }
            }) { Text("기록") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
