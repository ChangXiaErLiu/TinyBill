package com.tinybill.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tinybill.ui.theme.PrimaryGreen
import com.tinybill.util.SecurityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockDialog(
    onDismiss: () -> Unit,
    onSetupComplete: () -> Unit
) {
    val isLockEnabled = SecurityManager.isAppLockEnabled()
    val lockType = SecurityManager.getLockType()
    
    var selectedLockType by remember { mutableIntStateOf(if (isLockEnabled) lockType else 0) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("应用锁", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isLockEnabled) {
                    Text(
                        text = "设置应用锁来保护您的账单隐私",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "解锁方式",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LockTypeOption(
                            title = "密码解锁",
                            description = "使用数字密码解锁",
                            icon = Icons.Default.Lock,
                            selected = selectedLockType == 1,
                            onClick = { selectedLockType = 1 }
                        )
                        
                        LockTypeOption(
                            title = "暂不设置",
                            description = "之后可在设置中重新开启",
                            icon = Icons.Default.SkipNext,
                            selected = selectedLockType == 0,
                            onClick = {
                                selectedLockType = 0
                                onDismiss()
                            }
                        )
                    }
                } else {
                    Text(
                        text = "当前已设置${if (lockType == 1) "密码锁" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("输入密码") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = {
                            SecurityManager.clearLock()
                            onSetupComplete()
                        }) {
                            Text("清除锁定")
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                if (SecurityManager.verifyPassword(password)) {
                                    onSetupComplete()
                                } else {
                                    errorMessage = "密码错误"
                                    password = ""
                                }
                            },
                            enabled = password.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("解锁")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isLockEnabled && selectedLockType == 1) {
                Button(
                    onClick = {
                        if (password.length < 4) {
                            errorMessage = "密码至少4位"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorMessage = "两次密码不一致"
                            confirmPassword = ""
                            return@Button
                        }
                        SecurityManager.setPassword(password)
                        onSetupComplete()
                    },
                    enabled = password.length >= 4 && confirmPassword.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("设置")
                }
            } else if (!isLockEnabled) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
        dismissButton = {
            if (isLockEnabled) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockTypeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                PrimaryGreen.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryGreen
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("修改密码", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("当前密码") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认新密码") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!SecurityManager.verifyPassword(currentPassword)) {
                        errorMessage = "当前密码错误"
                        return@Button
                    }
                    if (newPassword.length < 4) {
                        errorMessage = "新密码至少4位"
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMessage = "两次密码不一致"
                        return@Button
                    }
                    SecurityManager.setPassword(newPassword)
                    onSuccess()
                },
                enabled = currentPassword.isNotEmpty() && newPassword.length >= 4 && confirmPassword.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
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