package com.tinybill.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tinybill.data.entity.Transaction
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.ui.theme.SuccessColor
import com.tinybill.ui.theme.WarningColor
import com.tinybill.data.repository.BudgetRepository
import com.tinybill.util.BudgetChallenge
import com.tinybill.util.BudgetChallengeManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetChallengeDialog(
    challengeManager: BudgetChallengeManager,
    categoryBudgets: List<BudgetRepository.CategoryBudget>,
    onDismiss: () -> Unit
) {
    val currentChallenge by challengeManager.getCurrentChallengeFlow().collectAsState()
    val streakData by challengeManager.getStreakDataFlow().collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎮 预算挑战",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (currentChallenge != null) {
                    ActiveChallengeCard(
                        challenge = currentChallenge!!,
                        onComplete = { challengeManager.completeChallenge() },
                        onCancel = { challengeManager.cancelChallenge() }
                    )
                } else {
                    NewChallengeCard(
                        categoryBudgets = categoryBudgets,
                        onCreateChallenge = { title, category, targetAmount, type, days ->
                            challengeManager.createChallenge(
                                title = title,
                                category = category,
                                targetAmount = targetAmount,
                                challengeType = type,
                                days = days
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                StreakCard(
                    currentStreak = streakData.currentStreak,
                    maxStreak = streakData.maxStreak
                )
            }
        }
    }
}

@Composable
private fun ActiveChallengeCard(
    challenge: BudgetChallenge,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val daysRemaining = ((challenge.endDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    val progress = challenge.completedDays.toFloat() / challenge.totalDays.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryGreen.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "进行中",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGreen
                )
                Text(
                    text = "剩余 $daysRemaining 天",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = challenge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PrimaryGreen,
                trackColor = PrimaryGreen.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已完成 ${challenge.completedDays} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "目标 ${challenge.totalDays} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(minOf(challenge.currentStreak, 7)) {
                    Text("🔥", fontSize = 20.sp)
                }
                if (challenge.currentStreak > 7) {
                    Text(
                        text = "×${challenge.currentStreak}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarningColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorColor
                    )
                ) {
                    Text("取消挑战")
                }

                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessColor
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("提前完成")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChallengeCard(
    categoryBudgets: List<BudgetRepository.CategoryBudget>,
    onCreateChallenge: (String, String?, Double?, BudgetChallenge.ChallengeType, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(BudgetChallenge.ChallengeType.NO_SPENDING) }
    var challengeTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var targetAmount by remember { mutableStateOf("") }
    var selectedDays by remember { mutableIntStateOf(7) }

    val presetTitles = when (selectedType) {
        BudgetChallenge.ChallengeType.NO_SPENDING -> listOf(
            "本周不点外卖", "本月不喝奶茶", "本周不网购", "挑战零支出"
        )
        BudgetChallenge.ChallengeType.UNDER_BUDGET -> listOf(
            "餐饮预算挑战", "购物预算挑战", "娱乐预算挑战"
        )
        BudgetChallenge.ChallengeType.DAILY_LIMIT -> listOf(
            "日花费不超100", "日花费不超50", "日花费不超200"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "创建新挑战",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "挑战类型",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChallengeTypeChip(
                    title = "不消费",
                    emoji = "🚫",
                    selected = selectedType == BudgetChallenge.ChallengeType.NO_SPENDING,
                    onClick = {
                        selectedType = BudgetChallenge.ChallengeType.NO_SPENDING
                        challengeTitle = ""
                    },
                    modifier = Modifier.weight(1f)
                )
                ChallengeTypeChip(
                    title = "预算内",
                    emoji = "💰",
                    selected = selectedType == BudgetChallenge.ChallengeType.UNDER_BUDGET,
                    onClick = {
                        selectedType = BudgetChallenge.ChallengeType.UNDER_BUDGET
                        challengeTitle = ""
                    },
                    modifier = Modifier.weight(1f)
                )
                ChallengeTypeChip(
                    title = "日限额",
                    emoji = "📅",
                    selected = selectedType == BudgetChallenge.ChallengeType.DAILY_LIMIT,
                    onClick = {
                        selectedType = BudgetChallenge.ChallengeType.DAILY_LIMIT
                        challengeTitle = ""
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "预设挑战",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val filteredCategories = categoryBudgets.map { it.category }

            presetTitles.forEach { title ->
                FilterChip(
                    selected = challengeTitle == title,
                    onClick = {
                        challengeTitle = title
                        if (selectedType == BudgetChallenge.ChallengeType.UNDER_BUDGET) {
                            selectedCategory = filteredCategories.firstOrNull { title.contains(it) }
                        }
                    },
                    label = { Text(title) },
                    modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                )
            }

            if (selectedType != BudgetChallenge.ChallengeType.NO_SPENDING) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (selectedType == BudgetChallenge.ChallengeType.UNDER_BUDGET) "选择分类" else "日消费上限",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedType == BudgetChallenge.ChallengeType.UNDER_BUDGET && filteredCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCategories.take(3).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("金额") },
                        prefix = { Text("¥") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "挑战天数",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 7, 14, 30).forEach { days ->
                    FilterChip(
                        selected = selectedDays == days,
                        onClick = { selectedDays = days },
                        label = { Text("${days}天") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val finalTitle = challengeTitle.ifEmpty {
                        when (selectedType) {
                            BudgetChallenge.ChallengeType.NO_SPENDING -> "${selectedDays}天不消费挑战"
                            BudgetChallenge.ChallengeType.UNDER_BUDGET -> "${selectedCategory ?: "餐饮"}预算挑战"
                            BudgetChallenge.ChallengeType.DAILY_LIMIT -> "日花${targetAmount}元挑战"
                        }
                    }
                    val amount = targetAmount.toDoubleOrNull()
                    onCreateChallenge(
                        finalTitle,
                        selectedCategory,
                        amount,
                        selectedType,
                        selectedDays
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始挑战")
            }
        }
    }
}

@Composable
private fun ChallengeTypeChip(
    title: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryGreen.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreakCard(
    currentStreak: Int,
    maxStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🔥",
                    fontSize = 28.sp
                )
                Text(
                    text = "$currentStreak 天",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "当前连续",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🏆",
                    fontSize = 28.sp
                )
                Text(
                    text = "$maxStreak 天",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "历史最佳",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}