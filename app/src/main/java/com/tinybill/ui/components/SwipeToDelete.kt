package com.tinybill.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tinybill.data.entity.Transaction
import com.tinybill.ui.theme.ErrorColor
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.util.HapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteTransactionCard(
    transaction: Transaction,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confirmValueChange: (SwipeToDismissBoxValue) -> Boolean = { dismissValue ->
        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
            HapticManager.performDelete()
            onDelete()
            true
        } else {
            false
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = confirmValueChange
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> ErrorColor
                    else -> Color.Transparent
                },
                label = "swipe_color"
            )
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                label = "swipe_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.scale(scale),
                    tint = Color.White
                )
            }
        },
        content = {
            TransactionCard(
                transaction = transaction,
                onClick = onClick
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissContainer(
    modifier: Modifier = Modifier,
    onDelete: (Transaction) -> Unit,
    onUndo: (Transaction) -> Unit,
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(pendingDelete) {
        val deleted = pendingDelete ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "已删除",
            actionLabel = "撤销",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndo(deleted)
        }
        pendingDelete = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = modifier.padding(padding)
        ) {
            transactions.forEach { transaction ->
                val onDeleteClick = remember(transaction.id) {
                    {
                        pendingDelete = transaction
                        onDelete(transaction)
                    }
                }
                val onItemClick = remember(transaction.id) {
                    { onTransactionClick(transaction) }
                }
                SwipeToDeleteTransactionCard(
                    transaction = transaction,
                    onDelete = onDeleteClick,
                    onClick = onItemClick
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableTransactionCard(
    transaction: Transaction,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        label = "select_bg"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = {
            if (isSelected) {
                onClick()
            } else {
                onLongClick()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryGreen
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            CategoryIcon(category = transaction.category)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "¥${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.type == Transaction.TYPE_EXPENSE)
                    MaterialTheme.colorScheme.error else PrimaryGreen
            )
        }
    }
}
