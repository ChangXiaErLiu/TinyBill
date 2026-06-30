package com.tinybill.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tinybill.data.repository.BudgetRepository
import com.tinybill.data.repository.CustomCategoryPrefsRepository
import com.tinybill.util.BudgetChallengeManager
import com.tinybill.util.SettingsManager

/**
 * 聚合 App Shell 生命周期内共享的 Manager/Repository，避免在 TinyBillAppContent 中
 * 重复创建 + 散落 4 个 remember。
 *
 * 历史包袱：旧版保留 BudgetManager / CustomCategoryManager facade 字段。
 * 2026-06 全部 UI 迁到 Repository 后，facade 字段已彻底死代码，整体删除。
 *
 * AppShell、DialogHost 都通过这个对象访问同一份实例。
 */
@Stable
class AppShellState(
    val settingsManager: SettingsManager,
    val budgetChallengeManager: BudgetChallengeManager,
    val budgetRepository: BudgetRepository,
    val customCategoryRepository: CustomCategoryPrefsRepository,
) {
    companion object {
        fun create(context: Context): AppShellState {
            val appContext = context.applicationContext
            return AppShellState(
                settingsManager = SettingsManager.getInstance(appContext),
                budgetChallengeManager = BudgetChallengeManager.getInstance(appContext),
                budgetRepository = BudgetRepository(appContext),
                customCategoryRepository = CustomCategoryPrefsRepository(appContext),
            )
        }
    }
}

@Composable
fun rememberAppShellState(): AppShellState {
    val context = LocalContext.current
    return remember { AppShellState.create(context) }
}

