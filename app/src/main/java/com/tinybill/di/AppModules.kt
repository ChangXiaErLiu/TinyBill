package com.tinybill.di

import com.tinybill.data.database.AppDatabase
import com.tinybill.data.repository.AccountRepositoryImpl
import com.tinybill.data.repository.TransactionRepository
import com.tinybill.domain.repository.IAccountRepository
import com.tinybill.domain.usecase.account.*
import com.tinybill.domain.usecase.transaction.*
import com.tinybill.presentation.viewmodel.AccountViewModel
import com.tinybill.presentation.viewmodel.CalendarViewModel
import com.tinybill.presentation.viewmodel.StatisticsViewModel
import com.tinybill.presentation.viewmodel.TransactionListViewModel
import com.tinybill.data.repository.BudgetRepository
import com.tinybill.data.repository.CustomCategoryPrefsRepository
import com.tinybill.data.repository.TemplateRepository
import com.tinybill.util.CategoryKeywordManager
import com.tinybill.util.CrashReporter
import com.tinybill.util.SettingsManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().scheduledTransactionDao() }
    single { get<AppDatabase>().templateDao() }
    single { get<AppDatabase>().accountDao() }
}

val repositoryModule = module {
    single { TransactionRepository(get()) }
    single<IAccountRepository> { AccountRepositoryImpl(get()) }
    single { com.tinybill.data.repository.ScheduledTransactionRepository(get()) }
    single { TemplateRepository(get()) }
    // 配置类 Repository：基于 DataStore，替代旧的 SharedPrefs-based Manager
    single { BudgetRepository(androidContext()) }
    single { CustomCategoryPrefsRepository(androidContext()) }
}

val useCaseModule = module {
    // Transaction UseCases：只保留有业务规则的。
    // 透传 UseCase（Delete/Restore/GetTransactions/GetCategorySuggestions）已删除，
    // ViewModel 直接调 Repository。
    single { AddTransactionUseCase(get()) }
    single { UpdateTransactionUseCase(get()) }
    single { SearchTransactionsUseCase(get()) }

    // Account UseCases：只保留有业务规则的。透传 UseCase（Get/Update/Delete）已删除，
    // ViewModel 直接调 Repository，与 Transaction 侧策略保持一致。
    single { AddAccountUseCase(get()) }
    single { GetAccountSummaryUseCase(get()) }
}

val managerModule = module {
    // 2026-06：旧的 BudgetManager / CustomCategoryManager 已删除，配置类读写全走
    // BudgetRepository / CustomCategoryPrefsRepository（基于 DataStore + Flow）。
    single { SettingsManager.getInstance(androidContext()) }
    single { CategoryKeywordManager.getInstance(androidContext()) }
    single { CrashReporter.getInstance(androidContext()) }
}

val viewModelModule = module {
    viewModel {
        TransactionListViewModel(
            addTransactionUseCase = get(),
            updateTransactionUseCase = get(),
            searchTransactionsUseCase = get(),
            repository = get(),
        )
    }

    viewModel {
        AccountViewModel(
            addAccountUseCase = get(),
            getAccountSummaryUseCase = get(),
            repository = get()
        )
    }

    viewModel {
        CalendarViewModel(
            repository = get(),
            budgetRepository = get()
        )
    }

    viewModel {
        StatisticsViewModel(
            repository = get(),
            budgetRepository = get()
        )
    }
}

val appModules = listOf(
    databaseModule,
    repositoryModule,
    useCaseModule,
    managerModule,
    viewModelModule
)
