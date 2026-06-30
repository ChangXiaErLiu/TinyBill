package com.tinybill.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.tinybill.presentation.navigation.Screen
import com.tinybill.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    currentRoute: String?,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsMenu: Boolean,
    onSettingsMenuChange: (Boolean) -> Unit,
    onMenuItemClick: (String) -> Unit
) {
    if (currentRoute == Screen.Home.route) {
        TopAppBar(
            title = {
                Text(
                    text = "微账",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
                Box {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { onSettingsMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("分类预算") },
                            onClick = {
                                onSettingsMenuChange(false)
                                onMenuItemClick("分类预算")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AA 算账") },
                            onClick = {
                                onSettingsMenuChange(false)
                                onMenuItemClick("AA 算账")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Group, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导出账单") },
                            onClick = {
                                onSettingsMenuChange(false)
                                onMenuItemClick("导出账单")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Download, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("数据备份") },
                            onClick = {
                                onSettingsMenuChange(false)
                                onMenuItemClick("数据备份")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Backup, contentDescription = null)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("应用锁") },
                            onClick = {
                                onSettingsMenuChange(false)
                                onMenuItemClick("应用锁")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
