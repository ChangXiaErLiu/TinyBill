package com.tinybill.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tinybill.presentation.navigation.AppNavigation
import com.tinybill.presentation.navigation.Screen
import com.tinybill.presentation.state.AppStateManager
import com.tinybill.ui.components.AppBottomBar
import com.tinybill.ui.components.AppTopBar
import com.tinybill.ui.theme.PrimaryGreen

/**
 * App 主体 Scaffold：TopBar + BottomBar + FAB + 导航。
 *
 * 不负责任何 Dialog（Dialog 由 DialogHost 渲染在 Scaffold 之外，
 * 通过 Material 的弹层机制自动覆盖在 Scaffold 之上）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    appStateManager: AppStateManager,
    @Suppress("UNUSED_PARAMETER") _shellState: AppShellState,
    snackbarHostState: SnackbarHostState,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showSettingsMenu by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val isError = data.visuals.message.contains("失败") || data.visuals.message.contains("错误")
                val containerColor = if (isError) MaterialTheme.colorScheme.error else PrimaryGreen
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = containerColor,
                    contentColor = Color.White,
                    dismissActionContentColor = Color.White
                )
            }
        },
        topBar = {
            AppTopBar(
                currentRoute = currentRoute,
                onSearchClick = { appStateManager.showSearchDialog() },
                onSettingsClick = { showSettingsMenu = true },
                showSettingsMenu = showSettingsMenu,
                onSettingsMenuChange = { showSettingsMenu = it },
                onMenuItemClick = { item ->
                    when (item) {
                        "分类预算" -> appStateManager.showBudgetSettings()
                        "AA 算账" -> appStateManager.showAACalculator()
                        "导出账单" -> appStateManager.showExportDialog()
                        "数据备份" -> appStateManager.showBackupDialog()
                        "应用锁" -> appStateManager.showAppLockDialog()
                    }
                }
            )
        },
        bottomBar = {
            AppBottomBar(
                navController = navController,
                currentRoute = currentRoute
            )
        },
        floatingActionButton = {
            if (currentRoute == Screen.Home.route) {
                ExtendedFloatingActionButton(
                    onClick = { appStateManager.showAddDialog() },
                    containerColor = PrimaryGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("记账", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { paddingValues ->
        AppNavigation(
            navController = navController,
            appStateManager = appStateManager,
            contentPadding = paddingValues
        )
    }
}
