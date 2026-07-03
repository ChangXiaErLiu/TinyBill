package com.tinybill

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tinybill.ui.TinyBillAppContent
import com.tinybill.ui.theme.TinyBillTheme
import com.tinybill.util.NotificationPermissionHelper
import com.tinybill.widget.WidgetActions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        NotificationPermissionHelper.registerPermissionLauncher(this)

        // 检查是否从 Widget 快捷记账启动
        checkWidgetAction(intent)

        setContent {
            TinyBillTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TinyBillAppContent()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkWidgetAction(intent)
    }

    private fun checkWidgetAction(intent: Intent?) {
        when (intent?.action) {
            WidgetActions.ACTION_QUICK_EXPENSE -> {
                TinyBillApp.pendingQuickAddAction = true to false
            }
            WidgetActions.ACTION_QUICK_INCOME -> {
                TinyBillApp.pendingQuickAddAction = false to false
            }
        }
    }
}
