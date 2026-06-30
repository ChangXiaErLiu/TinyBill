package com.tinybill.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tinybill.data.entity.Account
import com.tinybill.data.entity.AccountTransaction
import com.tinybill.data.entity.AccountTransactionType
import com.tinybill.domain.repository.IAccountRepository
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 转账对话框。
 *
 * 从 A 账户转账到 B 账户，自动修正双方余额并记录转账流水。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    repository: IAccountRepository = koinInject()
) {
    var selectedFrom by remember { mutableStateOf(accounts.firstOrNull()) }
    var selectedTo by remember { mutableStateOf(accounts.getOrNull(1)) }
    var amountText by remember { mutableStateOf("") }
    var showDropdownFrom by remember { mutableStateOf(false) }
    var showDropdownTo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = selectedFrom != null && selectedTo != null && selectedFrom != selectedTo && amount > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "转账",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // From account
                Text(
                    text = "从",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(
                    expanded = showDropdownFrom,
                    onExpandedChange = { showDropdownFrom = it }
                ) {
                    OutlinedTextField(
                        value = selectedFrom?.let { "${it.name} (¥${String.format("%.1f", it.currentBalance)})" } ?: "选择账户",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdownFrom) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showDropdownFrom,
                        onDismissRequest = { showDropdownFrom = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(account.name)
                                        Text(
                                            text = "¥${String.format("%.1f", account.currentBalance)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedFrom = account
                                    showDropdownFrom = false
                                }
                            )
                        }
                    }
                }

                // Swap button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val temp = selectedFrom
                            selectedFrom = selectedTo
                            selectedTo = temp
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "交换账户",
                            tint = PrimaryGreen
                        )
                    }
                }

                // To account
                Text(
                    text = "到",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(
                    expanded = showDropdownTo,
                    onExpandedChange = { showDropdownTo = it }
                ) {
                    OutlinedTextField(
                        value = selectedTo?.let { "${it.name} (¥${String.format("%.1f", it.currentBalance)})" } ?: "选择账户",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdownTo) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showDropdownTo,
                        onDismissRequest = { showDropdownTo = false }
                    ) {
                        accounts.filter { it.id != selectedFrom?.id }.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(account.name)
                                        Text(
                                            text = "¥${String.format("%.1f", account.currentBalance)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTo = account
                                    showDropdownTo = false
                                }
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = it
                        }
                    },
                    label = { Text("转账金额") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("¥ ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            val from = selectedFrom ?: return@Button
                            val to = selectedTo ?: return@Button
                            scope.launch {
                                try {
                                    // 扣减源账户
                                    val fromNewBalance = from.currentBalance - amount
                                    repository.updateBalance(from.id, fromNewBalance)
                                    repository.insertAccountTransaction(
                                        AccountTransaction(
                                            accountId = from.id,
                                            transactionId = 0,
                                            type = AccountTransactionType.TRANSFER_OUT,
                                            amount = amount,
                                            balanceAfter = fromNewBalance,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    // 增加目标账户
                                    val toNewBalance = to.currentBalance + amount
                                    repository.updateBalance(to.id, toNewBalance)
                                    repository.insertAccountTransaction(
                                        AccountTransaction(
                                            accountId = to.id,
                                            transactionId = 0,
                                            type = AccountTransactionType.TRANSFER_IN,
                                            amount = amount,
                                            balanceAfter = toNewBalance,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    onSuccess()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("转账失败: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("确认转账", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
