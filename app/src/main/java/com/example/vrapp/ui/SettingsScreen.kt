package com.example.vrapp.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vrapp.viewmodel.StockViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StockViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // State for Dialogs
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var showImportErrorDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    
    // Temp storage for imported JSON before confirmation
    var tempImportJson by remember { mutableStateOf("") }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = viewModel.exportData()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    Toast.makeText(context, "데이터 내보내기 성공", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "내보내기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stringBuilder = StringBuilder()
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line)
                                line = reader.readLine()
                            }
                        }
                    }
                    val json = stringBuilder.toString()
                    
                    // Validate JSON structure (simple check)
                    if (json.contains("stocks") && json.contains("transactions")) {
                        tempImportJson = json
                        showImportConfirmDialog = true
                    } else {
                        showImportErrorDialog = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showImportErrorDialog = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("데이터 관리") },
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("데이터 백업 및 복구", style = MaterialTheme.typography.titleMedium)
            
            // Export
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("내보내기 (Export)", style = MaterialTheme.typography.titleSmall)
                    Text("현재 앱의 모든 데이터를 파일로 저장합니다.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportLauncher.launch("VRApp_Backup_$timestamp.json")
                    }) {
                        Text("데이터 내보내기")
                    }
                }
            }

            // Import
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("가져오기 (Import)", style = MaterialTheme.typography.titleSmall)
                    Text("저장된 파일에서 데이터를 복구합니다.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("데이터 가져오기")
                    }
                }
            }
            
            Divider() 
            
            // Reset
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("데이터 초기화", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("모든 데이터를 삭제하고 초기 상태로 되돌립니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("초기화", color = Color.White)
                    }
                }
            }
        }
    }
    
    // Dialogs
    
    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("데이터 가져오기") },
            text = { Text("선택한 파일이 유효합니다.\n\n정말로 가져오시겠습니까?\n현재 앱의 모든 데이터가 초기화되고 선택한 파일의 내용으로 덮어씌워집니다.\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = viewModel.importData(tempImportJson)
                            if (success) {
                                Toast.makeText(context, "데이터 복구 완료", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "데이터 복구 실패", Toast.LENGTH_SHORT).show()
                            }
                            showImportConfirmDialog = false
                            tempImportJson = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("초기화 후 적용") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportConfirmDialog = false 
                    tempImportJson = ""
                }) { Text("취소") }
            }
        )
    }
    
    if (showImportErrorDialog) {
        AlertDialog(
            onDismissRequest = { showImportErrorDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
            title = { Text("파일 오류") },
            text = { Text("선택한 파일 형식이 올바르지 않거나 손상되었습니다.\n올바른 백업 파일을 선택해주세요.") },
            confirmButton = {
                TextButton(onClick = { showImportErrorDialog = false }) { Text("확인") }
            }
        )
    }
    
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
            title = { Text("데이터 초기화") },
            text = { Text("정말로 모든 데이터를 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetData()
                        Toast.makeText(context, "데이터가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("초기화") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("취소") }
            }
        )
    }
}