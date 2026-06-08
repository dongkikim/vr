package com.example.vrapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vrapp.viewmodel.IbViewModel
import com.example.vrapp.viewmodel.StockViewModel
import java.text.NumberFormat
import java.util.*

// [main][2026-06-08] 현황 화면 종목 데이터 통합 모델
data class StatusItem(
    val id: Long,
    val name: String,
    val quantity: Double,
    val avgPrice: Double,
    val currentPrice: Double,
    val purchaseAmountKRW: Double,
    val currentValuationKRW: Double,
    val profitRate: Double,
    val currency: String,
    val isIb: Boolean
)

enum class StatusSort(val label: String) {
    PROFIT_RATE("수익률"),
    VALUATION("현재평가금액"),
    PURCHASE_AMT("매입금액"),
    NAME("종목명")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    stockViewModel: StockViewModel,
    ibViewModel: IbViewModel
) {
    val stocks by stockViewModel.allStocks.collectAsState()
    val ibStocks by ibViewModel.allIbStocks.collectAsState()
    val exchangeRates by stockViewModel.exchangeRates.collectAsState()

    var sortOption by remember { mutableStateOf(StatusSort.PROFIT_RATE) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    // 필터링 가능한 컬럼들 (종목명 제외)
    val columns = listOf("보유주수", "매입평균가", "현재가", "매입금액", "현재평가금액", "수익률")
    var visibleColumns by remember { mutableStateOf(columns.toSet()) }

    // 데이터 통합 및 가공
    val statusItems = remember(stocks, ibStocks, exchangeRates) {
        val list = mutableListOf<StatusItem>()
        
        // VR 종목 추가
        stocks.forEach { s ->
            val rate = exchangeRates[s.currency] ?: 1.0
            
            // [main][2026-06-08] 사용자 제안 심플 공식 적용
            // 1. 매입금액 = 투자원금
            val purchaseAmountKRW = s.investedPrincipal * rate
            // 2. 현재평가금액 = (현재가 * 보유주수) + Pool
            val currentValuationKRW = ((s.currentPrice * s.quantity) + s.pool) * rate
            // 3. 매입평균가 = 투자원금 / 보유주수
            val avgPrice = if (s.quantity > 0) s.investedPrincipal / s.quantity else 0.0
            // 4. 수익률 = (현재평가금액 - 매입금액) / 매입금액
            val profitRate = if (purchaseAmountKRW > 0) (currentValuationKRW - purchaseAmountKRW) / purchaseAmountKRW * 100 else 0.0
            
            list.add(StatusItem(
                id = s.id,
                name = s.name,
                quantity = s.quantity,
                avgPrice = avgPrice,
                currentPrice = s.currentPrice,
                purchaseAmountKRW = purchaseAmountKRW,
                currentValuationKRW = currentValuationKRW,
                profitRate = profitRate,
                currency = s.currency,
                isIb = false
            ))
        }
        
        // 무한매수 종목 추가
        ibStocks.forEach { s ->
            val rate = exchangeRates[s.currency] ?: 1.0
            
            // [main][2026-06-08] 무한매수도 통일된 로직 적용
            // 매입금액 = 할당된 원금
            val purchaseAmountKRW = s.principal * rate
            // 현재평가금액 = (현재가 * 보유주수) + Pool(남은 할당금)
            val currentValuationKRW = ((s.currentPrice * s.quantity) + s.pool) * rate
            // 매입평균가 = 할당원금 / 보유주수 (또는 전략상 기록된 평단가 사용 가능하나 통일성을 위해 원금기준)
            // 무매는 전략상 평단가가 중요하므로 s.averagePrice를 쓰되, 매입금액과 수익률은 원금 기준으로 계산
            val avgPrice = s.averagePrice 
            val profitRate = if (purchaseAmountKRW > 0) (currentValuationKRW - purchaseAmountKRW) / purchaseAmountKRW * 100 else 0.0
            
            list.add(StatusItem(
                id = s.id,
                name = s.name,
                quantity = s.quantity,
                avgPrice = avgPrice,
                currentPrice = s.currentPrice,
                purchaseAmountKRW = purchaseAmountKRW,
                currentValuationKRW = currentValuationKRW,
                profitRate = profitRate,
                currency = s.currency,
                isIb = true
            ))
        }
        list
    }

    // 정렬 적용
    val sortedItems = remember(statusItems, sortOption) {
        when (sortOption) {
            StatusSort.PROFIT_RATE -> statusItems.sortedByDescending { it.profitRate }
            StatusSort.VALUATION -> statusItems.sortedByDescending { it.currentValuationKRW }
            StatusSort.PURCHASE_AMT -> statusItems.sortedByDescending { it.purchaseAmountKRW }
            StatusSort.NAME -> statusItems.sortedBy { it.name }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("보유 종목 현황", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "필터")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 정렬 옵션 선택기
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("정렬: ", style = MaterialTheme.typography.bodySmall)
                StatusSort.values().forEach { option ->
                    FilterChip(
                        selected = sortOption == option,
                        onClick = { sortOption = option },
                        label = { Text(option.label, fontSize = 12.sp) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // 테이블 (상하좌우 스크롤)
            val horizontalScrollState = rememberScrollState()
            
            Column(modifier = Modifier.fillMaxSize()) {
                // 테이블 헤더
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .horizontalScroll(horizontalScrollState)
                        .padding(vertical = 12.dp)
                ) {
                    TableHeaderCell("종목명", width = 120.dp)
                    if (visibleColumns.contains("보유주수")) TableHeaderCell("보유주수", width = 80.dp)
                    if (visibleColumns.contains("매입평균가")) TableHeaderCell("매입평균가", width = 100.dp)
                    if (visibleColumns.contains("현재가")) TableHeaderCell("현재가", width = 100.dp)
                    if (visibleColumns.contains("매입금액")) TableHeaderCell("매입금액(₩)", width = 120.dp)
                    if (visibleColumns.contains("현재평가금액")) TableHeaderCell("평가금액(₩)", width = 120.dp)
                    if (visibleColumns.contains("수익률")) TableHeaderCell("수익률", width = 80.dp)
                }

                // 테이블 데이터
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sortedItems, key = { "${if (it.isIb) "IB" else "VR"}_${it.id}" }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell(item.name, width = 120.dp, textAlign = TextAlign.Start, fontWeight = FontWeight.Bold)
                            if (visibleColumns.contains("보유주수")) TableCell(formatNumber(item.quantity, 2), width = 80.dp)
                            if (visibleColumns.contains("매입평균가")) TableCell(formatCurrency(item.avgPrice, item.currency), width = 100.dp)
                            if (visibleColumns.contains("현재가")) TableCell(formatCurrency(item.currentPrice, item.currency), width = 100.dp)
                            if (visibleColumns.contains("매입금액")) TableCell(formatKRW(item.purchaseAmountKRW), width = 120.dp)
                            if (visibleColumns.contains("현재평가금액")) TableCell(formatKRW(item.currentValuationKRW), width = 120.dp)
                            if (visibleColumns.contains("수익률")) {
                                val color = when {
                                    item.profitRate > 0 -> Color.Red
                                    item.profitRate < 0 -> Color.Blue
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                TableCell(
                                    String.format("%.2f%%", item.profitRate),
                                    width = 80.dp,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    // 필터 다이얼로그
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("표시 항목 설정") },
            text = {
                Column {
                    columns.forEach { column ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = visibleColumns.contains(column),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        visibleColumns = visibleColumns + column
                                    } else {
                                        visibleColumns = visibleColumns - column
                                    }
                                }
                            )
                            Text(column)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}

@Composable
fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    textAlign: TextAlign = TextAlign.End,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        textAlign = textAlign,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1
    )
}
