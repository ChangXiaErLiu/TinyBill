package com.tinybill.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.TransactionRepository
import com.tinybill.presentation.viewmodel.TransactionListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    title: String,
    category: String,
    onNavigateBack: () -> Unit,
    viewModel: TransactionListViewModel = koinViewModel(),
    repository: TransactionRepository = viewModel.getRepository(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // 当前正在编辑的账单：null 表示 BottomSheet 关闭
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is TransactionListViewModel.TransactionListUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is TransactionListViewModel.TransactionListUiState.Success -> {
                    val filteredTransactions = state.transactions.filter {
                        it.category == category
                    }
                    if (filteredTransactions.isEmpty()) {
                        Text(
                            text = "该分类下暂无交易记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTransactions) { transaction ->
                                TransactionCard(
                                    transaction = transaction,
                                    onClick = {
                                        // 弹 BottomSheet 而不是全屏 Dialog
                                        // 用户能保留对详情页的视觉感知
                                        editingTransaction = transaction
                                    }
                                )
                            }
                        }
                    }
                }
                is TransactionListViewModel.TransactionListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // BottomSheet 渲染在 Scaffold 之外，保留 Material 弹层机制
    editingTransaction?.let { transaction ->
        EditTransactionBottomSheet(
            transaction = transaction,
            repository = repository,
            onDismiss = { editingTransaction = null },
        )
    }
}
