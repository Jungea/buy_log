package com.example.buylog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.buylog.data.PaymentDraft
import com.example.buylog.data.PaymentEntity
import com.example.buylog.data.PaymentMethodEntity
import com.example.buylog.data.PaymentWithMethod
import com.example.buylog.data.NotificationEventEntity
import com.example.buylog.data.TagEntity
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
    val tags: List<TagEntity> = emptyList(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BuyLogApplication).repository
    private val messages = Channel<String>(Channel.BUFFERED)
    val messageEvents = messages.receiveAsFlow()

    val notificationEvents = repository.observeNotificationEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<NotificationEventEntity>())

    val uiState = combine(
        repository.observeAllPayments(),
        repository.observeCurrentMonthPayments(),
        repository.observePaymentMethods(),
        repository.observeTags(),
    ) { allPayments, monthlyPayments, methods, tags ->
        MainUiState(
            payments = allPayments,
            methods = methods,
            summary = PaymentSummaryCalculator.calculate(monthlyPayments),
            tags = tags,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            runCatching { repository.parseStoredNotificationEvents() }
                .onFailure { messages.send("기존 알림의 파싱 결과를 갱신하지 못했습니다.") }
            runCatching { repository.processPendingCandidates() }
                .onFailure { messages.send("결제 후보를 자동 등록하지 못했습니다.") }
        }
    }

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

    fun addTag(name: String, exclude: Boolean) {
        viewModelScope.launch {
            runCatching { repository.addTag(name, exclude) }
                .onFailure { messages.send(it.message ?: "태그를 추가하지 못했습니다.") }
        }
    }

    fun updateTag(tag: TagEntity, name: String, exclude: Boolean) {
        viewModelScope.launch {
            runCatching { repository.updateTag(tag, name, exclude) }
                .onFailure { messages.send(it.message ?: "태그를 수정하지 못했습니다.") }
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch { runCatching { repository.deleteTag(tag) } }
    }

    fun confirmDuplicateAsSame(event: NotificationEventEntity) {
        viewModelScope.launch {
            runCatching { repository.confirmDuplicateAsSame(event) }
                .onSuccess { messages.send("기존 결제에 알림 출처를 연결했습니다.") }
                .onFailure { messages.send(it.message ?: "중복 후보를 처리하지 못했습니다.") }
        }
    }

    fun confirmDuplicateAsSeparate(event: NotificationEventEntity) {
        viewModelScope.launch {
            runCatching { repository.confirmDuplicateAsSeparate(event) }
                .onSuccess { messages.send("별도 결제로 등록했습니다.") }
                .onFailure { messages.send(it.message ?: "결제를 등록하지 못했습니다.") }
        }
    }
}
