package com.tinybill.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 隐私政策页面。
 *
 * 微账是完全离线的本地记账应用，不收集任何用户信息。
 * 此页面说明数据处理方式，满足应用分发的合规要求。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "隐私政策",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "微账隐私政策",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "最后更新日期：2026 年 7 月",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            PolicySection(
                title = "1. 引言",
                content = "微账（以下简称「本应用」）是一款完全离线的个人记账工具。我们重视您的隐私，并致力于保护您的个人数据。本隐私政策说明了我们如何收集、使用和保护您的信息。"
            )

            PolicySection(
                title = "2. 数据收集",
                content = "本应用不收集任何个人数据。具体而言：\n\n" +
                        "• 我们不会收集您的姓名、邮箱、电话号码等个人身份信息\n" +
                        "• 我们不会收集您的位置信息\n" +
                        "• 我们不会收集您的设备标识符\n" +
                        "• 我们不会收集您的使用习惯或行为数据\n" +
                        "• 本应用没有服务器，所有数据仅存储在您的设备本地\n" +
                        "• 本应用不请求 INTERNET 权限，完全离线运行"
            )

            PolicySection(
                title = "3. 数据存储",
                content = "您的所有记账数据（账单记录、账户信息、预算设置、自定义分类等）均存储在您设备的本地数据库中。具体存储方式包括：\n\n" +
                        "• SQLite 数据库（通过 Room 框架管理）\n" +
                        "• DataStore Preferences（用户偏好设置）\n" +
                        "• SharedPreferences（部分配置项）\n\n" +
                        "这些数据不会离开您的设备。"
            )

            PolicySection(
                title = "4. 数据导出与备份",
                content = "本应用支持将数据导出为 CSV 文件或 JSON 备份文件，文件和导出操作完全在本地完成。您也可以通过备份功能将数据保存到您指定的位置（如文件管理器或云存储服务），此过程由您主动触发，应用不会自动上传任何数据。"
            )

            PolicySection(
                title = "5. 无障碍服务",
                content = "如果您启用了自动记账功能，本应用会请求无障碍服务权限。此权限仅用于读取屏幕上显示的金额和商户信息，以便自动记录账单。我们不会通过此权限收集任何其他信息。您可以随时在系统设置中关闭此权限。"
            )

            PolicySection(
                title = "6. 生物识别",
                content = "如果您启用了应用锁功能，本应用会使用系统提供的生物识别 API（指纹/面部识别）进行身份验证。生物识别数据由系统安全管理，本应用无法访问或存储您的生物特征信息。"
            )

            PolicySection(
                title = "7. 第三方服务",
                content = "本应用不集成任何第三方分析服务、广告SDK或社交媒体SDK。本应用完全离线运行，不依赖任何后端服务器。"
            )

            PolicySection(
                title = "8. 数据安全",
                content = "虽然本应用不收集您的数据，但我们仍然采取了合理的安全措施来保护您存储在设备上的数据：\n\n" +
                        "• 支持应用锁（密码/生物识别）保护\n" +
                        "• 数据存储在应用私有目录中\n" +
                        "• Android 系统提供的文件级加密保护"
            )

            PolicySection(
                title = "9. 儿童隐私",
                content = "本应用不面向 13 岁以下的儿童，也不会故意收集儿童的个人信息。"
            )

            PolicySection(
                title = "10. 政策更新",
                content = "如果我们对本隐私政策进行重大变更，我们会在应用内更新「最后更新日期」并通过应用内通知告知您。"
            )

            PolicySection(
                title = "11. 联系我们",
                content = "如果您对本隐私政策有任何疑问，请通过应用设置中的「意见反馈」功能联系我们。" +
                        "或者发送邮件至：privacy@tinybill.app（示例邮箱，请替换为实际邮箱）"
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "本应用完全离线运行，不收集任何个人信息，所有数据仅存储在您的设备本地。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
    )
}
