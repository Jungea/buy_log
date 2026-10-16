package com.example.buylog.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.buylog.MainViewModel
import com.example.buylog.NotificationCaptureSettings
import com.example.buylog.data.NotificationEventEntity
import com.example.buylog.data.PaymentDraft
import com.example.buylog.data.PaymentEntity
import com.example.buylog.data.PaymentMethodEntity
import com.example.buylog.data.PaymentWithMethod
import com.example.buylog.domain.PaymentSummary
import com.example.buylog.domain.PaymentParseStatus
import com.example.buylog.domain.CandidateRegistrationStatus
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val inputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val displayDateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
private val debugDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val moneyFormatter = NumberFormat.getNumberInstance(Locale.KOREA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyLogApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationEvents by viewModel.notificationEvents.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationAccessEnabled = rememberNotificationAccessEnabled()
    var notificationCaptureEnabled by remember {
        mutableStateOf(NotificationCaptureSettings.isEnabled(context))
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<PaymentWithMethod?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showMethodManager by remember { mutableStateOf(false) }
    var showNotificationLog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PaymentEntity?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messageEvents.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("결제 기록") },
                actions = {
                    TextButton(onClick = { showNotificationLog = true }) {
                        Text("알림 로그")
                    }
                    TextButton(onClick = { showMethodManager = true }) {
                        Text("결제수단 관리")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditor = true
            }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSummaryCard(state.summary) }
            item {
                Text(
                    text = "최근 결제",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.payments.isEmpty()) {
                item { EmptyPayments() }
            } else {
                items(state.payments, key = { it.payment.id }) { item ->
                    PaymentRow(
                        item = item,
                        onEdit = {
                            editing = item
                            showEditor = true
                        },
                        onDelete = { deleting = item.payment },
                    )
                }
            }
        }
    }

    if (showEditor) {
        PaymentEditorDialog(
            existing = editing,
            methods = state.methods,
            onDismiss = { showEditor = false },
            onSave = { draft ->
                viewModel.save(draft, editing?.payment)
                showEditor = false
            },
        )
    }

    if (showMethodManager) {
        PaymentMethodManagerDialog(
            methods = state.methods,
            onDismiss = { showMethodManager = false },
            onAddMethod = viewModel::addPaymentMethod,
        )
    }

    if (showNotificationLog) {
        NotificationLogDialog(
            events = notificationEvents,
            payments = state.payments,
            notificationAccessEnabled = notificationAccessEnabled,
            notificationCaptureEnabled = notificationCaptureEnabled,
            onCaptureEnabledChange = { enabled ->
                NotificationCaptureSettings.setEnabled(context, enabled)
                notificationCaptureEnabled = enabled
            },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            onConfirmDuplicateAsSame = viewModel::confirmDuplicateAsSame,
            onConfirmDuplicateAsSeparate = viewModel::confirmDuplicateAsSeparate,
            onDismiss = { showNotificationLog = false },
        )
    }

    deleting?.let { payment ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("결제를 삭제할까요?") },
            text = { Text("삭제한 기록은 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(payment)
                    deleting = null
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun rememberNotificationAccessEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(context.isNotificationAccessEnabled()) }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = context.isNotificationAccessEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

@Composable
private fun NotificationLogDialog(
    events: List<NotificationEventEntity>,
    payments: List<PaymentWithMethod>,
    notificationAccessEnabled: Boolean,
    notificationCaptureEnabled: Boolean,
    onCaptureEnabledChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onConfirmDuplicateAsSame: (NotificationEventEntity) -> Unit,
    onConfirmDuplicateAsSeparate: (NotificationEventEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val candidateCount = events.count { it.parseStatus == PaymentParseStatus.CANDIDATE.name }
    val reviewCount = events.count { it.parseStatus == PaymentParseStatus.NEEDS_REVIEW.name }
    val duplicateCount = events.count { it.registrationStatus == CandidateRegistrationStatus.DUPLICATE_CANDIDATE.name }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 및 결제 후보") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notificationAccessEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    ),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if (notificationAccessEnabled) "알림 접근 권한이 켜져 있습니다."
                            else "알림 접근 권한이 꺼져 있습니다.",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "원문과 파싱 결과는 이 기기에만 저장되며, 아직 결제로 자동 등록하지 않습니다.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("원문 알림 수집", fontWeight = FontWeight.SemiBold)
                        Text(
                            "켜는 동안 받은 알림만 로그에 저장합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = notificationCaptureEnabled,
                        onCheckedChange = onCaptureEnabledChange,
                        enabled = notificationAccessEnabled,
                    )
                }
                if (events.isEmpty()) {
                    Text(
                        if (notificationCaptureEnabled) {
                            "수집된 알림이 없습니다. 금융 앱 또는 문자 앱 알림을 받은 다음 다시 확인해 주세요."
                        } else {
                            "원문 알림 수집을 켠 뒤 필요한 앱의 알림을 받아 보세요."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "결제 후보 ${candidateCount}건 · 파싱 확인 ${reviewCount}건 · 중복 확인 ${duplicateCount}건",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("최근 알림 ${events.size}건", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(events, key = { it.id }) { event ->
                            NotificationEventRow(
                                event = event,
                                suggestedPayment = payments.firstOrNull { it.payment.id == event.suggestedPaymentId },
                                onConfirmDuplicateAsSame = { onConfirmDuplicateAsSame(event) },
                                onConfirmDuplicateAsSeparate = { onConfirmDuplicateAsSeparate(event) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text("권한 설정")
            }
        },
    )
}

@Composable
private fun NotificationEventRow(
    event: NotificationEventEntity,
    suggestedPayment: PaymentWithMethod?,
    onConfirmDuplicateAsSame: () -> Unit,
    onConfirmDuplicateAsSeparate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.appName, fontWeight = FontWeight.SemiBold)
                Text(
                    event.receivedAt.asDebugDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (event.sourceType == "SMS_NOTIFICATION") "문자 앱 알림" else "앱 알림",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("패키지: ${event.packageName}", style = MaterialTheme.typography.labelSmall)
            when (event.parseStatus) {
                PaymentParseStatus.CANDIDATE.name -> {
                    Text(
                        "결제 후보 · ${event.parsedAmount?.asWon().orEmpty()}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    event.parsedMerchant?.let { Text("가맹점: $it", style = MaterialTheme.typography.bodySmall) }
                    event.parsedPaidAt?.let { Text("결제 시각: ${it.asDebugDate()}", style = MaterialTheme.typography.bodySmall) }
                    Text(
                        "결제수단: ${event.parsedPaymentMethod ?: "확인 필요"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (event.parseMessage.isNotBlank()) {
                        Text(event.parseMessage, style = MaterialTheme.typography.labelSmall)
                    }
                    when (event.registrationStatus) {
                        CandidateRegistrationStatus.REGISTERED.name -> Text(
                            if (event.duplicateConfidence == "HIGH") "같은 결제로 자동 연결됨"
                            else "결제로 자동 등록됨",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        CandidateRegistrationStatus.DUPLICATE_CANDIDATE.name -> {
                            Text(
                                "중복 가능성 확인 필요: ${suggestedPayment?.payment?.merchant ?: "기존 결제"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Row {
                                TextButton(onClick = onConfirmDuplicateAsSame) { Text("같은 결제") }
                                TextButton(onClick = onConfirmDuplicateAsSeparate) { Text("별도 결제") }
                            }
                        }
                        CandidateRegistrationStatus.NEEDS_PAYMENT_METHOD.name -> Text(
                            "결제수단을 읽지 못해 자동 등록하지 않았습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                PaymentParseStatus.NEEDS_REVIEW.name -> Text(
                    "파싱 확인 필요 · ${event.parseMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                PaymentParseStatus.NOT_PAYMENT.name -> Text(
                    "결제 알림 아님",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text("파싱 대기 중", style = MaterialTheme.typography.labelSmall)
            }
            if (event.title.isNotBlank()) {
                Text("제목: ${event.title}", style = MaterialTheme.typography.bodySmall)
            }
            Text("원문: ${event.rawText}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MonthSummaryCard(summary: PaymentSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("이번 달", style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary.totalAmount.asWon(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyPayments() {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("아직 기록된 결제가 없습니다.", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("+ 버튼으로 첫 결제를 기록해 보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaymentRow(
    item: PaymentWithMethod,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.payment.merchant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${item.payment.paidAt.asDisplayDate()} · ${item.paymentMethod.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(item.payment.amount.asWon(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text("수정") }
                TextButton(onClick = onDelete) { Text("삭제") }
            }
            if (item.payment.memo.isNotBlank()) {
                HorizontalDivider()
                Text(item.payment.memo, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PaymentEditorDialog(
    existing: PaymentWithMethod?,
    methods: List<PaymentMethodEntity>,
    onDismiss: () -> Unit,
    onSave: (PaymentDraft) -> Unit,
) {
    var amount by rememberSaveable(existing?.payment?.id) {
        mutableStateOf(existing?.payment?.amount?.toString().orEmpty())
    }
    var merchant by rememberSaveable(existing?.payment?.id) {
        mutableStateOf(existing?.payment?.merchant.orEmpty())
    }
    var paidAt by rememberSaveable(existing?.payment?.id) {
        mutableStateOf((existing?.payment?.paidAt ?: System.currentTimeMillis()).asInputDate())
    }
    var methodId by rememberSaveable(existing?.payment?.id) {
        mutableStateOf(existing?.paymentMethod?.id ?: methods.firstOrNull()?.id)
    }
    var memo by rememberSaveable(existing?.payment?.id) {
        mutableStateOf(existing?.payment?.memo.orEmpty())
    }
    var methodMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(methods) {
        if (methodId == null) methodId = methods.firstOrNull()?.id
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "결제 등록" else "결제 수정") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter(Char::isDigit) },
                        label = { Text("금액 (원)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("사용처") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = paidAt,
                        onValueChange = { paidAt = it },
                        label = { Text("결제 일시") },
                        supportingText = { Text("예: 2026-10-15 12:31") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Column {
                        Text("결제수단", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        OutlinedCard(
                            onClick = {
                                if (methods.isNotEmpty()) methodMenuExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "선택된 결제수단",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        methods.firstOrNull { it.id == methodId }?.name ?: "선택해 주세요",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Text(
                                    if (methods.isEmpty()) "없음" else "변경 ▼",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = methodMenuExpanded,
                            onDismissRequest = { methodMenuExpanded = false },
                        ) {
                            methods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name) },
                                    onClick = {
                                        methodId = method.id
                                        methodMenuExpanded = false
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (methods.isEmpty()) {
                                "홈 상단의 결제수단 관리에서 먼저 추가해 주세요."
                            } else {
                                "이 영역을 눌러 결제수단을 변경할 수 있어요."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = memo,
                        onValueChange = { memo = it },
                        label = { Text("메모 (선택)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
                error?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = amount.toLongOrNull()
                val parsedPaidAt = paidAt.toEpochMillisOrNull()
                error = when {
                    parsedAmount == null || parsedAmount <= 0 -> "0원보다 큰 금액을 입력해 주세요."
                    merchant.isBlank() -> "사용처를 입력해 주세요."
                    parsedPaidAt == null -> "날짜를 yyyy-MM-dd HH:mm 형식으로 입력해 주세요."
                    methodId == null -> "결제수단을 선택하거나 추가해 주세요."
                    else -> null
                }
                if (error == null) {
                    onSave(
                        PaymentDraft(
                            amount = parsedAmount!!,
                            merchant = merchant,
                            paidAt = parsedPaidAt!!,
                            paymentMethodId = methodId!!,
                            memo = memo,
                        ),
                    )
                }
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun PaymentMethodManagerDialog(
    methods: List<PaymentMethodEntity>,
    onDismiss: () -> Unit,
    onAddMethod: (String) -> Unit,
) {
    var newMethod by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("결제수단 관리") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        "등록된 결제수단",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (methods.isEmpty()) {
                    item {
                        Text(
                            "아직 등록된 결제수단이 없습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(methods, key = { it.id }) { method ->
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                method.name,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                item {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    OutlinedTextField(
                        value = newMethod,
                        onValueChange = {
                            newMethod = it
                            error = null
                        },
                        label = { Text("새 결제수단 이름") },
                        placeholder = { Text("예: 신한카드, 현금") },
                        singleLine = true,
                        isError = error != null,
                        supportingText = error?.let { message -> { Text(message) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newMethod.isBlank()) {
                    error = "결제수단 이름을 입력해 주세요."
                } else {
                    onAddMethod(newMethod)
                    newMethod = ""
                }
            }) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

private fun Long.asWon(): String = "${moneyFormatter.format(this)}원"

private fun Long.asDisplayDate(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(displayDateFormatter)

private fun Long.asDebugDate(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(debugDateFormatter)

private fun Long.asInputDate(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(inputDateFormatter)

private fun String.toEpochMillisOrNull(): Long? = try {
    LocalDateTime.parse(trim(), inputDateFormatter)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}

private fun Context.isNotificationAccessEnabled(): Boolean =
    Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        ?.split(':')
        ?.any { value -> ComponentName.unflattenFromString(value)?.packageName == packageName }
        ?: false
