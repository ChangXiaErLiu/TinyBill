package com.tinybill.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinybill.R
import com.tinybill.data.entity.Transaction
import com.tinybill.data.repository.CustomCategoryPrefsRepository
import com.tinybill.presentation.state.AppStateManager
import com.tinybill.ui.components.CustomCategoryDialog
import com.tinybill.util.HapticManager
import kotlinx.coroutines.launch

/**
 * 微账设置主页
 *
 * 包含 6 个 section：
 * - 分类管理（支出/收入自定义分类）
 * - 预算（月度总预算、分类预算）
 * - 系统（无障碍、电池优化、自启动）
 * - 数据（导出、备份恢复）
 * - 安全（App 锁）
 * - 关于
 *
 * 分类相关的 [CategoryListDialog]、图标映射 [CATEGORY_ICON_MAP] 与
 * [CategoryListItem] 已拆分到 [SettingsCategorySection.kt]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToScheduled: () -> Unit = {},
    appStateManager: AppStateManager,
    context: Context,
    customCategoryRepository: CustomCategoryPrefsRepository = remember { CustomCategoryPrefsRepository(context.applicationContext) },
) {
    val scope = rememberCoroutineScope()
    val customCategories by customCategoryRepository.categories.collectAsState(initial = emptyList())
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CustomCategoryPrefsRepository.Category?>(null) }
    var showCategoryListDialog by remember { mutableStateOf(false) }
    var selectedCategoryType by remember { mutableStateOf<Int?>(null) }

    val expenseCategories = customCategories.filter { it.type == Transaction.TYPE_EXPENSE }
    val incomeCategories = customCategories.filter { it.type == Transaction.TYPE_INCOME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticManager.performClick()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "分类管理",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.RemoveCircleOutline,
                            title = "支出分类",
                            subtitle = "${expenseCategories.size} 个自定义分类",
                            showArrow = true,
                            onClick = {
                                HapticManager.performClick()
                                selectedCategoryType = Transaction.TYPE_EXPENSE
                                showCategoryListDialog = true
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            icon = Icons.Outlined.AddCircleOutline,
                            title = "收入分类",
                            subtitle = "${incomeCategories.size} 个自定义分类",
                            showArrow = true,
                            onClick = {
                                HapticManager.performClick()
                                selectedCategoryType = Transaction.TYPE_INCOME
                                showCategoryListDialog = true
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "预算",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "预算设置",
                        subtitle = "月度总预算、分类预算",
                        onClick = {
                            HapticManager.performClick()
                            appStateManager.showBudgetSettings()
                        }
                    )
                }
            }

            item {
                Text(
                    text = "系统",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.Accessibility,
                            title = "无障碍设置",
                            subtitle = "自动记账服务",
                            onClick = {
                                HapticManager.performClick()
                                openAccessibilitySettings(context)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            icon = Icons.Outlined.BatteryChargingFull,
                            title = "电池优化",
                            subtitle = "允许后台运行",
                            onClick = {
                                HapticManager.performClick()
                                openBatteryOptimizationSettings(context)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            icon = Icons.Outlined.SettingsApplications,
                            title = "自启动管理",
                            subtitle = "允许应用自启动",
                            onClick = {
                                HapticManager.performClick()
                                openAutoStartSettings(context)
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "数据",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.Upload,
                            title = "导出数据",
                            subtitle = "导出为 CSV 格式",
                            onClick = {
                                HapticManager.performClick()
                                appStateManager.showExportDialog()
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItem(
                            icon = Icons.Outlined.CloudUpload,
                            title = "备份与恢复",
                            subtitle = "云端备份、本地备份",
                            onClick = {
                                HapticManager.performClick()
                                appStateManager.showBackupDialog()
                            }
                        )
                    }
                }
            }

            // 定期账单
            item {
                Text(
                    text = "定期",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Schedule,
                        title = "定期账单",
                        subtitle = "每月固定日期自动生成账单",
                        onClick = {
                            HapticManager.performClick()
                            onNavigateToScheduled()
                        }
                    )
                }
            }

            // 安全
            item {
                Text(
                    text = "安全",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.Lock,
                            title = "应用锁",
                            subtitle = "设置密码保护隐私",
                            onClick = {
                                HapticManager.performClick()
                                appStateManager.showAppLockDialog()
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Info,
                        title = "关于微账",
                        subtitle = "版本信息、源代码",
                        onClick = {
                            HapticManager.performClick()
                            showAboutDialog(context)
                        }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.PrivacyTip,
                        title = "隐私政策",
                        subtitle = "我们如何保护您的数据",
                        onClick = {
                            HapticManager.performClick()
                            onNavigateToPrivacy()
                        }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Share,
                        title = "分享微账",
                        subtitle = "推荐给好友",
                        onClick = {
                            HapticManager.performClick()
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT,
                                    "推荐一款记账 App「微账」：完全离线、自动记账、简单好用。")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "分享微账"))
                        }
                    )
                }
            }
        }
    }

    if (showCategoryListDialog && selectedCategoryType != null) {
        val type = selectedCategoryType ?: return
        val typeCategories = customCategories.filter { it.type == type }
        CategoryListDialog(
            type = type,
            categories = typeCategories,
            onDismiss = { showCategoryListDialog = false },
            onAddClick = {
                showCategoryListDialog = false
                editingCategory = null
                showAddCategoryDialog = true
            },
            onEditClick = { category ->
                showCategoryListDialog = false
                editingCategory = category
                showAddCategoryDialog = true
            },
            onDeleteClick = { category ->
                scope.launch {
                    customCategoryRepository.deleteCategory(category.name, category.type)
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        val type = selectedCategoryType ?: Transaction.TYPE_EXPENSE
        // 转换 Manager.Category → UI CustomCategory（不同 data class 字段类型：Long vs Color）
        val existingAsCustomCategories = customCategories.map { cat ->
            com.tinybill.ui.components.CustomCategory(
                name = cat.name,
                icon = cat.icon,
                isEmoji = cat.isEmoji,
                color = cat.color,
                type = cat.type
            )
        }
        CustomCategoryDialog(
            existingCategories = existingAsCustomCategories,
            editingCategory = editingCategory?.let { cat ->
                com.tinybill.ui.components.CustomCategory(
                    name = cat.name,
                    icon = cat.icon,
                    isEmoji = cat.isEmoji,
                    color = cat.color,
                    type = cat.type
                )
            },
            defaultType = type,
            onDismiss = {
                showAddCategoryDialog = false
                editingCategory = null
            },
            onConfirm = { newCategory ->
                if (editingCategory != null) {
                    // 编辑：使用 updateCategory(oldName, oldType, newCategory)
                    val newRepositoryCategory = CustomCategoryPrefsRepository.Category(
                        name = newCategory.name,
                        icon = newCategory.icon,
                        isEmoji = newCategory.isEmoji,
                        colorValue = newCategory.color.value.toLong(),
                        type = newCategory.type
                    )
                    scope.launch {
                        customCategoryRepository.updateCategory(
                            editingCategory!!.name,
                            editingCategory!!.type,
                            newRepositoryCategory
                        )
                    }
                } else {
                    // 新增：使用 addCategory(name, icon, isEmoji, colorValue, type)
                    scope.launch {
                        customCategoryRepository.addCategory(
                            newCategory.name,
                            newCategory.icon,
                            newCategory.isEmoji,
                            newCategory.color.value.toLong(),
                            newCategory.type
                        )
                    }
                }
                showAddCategoryDialog = false
                editingCategory = null
            }
        )
    }
}

// CategoryListDialog / CATEGORY_ICON_MAP / CategoryListItem 已拆分到 [SettingsCategorySection.kt]

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    intent.data = Uri.parse("package:${context.packageName}")
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openAutoStartSettings(context: Context) {
    val autoStartIntents = listOf(
        Intent().setComponent(android.content.ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )),
        Intent().setComponent(android.content.ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        )),
        Intent().setComponent(android.content.ComponentName(
            "com.oppo.safe",
            "com.oppo.safe.permission.startup.StartupAppListActivity"
        )),
        Intent().setComponent(android.content.ComponentName(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        )),
        Intent().setComponent(android.content.ComponentName(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        ))
    )
    for (intent in autoStartIntents) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // 尝试下一个
        }
    }
    // fallback: 通用设置
    try {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun showAboutDialog(context: Context) {
    val url = context.getString(R.string.about_project_url)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
