package com.tinybill.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.util.AACalculator
import com.tinybill.util.AASplitResult
import com.tinybill.util.PersonSplit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AACalculatorDialog(
    onDismiss: () -> Unit
) {
    var totalAmount by remember { mutableStateOf("") }
    var numberOfPeople by remember { mutableStateOf("2") }
    var tipPercentage by remember { mutableStateOf("0") }
    var discountAmount by remember { mutableStateOf("") }
    var selectedPayer by remember { mutableIntStateOf(-1) }
    var excludeList by remember { mutableStateOf(setOf<Int>()) }
    var result by remember { mutableStateOf<AASplitResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AA 算账", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = {
                        totalAmount = it.filter { c -> c.isDigit() || c == '.' }
                        result = null
                    },
                    label = { Text("总金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        focusedLabelColor = PrimaryGreen
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = numberOfPeople,
                        onValueChange = {
                            numberOfPeople = it.filter { c -> c.isDigit() }
                            result = null
                            excludeList = emptySet()
                        },
                        label = { Text("人数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )

                    OutlinedTextField(
                        value = tipPercentage,
                        onValueChange = {
                            tipPercentage = it.filter { c -> c.isDigit() }
                            result = null
                        },
                        label = { Text("小费%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("%") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )
                }

                OutlinedTextField(
                    value = discountAmount,
                    onValueChange = {
                        discountAmount = it.filter { c -> c.isDigit() || c == '.' }
                        result = null
                    },
                    label = { Text("优惠券/折扣（可选）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("-¥") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        focusedLabelColor = PrimaryGreen
                    )
                )

                val peopleCount = numberOfPeople.toIntOrNull() ?: 0
                if (peopleCount > 1) {
                    Text(
                        text = "谁是付款人？",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0 until peopleCount).forEach { index ->
                            FilterChip(
                                selected = selectedPayer == index,
                                onClick = {
                                    selectedPayer = if (selectedPayer == index) -1 else index
                                    result = null
                                },
                                label = { Text("第${index + 1}人") }
                            )
                        }
                    }

                    Text(
                        text = "谁没来？",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0 until peopleCount).forEach { index ->
                            FilterChip(
                                selected = excludeList.contains(index),
                                onClick = {
                                    excludeList = if (excludeList.contains(index)) {
                                        excludeList - index
                                    } else {
                                        excludeList + index
                                    }
                                    result = null
                                },
                                label = { Text("第${index + 1}人") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ErrorColor.copy(alpha = 0.2f),
                                    selectedLabelColor = ErrorColor
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val amount = totalAmount.toDoubleOrNull() ?: return@Button
                        val people = numberOfPeople.toIntOrNull() ?: return@Button
                        val tip = tipPercentage.toIntOrNull() ?: 0
                        val discount = discountAmount.toDoubleOrNull() ?: 0.0

                        result = when {
                            tip > 0 -> AACalculator.calculateWithTips(
                                totalAmount = amount,
                                numberOfPeople = people,
                                tipPercentage = tip / 100f,
                                excludeList = excludeList.toList()
                            )
                            discount > 0 -> AACalculator.calculateWithDiscount(
                                totalAmount = amount,
                                discountAmount = discount,
                                numberOfPeople = people,
                                excludeList = excludeList.toList()
                            )
                            else -> AACalculator.calculateSplit(
                                totalAmount = amount,
                                numberOfPeople = people,
                                excludeList = excludeList.toList(),
                                payerIndex = selectedPayer
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (totalAmount.toDoubleOrNull() ?: 0.0) > 0 && (numberOfPeople.toIntOrNull() ?: 0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("计算")
                }

                result?.let { splitResult ->
                    Divider()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "每人应付",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "¥${String.format("%.2f", splitResult.perPersonAmount)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )

                            if (splitResult.remainder > 0) {
                                Text(
                                    text = "（含抹零 ¥${splitResult.remainder}）",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    splitResult.splits.forEachIndexed { index: Int, split: PersonSplit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = split.personName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (split.isPayer) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("付款人", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(26.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = PrimaryGreen.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                                if (excludeList.contains(index)) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("未参与", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(26.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = ErrorColor.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "¥${String.format("%.2f", split.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}