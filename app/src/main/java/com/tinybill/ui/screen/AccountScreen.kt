package com.tinybill.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.Account
import com.tinybill.data.entity.AccountType
import com.tinybill.presentation.viewmodel.AccountViewModel
import com.tinybill.ui.components.ShimmerEffect
import com.tinybill.ui.components.designsystem.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary by viewModel.summary.collectAsState()
    var showTransferDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "资产管理",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Show add account dialog */ },
                containerColor = TinyBillColors.Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Summary Card
            summary?.let { s ->
                AccountSummaryCard(summary = s)
                Spacer(modifier = Modifier.height(12.dp))
                // 转账按钮
                if (uiState is AccountViewModel.AccountUiState.Success &&
                    (uiState as AccountViewModel.AccountUiState.Success).accounts.size >= 2
                ) {
                    OutlinedButton(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("转账")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Account List
            when (val state = uiState) {
                is AccountViewModel.AccountUiState.Loading -> {
                    AccountListSkeleton(itemCount = 3)
                }
                is AccountViewModel.AccountUiState.Success -> {
                    if (state.accounts.isEmpty()) {
                        EmptyStateView(
                            title = "还没有账户",
                            description = "添加你的第一个账户开始管理资产",
                            action = {
                                TinyBillButton(
                                    text = "添加账户",
                                    onClick = { /* Show add dialog */ }
                                )
                            }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.accounts) { account ->
                                AccountCard(
                                    account = account,
                                    onClick = { /* Show account detail */ }
                                )
                            }
                        }
                    }
                }
                is AccountViewModel.AccountUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.onEvent(AccountViewModel.AccountUserEvent.OnRefresh) }
                    )
                }
            }
        }
    }

    // 转账对话框
    if (showTransferDialog) {
        val accounts = (uiState as? AccountViewModel.AccountUiState.Success)?.accounts ?: emptyList()
        TransferDialog(
            accounts = accounts,
            onDismiss = { showTransferDialog = false },
            onSuccess = {
                showTransferDialog = false
                viewModel.onEvent(AccountViewModel.AccountUserEvent.OnRefresh)
            }
        )
    }
}

@Composable
fun AccountSummaryCard(summary: com.tinybill.domain.usecase.account.GetAccountSummaryUseCase.AccountSummary) {
    TinyBillCard {
        Column {
            Text(
                text = "资产概览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "总资产",
                    amount = summary.totalAssets,
                    color = TinyBillColors.Income
                )
                SummaryItem(
                    label = "总负债",
                    amount = summary.totalLiabilities,
                    color = TinyBillColors.Expense
                )
                SummaryItem(
                    label = "净资产",
                    amount = summary.netWorth,
                    color = if (summary.netWorth >= 0) TinyBillColors.Primary else TinyBillColors.Expense
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¥${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AccountCard(
    account: Account,
    onClick: () -> Unit
) {
    TinyBillCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(account.color).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAccountIcon(account.icon),
                    contentDescription = null,
                    tint = Color(account.color),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getAccountTypeName(account.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Balance
            val isNegative = account.currentBalance < 0
            Text(
                text = "${if (isNegative) "-" else ""}¥${String.format("%.2f", kotlin.math.abs(account.currentBalance))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isNegative) TinyBillColors.Expense else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AccountListSkeleton(itemCount: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(itemCount) {
            TinyBillCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerEffect(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerEffect(
                            modifier = Modifier
                                .width(100.dp)
                                .height(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ShimmerEffect(
                            modifier = Modifier
                                .width(60.dp)
                                .height(16.dp)
                        )
                    }
                    ShimmerEffect(
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                    )
                }
            }
        }
    }
}

fun getAccountIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        Account.ICON_CASH -> Icons.Default.Payments
        Account.ICON_BANK_CARD -> Icons.Default.CreditCard
        Account.ICON_CREDIT_CARD -> Icons.Default.CreditScore
        Account.ICON_ALIPAY -> Icons.Default.AccountBalance
        Account.ICON_WECHAT -> Icons.AutoMirrored.Filled.Chat
        Account.ICON_INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.Default.AccountBalanceWallet
    }
}

fun getAccountTypeName(type: AccountType): String {
    return when (type) {
        AccountType.CASH -> "现金"
        AccountType.BANK_CARD -> "借记卡"
        AccountType.CREDIT_CARD -> "信用卡"
        AccountType.E_WALLET -> "电子钱包"
        AccountType.INVESTMENT -> "投资账户"
        AccountType.PREPAID_CARD -> "储值卡"
        AccountType.OTHER -> "其他"
    }
}
