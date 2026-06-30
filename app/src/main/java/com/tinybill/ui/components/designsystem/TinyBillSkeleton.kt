package com.tinybill.ui.components.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tinybill.ui.components.ShimmerEffect

/**
 * 骨架屏布局组件。
 *
 * 核心 ShimmerEffect 统一在 [com.tinybill.ui.components.ShimmerEffect]。
 * 本文件只提供布局骨架的排列方式。
 */
@Composable
fun BudgetCardSkeleton() {
    TinyBillCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerEffect(
                    modifier = Modifier
                        .width(120.dp)
                        .height(24.dp)
                )
                ShimmerEffect(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerEffect(
                    modifier = Modifier
                        .width(80.dp)
                        .height(32.dp)
                )
                ShimmerEffect(
                    modifier = Modifier
                        .width(80.dp)
                        .height(32.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionListSkeleton(
    itemCount: Int = 5
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(itemCount) {
            TinyBillCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        ShimmerEffect(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
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

@Composable
fun StatisticsSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerEffect(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            ShimmerEffect(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        
        // Chart
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        
        // Category list
        repeat(4) {
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}
