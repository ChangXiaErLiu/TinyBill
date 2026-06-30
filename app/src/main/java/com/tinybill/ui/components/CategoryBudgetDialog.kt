package com.tinybill.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tinybill.ui.theme.PrimaryGreen

@Composable
fun CategoryBudgetDialog(
    category: String,
    currentBudget: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${category}预算",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "设置该分类的月度预算上限",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            budgetText = it
                        }
                    },
                    label = { Text("预算金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("¥ ") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                if (currentBudget != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            onConfirm(null)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清除预算")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val budget = budgetText.toDoubleOrNull()
                    onConfirm(budget)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun CategoryBudgetSummary(
    category: String,
    spent: Double,
    budget: Double?,
    modifier: Modifier = Modifier
) {
    if (budget != null && budget > 0) {
        val percentage = (spent / budget * 100).coerceIn(0.0, 100.0)
        val isOverBudget = spent > budget

        Column(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "预算",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "¥${String.format("%.2f", spent)} / ¥${String.format("%.2f", budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = (percentage / 100).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else PrimaryGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            if (isOverBudget) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已超出 ¥${String.format("%.2f", spent - budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}