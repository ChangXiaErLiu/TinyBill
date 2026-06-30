package com.tinybill.domain.usecase.transaction

import com.tinybill.data.entity.Transaction

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * 搜索账单的 UseCase。
 *
 * 保留理由：内含 debounce(300) + flatMapLatest 防抖逻辑，ViewModel 直接
 * 调 Repository 会丢失这个能力。
 */
class SearchTransactionsUseCase(
    private val repository: com.tinybill.data.repository.TransactionRepository
) {
    @OptIn(FlowPreview::class)
    operator fun invoke(query: Flow<String>): Flow<List<Transaction>> {
        return query
            .debounce(300) // 300ms防抖
            .flatMapLatest { keyword ->
                if (keyword.isBlank()) {
                    flow { emit(emptyList()) }
                } else {
                    repository.searchTransactionsFlow(keyword)
                }
            }
    }
}
