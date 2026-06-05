package com.example.vrapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vrapp.viewmodel.IbViewModel

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbWalletDialog(
    viewModel: IbViewModel,
    stockViewModel: com.example.vrapp.viewmodel.StockViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ibAccounts by viewModel.allIbAccounts.collectAsState()
    val historyList by stockViewModel.dailyHistory.collectAsState()
    
    var selectedCurrency by remember { mutableStateOf("USD") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 관리, 1: 내역, 2: 현황

    // 선택된 통화의 내역 불러오기
    val walletHistoryFlow = remember(selectedCurrency) { viewModel.getWalletHistory(selectedCurrency) }
    val walletHistory by walletHistoryFlow.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("무한매수 지갑") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("관리", fontSize = 12.sp) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("내역", fontSize = 12.sp) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("현황", fontSize = 12.sp) })
                }
                
                Spacer(Modifier.height(12.dp))

                if (selectedTab == 2) {
                    IbWalletStatusTab(historyList)
                } else {
                    // 현재 잔액 요약 (공통 표출)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text("통합 잔액 요약", style = MaterialTheme.typography.labelSmall)
                            ibAccounts.forEach { acc ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(acc.currency, fontWeight = FontWeight.Bold)
                                    Text(formatCurrency(acc.balance, acc.currency))
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // 입출금 관리 화면
                        var amountStr by remember { mutableStateOf("") }
                        var isDeposit by remember { mutableStateOf(true) }
                        
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("통화 선택", style = MaterialTheme.typography.labelMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("USD", "KRW").forEach { curr ->
                                    FilterChip(
                                        selected = selectedCurrency == curr,
                                        onClick = { selectedCurrency = curr },
                                        label = { Text(curr) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text("작업 선택", style = MaterialTheme.typography.labelMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = isDeposit,
                                    onClick = { isDeposit = true },
                                    label = { Text("시드 충전 (+)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = !isDeposit,
                                    onClick = { isDeposit = false },
                                    label = { Text("시드 인출 (-)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text(if (isDeposit) "충전할 금액" else "인출할 금액") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                suffix = { Text(selectedCurrency) }
                            )

                            Button(
                                onClick = {
                                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                                    if (amount > 0) {
                                        if (isDeposit) {
                                            viewModel.depositToWallet(selectedCurrency, amount)
                                        } else {
                                            viewModel.withdrawFromWallet(selectedCurrency, amount)
                                        }
                                        amountStr = "" // 실행 후 입력창 초기화
                                        Toast.makeText(context, "처리 완료", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = amountStr.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isDeposit) "충전하기" else "인출하기")
                            }
                        }
                    } else {
                        // 내역(History) 화면
                        Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                listOf("USD", "KRW").forEach { curr ->
                                    FilterChip(
                                        selected = selectedCurrency == curr,
                                        onClick = { selectedCurrency = curr },
                                        label = { Text(curr) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            if (walletHistory.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("해당 통화의 지갑 내역이 없습니다.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(walletHistory) { item ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(
                                                        text = when(item.type) {
                                                            "DEPOSIT" -> "입금"
                                                            "WITHDRAW" -> "출금"
                                                            "ALLOCATE" -> "종목 할당"
                                                            "RETURN" -> "잔금/수익 반환"
                                                            else -> item.type
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        color = if(item.amount >= 0) Color.Red else Color.Blue
                                                    )
                                                    Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                                                    Text(
                                                        text = "${if(item.amount > 0) "+" else ""}${formatCurrency(item.amount, item.currency)}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text("잔액: ${formatCurrency(item.balanceAfter, item.currency)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

@Composable
fun IbWalletStatusTab(history: List<com.example.vrapp.model.DailyAssetHistory>) {
    val lastItem = history.lastOrNull()
    
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        if (lastItem == null || lastItem.ibPrincipal <= 0) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("차트 데이터 수집 중입니다...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            // 1. 요약 카드
            val profit = lastItem.ibCurrentValue - lastItem.ibPrincipal
            val roi = if (lastItem.ibPrincipal > 0) (profit / lastItem.ibPrincipal) * 100 else 0.0
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("무한매수 통합 자산 현황", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("총 투입 원금", style = MaterialTheme.typography.bodySmall)
                        Text(formatCurrency(lastItem.ibPrincipal, "KRW"), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("현재 평가 자산", style = MaterialTheme.typography.bodySmall)
                        Text(formatCurrency(lastItem.ibCurrentValue, "KRW"), fontWeight = FontWeight.Bold)
                    }
                    Divider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("누적 손익", style = MaterialTheme.typography.bodySmall)
                        Text("${formatCurrency(profit, "KRW")} (${String.format("%.2f%%", roi)})", 
                            color = if(profit >= 0) Color.Red else Color.Blue, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. 미니 차트 (최근 30일)
            val filteredHistory = history.filter { it.ibPrincipal > 0 }.takeLast(30)
            if (filteredHistory.size >= 2) {
                Text("자산 성장 추이 (최근 30일)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            com.github.mikephil.charting.charts.LineChart(context).apply {
                                description.isEnabled = false
                                legend.isEnabled = true
                                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                                xAxis.setDrawGridLines(false)
                                xAxis.granularity = 1f
                                xAxis.isGranularityEnabled = true
                                axisRight.isEnabled = false
                                setTouchEnabled(true)
                                setPinchZoom(true)
                            }
                        },
                        update = { chart ->
                            val pEntries = filteredHistory.mapIndexed { i, item -> com.github.mikephil.charting.data.Entry(i.toFloat(), item.ibPrincipal.toFloat()) }
                            val vEntries = filteredHistory.mapIndexed { i, item -> com.github.mikephil.charting.data.Entry(i.toFloat(), item.ibCurrentValue.toFloat()) }
                            
                            val pSet = com.github.mikephil.charting.data.LineDataSet(pEntries, "총원금").apply {
                                color = android.graphics.Color.RED
                                setDrawCircles(false)
                                lineWidth = 2f
                                setDrawValues(false)
                            }
                            val vSet = com.github.mikephil.charting.data.LineDataSet(vEntries, "총자산").apply {
                                color = android.graphics.Color.BLUE
                                setDrawCircles(false)
                                lineWidth = 2f
                                setDrawValues(false)
                            }
                            
                            // X축 날짜 포맷터 적용 (YYYY-MM-DD -> MM/dd)
                            chart.xAxis.apply {
                                val labels = filteredHistory.map { item ->
                                    try {
                                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.date)
                                        if (date != null) SimpleDateFormat("MM/dd", Locale.getDefault()).format(date) else item.date
                                    } catch (e: Exception) {
                                        item.date
                                    }
                                }
                                valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels.toTypedArray())
                                granularity = 1f
                                setLabelCount(minOf(filteredHistory.size, 5), true)
                            }

                            chart.data = com.github.mikephil.charting.data.LineData(pSet, vSet)
                            chart.invalidate()
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DeleteConfirmDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("삭제 확인") },
        text = { Text("'$itemName'을(를) 삭제하시겠습니까?\n무한매수 종목인 경우 운용 중인 잔금은 지갑으로 반환됩니다.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("삭제")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
