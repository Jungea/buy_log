package com.example.buylog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.buylog.data.PaymentDraft
import com.example.buylog.data.PaymentEntity
import com.example.buylog.data.PaymentMethodEntity
import com.example.buylog.data.PaymentWithMethod
import com.example.buylog.domain.PaymentSummary
import com.example.buylog.domain.PaymentSummaryCalculator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val payments: List<PaymentWithMethod> = emptyList(),
    val methods: List<PaymentMethodEntity> = emptyList(),
    val summary: PaymentSummary = PaymentSummary(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BuyLogApplication).repository
    private val messages = Channel<String>(Channel.BUFFERED)
    val messageEvents = messages.receiveAsFlow()

    val uiState = combine(
        repository.observeAllPayments(),
        repository.observeCurrentMonthPayments(),
        repository.observePaymentMethods(),
    ) { allPayments, monthlyPayments, methods ->
        MainUiState(
            payments = allPayments,
            methods = methods,
            summary = PaymentSummaryCalculator.calculate(monthlyPayments),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun save(draft: PaymentDraft, existing: PaymentEntity?) {
        viewModelScope.launch {
            runCatching { repository.savePayment(draft, existing) }
                .onFailure { messages.send(it.message ?: "결제를 저장하지 못했습니다.") }
        }
    }

    fun delete(payment: PaymentEntity) {
        viewModelScope.launch {
            runCatching { repository.deletePayment(payment) }
                .onFailure { messages.send(it.message ?: "결제를 삭제하지 못했습니다.") }
        }
    }

    fun addPaymentMethod(name: String) {
        viewModelScope.launch {
            runCatching { repository.addPaymentMethod(name) }
                .onSuccess { id ->
                    if (id == -1L) messages.send("이미 같은 이름의 결제수단이 있습니다.")
                }
                .onFailure { messages.send(it.message ?: "결제수단을 추가하지 못했습니다.") }
        }
    }
}
