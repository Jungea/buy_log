package com.example.buylog

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.buylog.data.NotificationEventDraft
import com.example.buylog.data.NotificationSourceClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(notification: StatusBarNotification) {
        if (!NotificationCaptureSettings.isEnabled(this)) return
        val isQaNotification = notification.packageName == packageName &&
            notification.notification.channelId == QaPaymentNotificationSender.CHANNEL_ID
        if (notification.packageName == packageName && !isQaNotification) return

        val extras = notification.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val rawText = listOf(title, text, bigText)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(separator = "\n")

        if (rawText.isBlank()) return

        val appName = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(notification.packageName, 0),
            ).toString()
        }.getOrDefault(notification.packageName)

        val draft = NotificationEventDraft(
            sourceType = NotificationSourceClassifier.classify(notification.packageName).name,
            packageName = notification.packageName,
            appName = appName,
            title = title,
            content = text.ifBlank { bigText },
            rawText = rawText,
            receivedAt = notification.postTime,
        )

        serviceScope.launch {
            (application as BuyLogApplication).repository.recordNotificationEvent(
                draft = draft,
                recognizeAsPayment = NotificationRecognitionSettings.isSelected(
                    this@PaymentNotificationListenerService,
                    notification.packageName,
                ),
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
