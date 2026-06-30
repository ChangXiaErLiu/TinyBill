# 微账 TinyBill

> 极简的微信 / 支付宝自动记账 App，专为小米 / 红米等深度定制系统优化。
> 无网络权限 · 体积 ≤ 2MB · 后台零打扰。

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-4285F4?logo=android)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-3DDC84)](#)
[![License](https://img.shields.io/badge/License-MIT-blue)](#)

---

## ✨ 核心特性

### 🤖 自动记账
- 通过 Android **无障碍服务（AccessibilityService）** 监听微信、支付宝、云闪付的支付成功页
- 自动提取 **金额、商户名、收支类型**
- 关键字匹配自动归类（美团 / 饿了么 → 餐饮；超市 → 购物；…）
- 解析成功率实时统计，首页 `AutoBillSummaryBanner` 一目了然

### 🔒 隐私优先
- ❌ **不申请 `INTERNET` 权限**，所有数据 100% 留在本机
- 数据存于 Room，配置存于 DataStore（加密 / 普通双轨）
- 应用锁、生物识别、Crash 自采集均不外发

### 📱 系统适配
- 内置 **前台服务**（`ForegroundService` + 通知）防小米 / 红米杀进程
- `BootReceiver` 开机自启动
- `RECEIVE_BOOT_COMPLETED` 持久化无障碍引导状态

### 💡 极简 UI
- Jetpack Compose + Material 3
- 4 个主 Tab：**账单 / 统计 / 日历 / 设置**
- 极简卡片 + Shimmer 骨架屏
- 一键 **快速记账**（FAB）+ **键盘式快速输入**（QuickAddScreen）
- **模板**：常用交易一键复用，自动统计使用频率

### 🛠 实用功能
- 📊 折线 / 饼图统计，分类 / 月度趋势
- 📅 日历视图，按日查看
- 💰 分类预算 + 预算挑战（攒钱目标）
- 🔁 周期性账单（房租 / 订阅自动生成）
- 💸 AA 算账
- 📤 导出 CSV / Excel
- ☁️ 本地备份 / 恢复
- 📲 桌面 Widget（Glance）

---

## 🏗 技术栈

| 维度 | 选型 | 版本 |
| --- | --- | --- |
| 语言 | Kotlin | 1.9.22 |
| UI | Jetpack Compose | BOM 2024.02 |
| 主题 | Material 3 | – |
| 架构 | Clean Architecture (data / domain / presentation) | – |
| 异步 | Coroutines + Flow | 1.7.3 |
| 数据库 | Room + KSP | 2.6.0 |
| 配置 | DataStore (Preferences) | 1.0.0 |
| DI | Koin | 3.5.3 |
| 导航 | Navigation Compose | 2.7.5 |
| Widget | Glance | 1.0.0 |
| 后台 | WorkManager | 2.8.1 |
| 安全 | Biometric + security-crypto | 1.1.0 / α06 |
| 静态分析 | Detekt | 1.23.0 |
| 构建 | AGP | 8.2.0 |
| JVM | Java / Kotlin target | 17 |

> **不依赖** Glide / Coil / OkHttp / Retrofit / Gson 之外的图片 / 网络库。

---

## 📂 项目结构

```
app/src/main/java/com/tinybill/
├── data/                       # 数据层
│   ├── dao/                    # Room DAO（Transaction/Account/Template/Scheduled）
│   ├── database/               # AppDatabase + Migration
│   ├── entity/                 # 实体（Transaction / Account / Template / CustomCategory）
│   ├── repository/             # 仓库实现（Transaction / Account / Budget / Template / CustomCategoryPrefs）
│   ├── stats/                  # 解析成功率统计（ParserStats + ParserStatsStore）
│   └── ServiceLocator.kt       # 旧式服务定位器（逐步被 Koin 取代）
│
├── domain/                     # 领域层
│   ├── model/                  # 领域模型 / DTO
│   ├── repository/             # 仓库接口
│   └── usecase/                # 业务用例
│       ├── account/
│       └── transaction/
│
├── presentation/               # 表示层
│   ├── navigation/             # AppNavigation + Screen sealed class
│   ├── state/                  # AppStateManager + DialogState
│   └── viewmodel/              # 5 个 ViewModel（Transaction / Statistics / Calendar / Account / …）
│
├── service/                    # 系统服务
│   ├── BillAccessibilityService.kt  # 监听微信/支付宝/云闪付
│   ├── ForegroundService.kt         # 持久化前台服务
│   ├── parser/                      # 账单解析（按包名分发）
│   │   ├── BillParser.kt
│   │   └── SnapshotWriter.kt        # 解析失败时落盘快照用于排查
│   └── extractor/                   # 单一职责提取器
│       ├── AmountExtractor.kt
│       ├── MerchantExtractor.kt
│       └── CategoryClassifier.kt
│
├── ui/                         # UI 层
│   ├── AppShell.kt             # 主体 Scaffold
│   ├── AppShellState.kt        # 全局 Shell 状态
│   ├── DialogHost.kt           # Dialog 注册表 + 路由
│   ├── DialogContext.kt        # Dialog 上下文（依赖聚合）
│   ├── components/             # 通用组件
│   │   ├── AppTopBar.kt
│   │   ├── AppBottomBar.kt
│   │   ├── AutoBillSummaryBanner.kt   # 首页自动记账反馈条
│   │   ├── AccessibilityStatusBanner.kt
│   │   ├── Category.kt / CategoryIcons.kt / IconPicker.kt
│   │   ├── Charts.kt / ChartDrawers.kt / ChartModels.kt
│   │   ├── SwipeToDelete.kt
│   │   ├── ShimmerEffect.kt
│   │   ├── TemplateSection.kt
│   │   ├── BudgetComponents.kt
│   │   └── designsystem/       # 统一设计系统（卡片 / 按钮 / 骨架 / 颜色）
│   ├── screen/                 # 全部业务页面（按域拆分）
│   └── theme/                  # Color / Type / Theme
│
├── util/                       # 工具类
│   ├── SettingsManager.kt      # DataStore-based（替代旧 SharedPreferences）
│   ├── CategoryKeywordManager.kt
│   ├── ScheduledTransactionManager.kt / ScheduledTransactionWorker.kt
│   ├── BudgetChallengeManager.kt
│   ├── BackupManager.kt / ExportManager.kt
│   ├── AACalculator.kt
│   ├── HapticManager.kt
│   ├── SecurityManager.kt
│   ├── PermissionHelper.kt
│   └── CrashReporter.kt
│
├── receiver/                   # 广播接收器
│   └── BootReceiver.kt         # 开机自启
│
├── widget/                     # 桌面小部件
│   └── TinyBillWidget.kt
│
├── di/                         # 依赖注入
│   └── AppModules.kt           # database / repository / useCase / manager / viewModel
│
└── MainActivity.kt / TinyBillApp.kt
```

### 三层职责

- **data**：纯数据访问，无任何业务规则。`repository/*Impl` 实现 `domain/repository` 接口。
- **domain**：纯 Kotlin/Java 业务模型 + 仓库接口 + 用例。可独立于 Android 框架测试。
- **presentation / ui / service**：Android 平台相关层（Compose / AccessibilityService / BroadcastReceiver）。

---

## 🚀 构建 & 运行

### 环境要求
- **Android Studio Hedgehog (2023.1.1) 或更高**
- **JDK 17**
- **Android SDK 34**（minSdk 26 / targetSdk 34）
- **Gradle 8.2+**（项目自带 wrapper）

### 命令行构建
```bash
# Debug 包（含调试信息，体积略大）
./gradlew assembleDebug

# Release 包（启用 R8 + 资源压缩，目标是 ≤ 2MB）
./gradlew assembleRelease

# 安装到连接设备
./gradlew installDebug

# 静态检查
./gradlew detekt
```

> ⚠️ Release 构建默认启用了 `isMinifyEnabled = true` + `isShrinkResources = true`，APK 体积会显著压缩。

### 首次启动权限引导
App 内置 `OnboardingScreen`，按顺序引导：
1. **无障碍权限**（必需）：用于监听支付成功页
2. **通知权限**（Android 13+）：前台服务通知
3. **电池优化白名单**（建议）：防被小米 / 红米系统杀掉
4. **自启动权限**（建议）：开机后自动恢复服务

---

## 🧠 核心设计

### 1. 自动记账流水线

```
[AccessibilityService.onAccessibilityEvent]
        │
        ▼
[Filter: pkg ∈ {微信, 支付宝, 云闪付}]
        │
        ▼
[BillParser.parse(rootNode, packageName)]
        │   ┌──────────────┬──────────────┬──────────────┐
        ▼   ▼              ▼              ▼
  AmountExtractor  MerchantExtractor  CategoryClassifier
        │              │                    │
        └──────────────┴────────────────────┘
                              │
                              ▼
                  ParseResult.Expense / Income
                              │
                              ▼
       [ParserStats] 统计 + [TransactionRepository.insert]
                              │
                              ▼
                  [Home: AutoBillSummaryBanner] 实时反馈
```

- **解析失败**会落盘 `SnapshotWriter`（用于线上排障）
- 解析统计写入 `ParserStatsStore`（单例 + AtomicReference + Flow），首页 Banner 实时反映成功率

### 2. 分类匹配

`CategoryClassifier` 优先按 **商户名 → 关键字 → 兜底 "其他"** 顺序归类，关键字由 `CategoryKeywordManager` 管理（DataStore 持久化，支持用户自定义）。

### 3. Dialog 注册表

`DialogHost` 用 `Map<KClass, @Composable (DialogState, DialogContext) -> Unit>` 取代了 `when` 调度，新增 Dialog 只需 3 步：
1. `DialogState.kt` 加新 `data class / object`
2. `AppStateManager` 加便捷 `showXxx()` 方法
3. `dialogRegistry` 加 1 行映射

### 4. 状态管理

- **UI 局部状态**：`remember { mutableStateOf(...) }` + State Hoisting
- **页面级状态**：`ViewModel` 持有 `StateFlow<UiState>`（密封类 Success / Loading / Error）
- **全局 Dialog 状态**：`AppStateManager.dialogState: StateFlow<DialogState>`（单例 + Flow）
- **配置类**：`*PrefsRepository`（DataStore + Flow），无 SharedPreferences

### 5. 防杀策略（针对 MIUI / HyperOS）
- `ForegroundService`（`dataSync` 类型）常驻通知
- `BootReceiver` 开机自启
- `SettingsManager` 持久化引导状态
- `RECEIVE_BOOT_COMPLETED` 权限

---

## 🧪 测试

```bash
# 单元测试
./gradlew test

# Instrumented 测试
./gradlew connectedAndroidTest
```

依赖：JUnit4、MockK、Mockito-Kotlin、Truth、kotlinx-coroutines-test、Arch Core Testing。

---

## 🛡 隐私 & 安全

- ❌ **没有 `INTERNET` 权限**——应用根本无法联网
- 🔐 应用锁支持 **生物识别 / PIN**
- 🗂 敏感偏好用 `EncryptedSharedPreferences`（security-crypto）
- 🚫 `android:allowBackup="false"` + `android:fullBackupContent="false"`，系统级备份关闭
- 🧹 数据库 `indices` 优化查询同时保证去重检测高效

---

## 📦 体积优化

- **R8** 混淆 + Tree-shaking
- **资源压缩** `isShrinkResources = true`
- **无图片加载库**：图标全部用 Material Icons（矢量）
- **无网络栈**：省下 OkHttp / Retrofit / Coil
- **无 Material You 之外的主题依赖**

---

## 🤝 贡献

欢迎提 Issue / PR。请保持：
- 单一职责
- 关键模块补 KDoc
- Detekt 通过：`./gradlew detekt`

---

## 📄 License

MIT
