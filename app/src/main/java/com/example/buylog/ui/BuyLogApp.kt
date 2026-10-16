package com.example.buylog.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.buylog.MainViewModel
import com.example.buylog.NotificationCaptureSettings
import com.example.buylog.MissedPaymentPreferences
import com.example.buylog.SpendingPreferences
import com.example.buylog.data.NotificationEventEntity
import com.example.buylog.data.PaymentDraft
import com.example.buylog.data.PaymentEntity
import com.example.buylog.data.PaymentMethodEntity
import com.example.buylog.data.PaymentWithMethod
import com.example.buylog.data.TagEntity
import com.example.buylog.data.AutoTagRuleEntity
import com.example.buylog.data.UNCLASSIFIED_PAYMENT_METHOD_NAME
import com.example.buylog.domain.PaymentSummary
import com.example.buylog.domain.PaymentParseStatus
import com.example.buylog.domain.CandidateRegistrationStatus
import com.example.buylog.domain.PaymentSummaryCalculator
import com.example.buylog.domain.SpendingPeriodCalculator
import com.example.buylog.domain.CardPeriodCalculator
import com.example.buylog.domain.CardPerformanceCalculator
import com.example.buylog.domain.PaymentPatternAnalyzer
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.YearMonth
import java.time.Clock
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val inputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val displayDateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
private val debugDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val patternDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val moneyFormatter = NumberFormat.getNumberInstance(Locale.KOREA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyLogApp(viewModel: MainViewModel, addPaymentRequest: Int = 0) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationEvents by viewModel.notificationEvents.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationAccessEnabled = rememberNotificationAccessEnabled()
    var notificationCaptureEnabled by remember {
        mutableStateOf(NotificationCaptureSettings.isEnabled(context))
    }
    var missedPaymentNotificationsEnabled by remember {
        mutableStateOf(MissedPaymentPreferences.isEnabled(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        missedPaymentNotificationsEnabled = granted
        MissedPaymentPreferences.setEnabled(context, granted)
    }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(viewModel::exportBackup) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingRestoreUri = uri }
    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<PaymentWithMethod?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showMethodManager by remember { mutableStateOf(false) }
    var showNotificationLog by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }
    var showManagementMenu by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PaymentEntity?>(null) }
    var deletingMethod by remember { mutableStateOf<PaymentMethodEntity?>(null) }
    var page by rememberSaveable { mutableStateOf(0) }
    var salaryMode by rememberSaveable { mutableStateOf(false) }
    var periodOffset by rememberSaveable { mutableStateOf(0L) }
    var payday by rememberSaveable { mutableStateOf(SpendingPreferences.getPayday(context).toString()) }
    var moveWeekendPayday by rememberSaveable { mutableStateOf(SpendingPreferences.getMoveWeekendPayday(context)) }
    var paydayOverrides by remember { mutableStateOf(SpendingPreferences.getPaydayOverrides(context)) }
    var overrideMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var overrideDate by rememberSaveable { mutableStateOf("") }
    var overrideError by remember { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedTagId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedMethodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exclusionFilter by rememberSaveable { mutableStateOf(0) }
    var customFrom by rememberSaveable { mutableStateOf("") }
    var customTo by rememberSaveable { mutableStateOf("") }
    var selectedCardId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cardPeriodOffset by rememberSaveable { mutableStateOf(0L) }
    val zoneId = ZoneId.systemDefault()
    val spendingPage = page == 0 || page == 1
    val period = if (salaryMode) {
        SpendingPeriodCalculator.salaryCycle(
            Clock.systemDefaultZone().instant(),
            payday.toIntOrNull()?.coerceIn(1, 31) ?: 25,
            periodOffset,
            zoneId,
            moveWeekendPayday,
            paydayOverrides,
        )
    } else {
        SpendingPeriodCalculator.calendarMonth(YearMonth.now(zoneId).plusMonths(periodOffset), zoneId)
    }
    val customFromMillis = customFrom.takeIf(String::isNotBlank)?.let { "$it 00:00".toEpochMillisOrNull() }
    val customToMillis = customTo.takeIf(String::isNotBlank)?.let { "$it 23:59".toEpochMillisOrNull()?.plus(60_000) }
    val effectiveFrom = if (page == 1) customFromMillis ?: period.fromInclusive else period.fromInclusive
    val effectiveTo = if (page == 1) customToMillis ?: period.toExclusive else period.toExclusive
    val periodPayments = state.payments.filter { it.payment.paidAt in effectiveFrom until effectiveTo }
    val filteredPayments = periodPayments.filter { item ->
        (search.isBlank() || item.payment.merchant.contains(search, ignoreCase = true)) &&
            (selectedTagId == null || item.tags.any { it.id == selectedTagId }) &&
            (selectedMethodId == null || item.paymentMethod.id == selectedMethodId) &&
            when (exclusionFilter) {
                1 -> item.tags.none { it.excludeFromSpending }
                2 -> item.tags.any { it.excludeFromSpending }
                else -> true
            }
    }
    val periodSummary = PaymentSummaryCalculator.calculate(periodPayments)
    val cardMethods = state.methods.filter { it.isCard }
    val selectedCard = cardMethods.firstOrNull { it.id == selectedCardId } ?: cardMethods.firstOrNull()
    val cardPeriod = selectedCard?.let { CardPeriodCalculator.containing(Clock.systemDefaultZone().instant(), it.periodStartDay, cardPeriodOffset, zoneId) }
    val cardPayments = if (selectedCard == null || cardPeriod == null) emptyList() else state.payments.filter {
        it.paymentMethod.id == selectedCard.id && it.payment.paidAt in cardPeriod.fromInclusive until cardPeriod.toExclusive
    }
    val cardPerformance = CardPerformanceCalculator.calculate(cardPayments, selectedCard?.performanceTarget ?: 0)
    val patternReport = PaymentPatternAnalyzer.analyze(
        paidAtValues = state.payments.map { it.payment.paidAt },
        now = Clock.systemDefaultZone().instant(),
        zoneId = zoneId,
    )

    LaunchedEffect(viewModel) {
        viewModel.messageEvents.collectLatest { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(addPaymentRequest) {
        if (addPaymentRequest > 0) {
            editing = null
            showEditor = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("결제 기록") },
                actions = {
                    TextButton(onClick = { showManagementMenu = true }) { Text("관리") }
                    DropdownMenu(expanded = showManagementMenu, onDismissRequest = { showManagementMenu = false }) {
                        DropdownMenuItem(text = { Text("알림 로그") }, onClick = { showManagementMenu = false; showNotificationLog = true })
                        DropdownMenuItem(text = { Text("결제수단 관리") }, onClick = { showManagementMenu = false; showMethodManager = true })
                        DropdownMenuItem(text = { Text("태그 관리") }, onClick = { showManagementMenu = false; showTagManager = true })
                        DropdownMenuItem(text = { Text("백업 및 복원") }, onClick = { showManagementMenu = false; showBackupDialog = true })
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = page == 0, onClick = { page = 0 }, icon = { Text("월간") })
                NavigationBarItem(selected = page == 1, onClick = { page = 1 }, icon = { Text("결제 내역") })
                NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Text("카드") })
                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Text("패턴") })
            }
        },
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
            if (spendingPage) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !salaryMode, onClick = { salaryMode = false; periodOffset = 0 }, label = { Text("달력 월") })
                    FilterChip(selected = salaryMode, onClick = { salaryMode = true; periodOffset = 0 }, label = { Text("월급 주기") })
                }
            }
            if (spendingPage) item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { periodOffset-- }) { Text("이전") }
                    Text(period.label, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { periodOffset++ }) { Text("다음") }
                }
            }
            if (spendingPage && salaryMode) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = payday,
                        onValueChange = {
                            payday = it.filter(Char::isDigit).take(2)
                            payday.toIntOrNull()?.takeIf { day -> day in 1..31 }
                                ?.let { day -> SpendingPreferences.setPayday(context, day) }
                        },
                        label = { Text("월급일 (1~31)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("주말이면 직전 평일로 조정", fontWeight = FontWeight.SemiBold)
                            Text("토요일·일요일만 적용하며 공휴일은 자동 조정하지 않아요.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = moveWeekendPayday,
                            onCheckedChange = { enabled ->
                                moveWeekendPayday = enabled
                                SpendingPreferences.setMoveWeekendPayday(context, enabled)
                                periodOffset = 0
                            },
                        )
                    }
                    HorizontalDivider()
                    Text("특정 달 실제 월급일", fontWeight = FontWeight.SemiBold)
                    Text(
                        "회사 휴무 등으로 입금일이 달라진 달만 지정하세요. 직접 지정일이 주말 보정보다 우선합니다.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = overrideMonth,
                        onValueChange = { overrideMonth = it; overrideError = null },
                        label = { Text("대상 월 (yyyy-MM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = overrideDate,
                        onValueChange = { overrideDate = it; overrideError = null },
                        label = { Text("실제 월급일 (yyyy-MM-dd)") },
                        singleLine = true,
                        isError = overrideError != null,
                        supportingText = overrideError?.let { message -> { Text(message) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = {
                        val month = runCatching { YearMonth.parse(overrideMonth) }.getOrNull()
                        val date = runCatching { LocalDate.parse(overrideDate) }.getOrNull()
                        overrideError = when {
                            month == null -> "대상 월을 yyyy-MM 형식으로 입력해 주세요."
                            date == null -> "실제 월급일을 yyyy-MM-dd 형식으로 입력해 주세요."
                            YearMonth.from(date) != month -> "실제 월급일은 대상 월 안의 날짜여야 합니다."
                            else -> null
                        }
                        if (overrideError == null) {
                            SpendingPreferences.setPaydayOverride(context, month!!, date!!)
                            paydayOverrides = SpendingPreferences.getPaydayOverrides(context)
                            overrideDate = ""
                            periodOffset = 0
                        }
                    }) { Text("지정일 저장") }
                    paydayOverrides.entries.sortedByDescending { it.key }.forEach { (month, date) ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("$month · $date", Modifier.weight(1f))
                                TextButton(onClick = {
                                    SpendingPreferences.removePaydayOverride(context, month)
                                    paydayOverrides = SpendingPreferences.getPaydayOverrides(context)
                                    periodOffset = 0
                                }) { Text("삭제") }
                            }
                        }
                    }
                }
            }
            if (spendingPage) item { MonthSummaryCard(periodSummary) }
            if (page == 1) {
                item { OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("가맹점 검색") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = customFrom, onValueChange = { customFrom = it }, label = { Text("시작일 yyyy-MM-dd") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = customTo, onValueChange = { customTo = it }, label = { Text("종료일 yyyy-MM-dd") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = exclusionFilter == 0, onClick = { exclusionFilter = 0 }, label = { Text("전체") })
                        FilterChip(selected = exclusionFilter == 1, onClick = { exclusionFilter = 1 }, label = { Text("집계 포함") })
                        FilterChip(selected = exclusionFilter == 2, onClick = { exclusionFilter = 2 }, label = { Text("집계 제외") })
                    }
                }
                item {
                    LazyColumn(Modifier.height(100.dp)) {
                        item { FilterChip(selected = selectedMethodId == null, onClick = { selectedMethodId = null }, label = { Text("모든 결제수단") }) }
                        items(state.methods) { method -> FilterChip(selected = selectedMethodId == method.id, onClick = { selectedMethodId = method.id }, label = { Text(method.name) }) }
                    }
                }
                item {
                    LazyColumn(Modifier.height(100.dp)) {
                        item { FilterChip(selected = selectedTagId == null, onClick = { selectedTagId = null }, label = { Text("모든 태그") }) }
                        items(state.tags) { tag -> FilterChip(selected = selectedTagId == tag.id, onClick = { selectedTagId = tag.id }, label = { Text(tag.name) }) }
                    }
                }
                item { Text("필터 결과 ${filteredPayments.size}건 · ${filteredPayments.sumOf { it.payment.amount }.asWon()}", fontWeight = FontWeight.SemiBold) }
            }
            if (page == 0 && periodPayments.isNotEmpty()) {
                item {
                    val merchantTotals = periodPayments.groupBy { it.payment.merchant }.mapValues { it.value.sumOf { row -> row.payment.amount } }.entries.sortedByDescending { it.value }.take(5)
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                        Text("많이 사용한 곳", fontWeight = FontWeight.Bold)
                        merchantTotals.forEach { Text("${it.key}  ${it.value.asWon()}") }
                    } }
                }
                item {
                    val methodTotals = periodPayments.groupBy { it.paymentMethod.name }.mapValues { it.value.sumOf { row -> row.payment.amount } }.entries.sortedByDescending { it.value }
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                        Text("결제수단별", fontWeight = FontWeight.Bold)
                        methodTotals.forEach { Text("${it.key}  ${it.value.asWon()}") }
                    } }
                }
            }
            if (page == 2) {
                if (cardMethods.isEmpty()) {
                    item { Text("결제수단 관리에서 카드를 지정해 주세요.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    item {
                        LazyColumn(Modifier.height(120.dp)) {
                            items(cardMethods) { method ->
                                FilterChip(
                                    selected = selectedCard?.id == method.id,
                                    onClick = { selectedCardId = method.id; cardPeriodOffset = 0 },
                                    label = { Text(if (method.cardCompany.isBlank()) method.name else "${method.cardCompany} · ${method.name}") },
                                )
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { cardPeriodOffset-- }) { Text("이전") }
                            Text(cardPeriod?.label.orEmpty(), fontWeight = FontWeight.Bold)
                            TextButton(onClick = { cardPeriodOffset++ }) { Text("다음") }
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(selectedCard?.name.orEmpty(), fontWeight = FontWeight.Bold)
                            Text("기간 전체 ${cardPerformance.total.asWon()}")
                            Text("실적 ${cardPerformance.included.asWon()} / ${cardPerformance.target.asWon()}")
                            Text("남음 ${cardPerformance.remaining.asWon()} · 진행률 ${cardPerformance.progressPercent}%")
                        } }
                    }
                }
            }
            if (page == 3) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("결제 누락 알림", fontWeight = FontWeight.Bold)
                                Text("강한 패턴 시간대에 기록이 없을 때 하루 한 번만 확인합니다.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = missedPaymentNotificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        missedPaymentNotificationsEnabled = enabled
                                        MissedPaymentPreferences.setEnabled(context, enabled)
                                    }
                                },
                            )
                        }
                    }
                }
                item {
                    Text("결제 발생 패턴", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "최근 최대 8주의 완료된 날짜를 2시간 단위로 분석합니다. 오늘 기록은 아직 하루가 끝나지 않아 제외합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!patternReport.hasSufficientData) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("아직 분석할 데이터가 부족해요", fontWeight = FontWeight.Bold)
                                Text("같은 요일의 완료된 기록이 최소 ${patternReport.minimumObservedDays}일 쌓이면 패턴을 보여줍니다.")
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "분석 기간 ${patternReport.from?.format(patternDateFormatter)} ~ ${patternReport.through?.format(patternDateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    items(patternReport.weekdays, key = { it.dayOfWeek }) { weekday ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("${weekday.dayOfWeek.koreanName()} · 관찰 ${weekday.observedDays}일", fontWeight = FontWeight.Bold)
                                if (weekday.patterns.isEmpty()) {
                                    Text("50% 이상 반복된 시간대가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    weekday.patterns.forEach { pattern ->
                                        Text(
                                            "%02d:00~%02d:00  %d%% (%d/%d일)".format(
                                                pattern.startHour,
                                                pattern.endHour,
                                                pattern.probabilityPercent,
                                                pattern.occurrenceDays,
                                                weekday.observedDays,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "통계 화면은 관찰일 4일 이상·발생률 50% 이상을 표시합니다. 누락 알림은 더 보수적으로 관찰일 6일 이상·발생률 70% 이상일 때만 동작합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = if (page == 2) "카드 사용 이력" else "최근 결제",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                val visiblePayments = when (page) { 0 -> periodPayments; 1 -> filteredPayments; else -> cardPayments }
                if (visiblePayments.isEmpty()) {
                    item { EmptyPayments() }
                } else {
                    items(visiblePayments, key = { it.payment.id }) { item ->
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
    }

    if (showEditor) {
        PaymentEditorDialog(
            existing = editing,
            methods = state.methods,
            tags = state.tags,
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
            onUpdateCardSettings = viewModel::updateCardSettings,
            onDeleteMethod = { method ->
                showMethodManager = false
                deletingMethod = method
            },
        )
    }

    if (showTagManager) {
        TagManagerDialog(
            tags = state.tags,
            rules = state.autoTagRules,
            onDismiss = { showTagManager = false },
            onAdd = viewModel::addTag,
            onUpdate = viewModel::updateTag,
            onDelete = viewModel::deleteTag,
            onAddRule = viewModel::addAutoTagRule,
            onDeleteRule = viewModel::deleteAutoTagRule,
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("백업 및 복원") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("결제, 결제수단, 태그, 자동 규칙, 알림 원문과 누락 응답을 하나의 로컬 파일로 저장합니다.")
                    Text("백업 파일에는 금융 알림 원문이 포함될 수 있으므로 안전한 위치에 보관해 주세요.", color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = {
                            exportBackupLauncher.launch("buy-log-${LocalDate.now()}.buylog")
                            showBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("백업 파일 만들기") }
                    Button(
                        onClick = {
                            restoreBackupLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
                            showBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("백업 파일 복원") }
                }
            },
            confirmButton = { TextButton(onClick = { showBackupDialog = false }) { Text("닫기") } },
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("백업을 복원할까요?") },
            text = { Text("현재 기기의 모든 BuyLog 데이터를 선택한 백업 내용으로 교체합니다. 복원 전에 현재 데이터도 백업해 두는 것을 권장합니다.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.restoreBackup(uri)
                    pendingRestoreUri = null
                }) { Text("복원") }
            },
            dismissButton = { TextButton(onClick = { pendingRestoreUri = null }) { Text("취소") } },
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

    deletingMethod?.let { method ->
        val paymentCount = state.payments.count { it.payment.paymentMethodId == method.id }
        val replacementCandidates = state.methods.filter { it.id != method.id }
        var replacementMethodId by rememberSaveable(method.id) {
            mutableStateOf(replacementCandidates.firstOrNull { it.name == UNCLASSIFIED_PAYMENT_METHOD_NAME }?.id)
        }
        var replacementMenuExpanded by remember(method.id) { mutableStateOf(false) }
        LaunchedEffect(replacementCandidates, method.id) {
            if (replacementCandidates.none { it.id == replacementMethodId }) {
                replacementMethodId = replacementCandidates.firstOrNull { it.name == UNCLASSIFIED_PAYMENT_METHOD_NAME }?.id
                    ?: replacementCandidates.firstOrNull()?.id
            }
        }
        val replacement = replacementCandidates.firstOrNull { it.id == replacementMethodId }
        AlertDialog(
            onDismissRequest = { deletingMethod = null },
            title = { Text("${method.name}을 삭제할까요?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("연결된 결제 ${paymentCount}건은 삭제하지 않고 아래 결제수단으로 변경됩니다.")
                    OutlinedCard(
                        onClick = { replacementMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("이동할 결제수단", style = MaterialTheme.typography.labelSmall)
                                Text(replacement?.name ?: "선택해 주세요", fontWeight = FontWeight.SemiBold)
                            }
                            Text("변경 ▼", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    DropdownMenu(
                        expanded = replacementMenuExpanded,
                        onDismissRequest = { replacementMenuExpanded = false },
                    ) {
                        replacementCandidates.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate.name) },
                                onClick = {
                                    replacementMethodId = candidate.id
                                    replacementMenuExpanded = false
                                },
                            )
                        }
                    }
                    Text("${method.name}만 삭제되며 결제 기록과 태그는 유지됩니다.")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = replacement != null,
                    onClick = {
                        replacement?.let { viewModel.deletePaymentMethod(method, it) }
                        deletingMethod = null
                    },
                ) { Text("이동 후 삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingMethod = null }) { Text("취소") } },
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
            Text("선택 기간", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "실제 소비 ${summary.actualSpending.asWon()}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text("전체 ${summary.totalAmount.asWon()} · 제외 ${summary.excludedAmount.asWon()}")
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
            if (item.tags.isNotEmpty()) Text(item.tags.joinToString(" · ") { "#${it.name}" }, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PaymentEditorDialog(
    existing: PaymentWithMethod?,
    methods: List<PaymentMethodEntity>,
    tags: List<TagEntity>,
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
    var selectedTagIds by rememberSaveable(existing?.payment?.id) {
        mutableStateOf(existing?.tags?.map { it.id }?.toSet() ?: emptySet())
    }
    var performanceIncluded by rememberSaveable(existing?.payment?.id) {
        mutableStateOf(existing?.payment?.performanceIncluded ?: true)
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("카드 실적 포함", fontWeight = FontWeight.SemiBold)
                            Text("소비 집계 제외 태그와 별도로 적용됩니다.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = performanceIncluded, onCheckedChange = { performanceIncluded = it })
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("태그", style = MaterialTheme.typography.labelLarge)
                        if (tags.isEmpty()) Text("상단 태그 관리에서 태그를 추가해 주세요.", style = MaterialTheme.typography.bodySmall)
                        tags.forEach { tag ->
                            FilterChip(
                                selected = tag.id in selectedTagIds,
                                onClick = { selectedTagIds = if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id },
                                label = { Text(if (tag.excludeFromSpending) "${tag.name} · 집계 제외" else tag.name) },
                            )
                        }
                    }
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
                            tagIds = selectedTagIds,
                            performanceIncluded = performanceIncluded,
                        ),
                    )
                }
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun TagManagerDialog(
    tags: List<TagEntity>,
    rules: List<AutoTagRuleEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, Boolean) -> Unit,
    onUpdate: (TagEntity, String, Boolean) -> Unit,
    onDelete: (TagEntity) -> Unit,
    onAddRule: (String, Long) -> Unit,
    onDeleteRule: (AutoTagRuleEntity) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var exclude by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TagEntity?>(null) }
    var rulePhrase by rememberSaveable { mutableStateOf("") }
    var ruleTagId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(tags) {
        if (tags.none { it.id == ruleTagId }) ruleTagId = tags.firstOrNull()?.id
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("태그 관리") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags, key = { it.id }) { tag ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tag.name, fontWeight = FontWeight.SemiBold)
                                Text(if (tag.excludeFromSpending) "소비 집계 제외" else "소비 집계 포함", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { editing = tag; name = tag.name; exclude = tag.excludeFromSpending }) { Text("수정") }
                            TextButton(onClick = { onDelete(tag) }) { Text("삭제") }
                        }
                    }
                }
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("태그 이름") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("소비 집계 제외", modifier = Modifier.weight(1f))
                        Switch(checked = exclude, onCheckedChange = { exclude = it })
                    }
                }
                item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
                item {
                    Text("자동 태그 규칙", fontWeight = FontWeight.Bold)
                    Text(
                        "사용처나 메모에 문구가 포함되면 선택한 태그를 자동으로 추가합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (rules.isEmpty()) {
                    item { Text("등록된 자동 규칙이 없습니다.", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(rules, key = { "rule-${it.id}" }) { rule ->
                        val tagName = tags.firstOrNull { it.id == rule.tagId }?.name ?: "삭제된 태그"
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("“${rule.phrase}” 포함", fontWeight = FontWeight.SemiBold)
                                    Text("→ #$tagName", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { onDeleteRule(rule) }) { Text("삭제") }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = rulePhrase,
                        onValueChange = { rulePhrase = it },
                        label = { Text("자동 적용할 문구") },
                        placeholder = { Text("예: 테스트몰, PG_TEST") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                if (tags.isEmpty()) {
                    item { Text("먼저 태그를 추가해야 자동 규칙을 만들 수 있습니다.", color = MaterialTheme.colorScheme.error) }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("적용할 태그", style = MaterialTheme.typography.labelMedium)
                            tags.forEach { tag ->
                                FilterChip(
                                    selected = ruleTagId == tag.id,
                                    onClick = { ruleTagId = tag.id },
                                    label = { Text(tag.name) },
                                )
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                ruleTagId?.let { onAddRule(rulePhrase, it) }
                                rulePhrase = ""
                            },
                            enabled = rulePhrase.isNotBlank() && ruleTagId != null,
                        ) { Text("자동 규칙 추가") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    editing?.let { onUpdate(it, name, exclude) } ?: onAdd(name, exclude)
                    editing = null; name = ""; exclude = false
                }
            }) { Text(if (editing == null) "추가" else "저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun PaymentMethodManagerDialog(
    methods: List<PaymentMethodEntity>,
    onDismiss: () -> Unit,
    onAddMethod: (String) -> Unit,
    onUpdateCardSettings: (PaymentMethodEntity, Boolean, String, Int, Long) -> Unit,
    onDeleteMethod: (PaymentMethodEntity) -> Unit,
) {
    var newMethod by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var editingMethod by remember { mutableStateOf<PaymentMethodEntity?>(null) }
    var isCard by remember { mutableStateOf(false) }
    var company by remember { mutableStateOf("") }
    var startDay by remember { mutableStateOf("1") }
    var target by remember { mutableStateOf("0") }

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
                    Text(
                        "신용카드와 체크카드는 설정에서 카드로 관리하고, 통장·계좌이체·현금은 카드 설정 없이 사용할 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(method.name, fontWeight = FontWeight.SemiBold)
                                    if (method.name == UNCLASSIFIED_PAYMENT_METHOD_NAME) {
                                        Text("첫 결제에 사용할 수 있는 기본 수단 · 삭제 불가", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (method.isCard) Text("${method.cardCompany} · 매월 ${method.periodStartDay}일 시작 · 목표 ${method.performanceTarget.asWon()}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (method.name != UNCLASSIFIED_PAYMENT_METHOD_NAME) {
                                    TextButton(onClick = {
                                        editingMethod = method; isCard = method.isCard; company = method.cardCompany
                                        startDay = method.periodStartDay.toString(); target = method.performanceTarget.toString()
                                    }) { Text("설정") }
                                    TextButton(onClick = { onDeleteMethod(method) }) {
                                        Text("삭제", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                editingMethod?.let { method ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${method.name} 설정", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) { Text("카드·체크카드로 관리", Modifier.weight(1f)); Switch(isCard, { isCard = it }) }
                            if (isCard) {
                                OutlinedTextField(company, { company = it }, label = { Text("카드사") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(startDay, { startDay = it.filter(Char::isDigit).take(2) }, label = { Text("계산 시작일 (1~31)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(target, { target = it.filter(Char::isDigit) }, label = { Text("실적 목표 (원)") }, modifier = Modifier.fillMaxWidth())
                            }
                            Button(onClick = {
                                onUpdateCardSettings(method, isCard, company, startDay.toIntOrNull() ?: 1, target.toLongOrNull() ?: 0)
                                editingMethod = null
                            }) { Text("설정 저장") }
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
                        placeholder = { Text("예: 체크카드, 국민은행 통장, 계좌이체, 현금") },
                        singleLine = true,
                        isError = error != null,
                        supportingText = error?.let { message -> { Text(message) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "계좌이체는 돈이 나간 통장 이름으로 등록하면 지출 경로를 더 정확히 확인할 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun DayOfWeek.koreanName(): String = when (this) {
    DayOfWeek.MONDAY -> "월요일"
    DayOfWeek.TUESDAY -> "화요일"
    DayOfWeek.WEDNESDAY -> "수요일"
    DayOfWeek.THURSDAY -> "목요일"
    DayOfWeek.FRIDAY -> "금요일"
    DayOfWeek.SATURDAY -> "토요일"
    DayOfWeek.SUNDAY -> "일요일"
}

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
