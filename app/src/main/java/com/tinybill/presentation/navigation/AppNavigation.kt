package com.tinybill.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tinybill.presentation.state.AppStateManager
import com.tinybill.presentation.viewmodel.CalendarViewModel
import com.tinybill.presentation.viewmodel.StatisticsViewModel
import com.tinybill.presentation.viewmodel.TransactionListViewModel
import com.tinybill.ui.screen.CalendarScreen
import com.tinybill.ui.screen.SettingsScreen
import com.tinybill.ui.screen.StatisticsScreen
import com.tinybill.ui.screen.TransactionDetailScreen
import com.tinybill.ui.screen.TransactionListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    appStateManager: AppStateManager,
    contentPadding: PaddingValues = PaddingValues()
) {
    val fadeTransitionSpec = tween<Float>(300)
    val slideTransitionSpec = tween<IntOffset>(300)

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(
            route = Screen.Home.route,
            enterTransition = { fadeIn(animationSpec = fadeTransitionSpec) },
            exitTransition = { fadeOut(animationSpec = fadeTransitionSpec) },
            popEnterTransition = { fadeIn(animationSpec = fadeTransitionSpec) },
            popExitTransition = { fadeOut(animationSpec = fadeTransitionSpec) }
        ) {
            val viewModel: TransactionListViewModel = koinViewModel()
            TransactionListScreen(
                viewModel = viewModel,
                appStateManager = appStateManager,
                modifier = Modifier.padding(contentPadding)
            )
        }

        composable(
            route = Screen.Statistics.route,
            enterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { it } + fadeIn(animationSpec = fadeTransitionSpec) },
            exitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it / 3 } + fadeOut(animationSpec = fadeTransitionSpec) },
            popEnterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { -it / 3 } + fadeIn(animationSpec = fadeTransitionSpec) },
            popExitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it } + fadeOut(animationSpec = fadeTransitionSpec) }
        ) {
            val viewModel: StatisticsViewModel = koinViewModel()
            StatisticsScreen(
                viewModel = viewModel,
                onTransactionClick = { transaction ->
                    appStateManager.selectTransaction(transaction)
                    appStateManager.showAddDialog(transaction)
                },
                onShowExpenseList = { list ->
                    appStateManager.showExpenseList(list)
                },
                onShowIncomeList = { list ->
                    appStateManager.showIncomeList(list)
                },
                onCategoryClick = { category, isExpense, year, month ->
                    navController.navigate("category_detail/$category/$isExpense/$year/$month")
                },
                modifier = Modifier.padding(contentPadding)
            )
        }

        composable(
            route = Screen.Calendar.route,
            enterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { it } + fadeIn(animationSpec = fadeTransitionSpec) },
            exitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it / 3 } + fadeOut(animationSpec = fadeTransitionSpec) },
            popEnterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { -it / 3 } + fadeIn(animationSpec = fadeTransitionSpec) },
            popExitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it } + fadeOut(animationSpec = fadeTransitionSpec) }
        ) {
            val calendarViewModel: CalendarViewModel = koinViewModel()
            CalendarScreen(
                viewModel = calendarViewModel,
                onTransactionClick = { transaction ->
                    appStateManager.selectTransaction(transaction)
                    appStateManager.showAddDialog(transaction)
                },
                onQuickAdd = { year, month, day ->
                    appStateManager.showAddDialogWithDate(year, month, day)
                },
                modifier = Modifier.padding(contentPadding)
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { it } + fadeIn(animationSpec = fadeTransitionSpec) },
            exitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it / 3 } + fadeOut(animationSpec = fadeTransitionSpec) },
            popEnterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { -it / 3 } + fadeIn(animationSpec = fadeTransitionSpec) },
            popExitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it } + fadeOut(animationSpec = fadeTransitionSpec) }
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToScheduled = { navController.navigate(Screen.Scheduled.route) },
                appStateManager = appStateManager,
                context = context,
            )
        }

        composable(
            route = Screen.Privacy.route,
            enterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { it } + fadeIn(animationSpec = fadeTransitionSpec) },
            exitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it / 3 } + fadeOut(animationSpec = fadeTransitionSpec) },
            popEnterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { -it / 3 } + fadeIn(animationSpec = fadeTransitionSpec) },
            popExitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it } + fadeOut(animationSpec = fadeTransitionSpec) }
        ) {
            com.tinybill.ui.screen.PrivacyPolicyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Scheduled.route,
            enterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { it } + fadeIn(animationSpec = fadeTransitionSpec) },
            exitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it / 3 } + fadeOut(animationSpec = fadeTransitionSpec) },
            popEnterTransition = { slideInHorizontally(animationSpec = slideTransitionSpec) { -it / 3 } + fadeIn(animationSpec = fadeTransitionSpec) },
            popExitTransition = { slideOutHorizontally(animationSpec = slideTransitionSpec) { it } + fadeOut(animationSpec = fadeTransitionSpec) }
        ) {
            com.tinybill.ui.screen.ScheduledTransactionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "category_detail/{category}/{isExpense}/{year}/{month}",
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("isExpense") { type = NavType.BoolType },
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            TransactionDetailScreen(
                title = "${category}明细",
                category = category,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Simplified Screen class
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Statistics : Screen("statistics")
    data object Calendar : Screen("calendar")
    data object Settings : Screen("settings")
    data object Privacy : Screen("privacy")
    data object Scheduled : Screen("scheduled")
}

// Screen sealed class 结束
// 注意：Settings 中的 onSettingsClick 已改为弹出下拉菜单而非导航到设置页
