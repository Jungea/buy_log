package com.example.buylog

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.buylog.data.BuyLogDatabase
import com.example.buylog.data.MissedPaymentCheckEntity
import com.example.buylog.domain.MissedPaymentEvaluator
import java.time.Clock
import java.time.ZoneId

class MissedPaymentNotificationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        if (!MissedPaymentPreferences.isEnabled(applicationContext)) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        if (!notificationManager.areNotificationsEnabled()) return Result.success()

        val database = BuyLogDatabase.getInstance(applicationContext)
        val paymentDao = database.paymentDao()
        val checkDao = database.missedPaymentCheckDao()
        val now = Clock.systemDefaultZone().instant()
        val zoneId = ZoneId.systemDefault()
        val todayEpochDay = now.atZone(zoneId).toLocalDate().toEpochDay()
        val candidate = MissedPaymentEvaluator.evaluate(
            paidAtValues = paymentDao.getAllPaidAt(),
            now = now,
            zoneId = zoneId,
            alreadyHandledToday = checkDao.countForDate(todayEpochDay) > 0,
        ) ?: return Result.success()

        val insertedId = checkDao.insert(
            MissedPaymentCheckEntity(
                dateEpochDay = candidate.dateEpochDay,
                startHour = candidate.startHour,
                endHour = candidate.endHour,
                probabilityPercent = candidate.probabilityPercent,
                status = STATUS_NOTIFIED,
                createdAt = now.toEpochMilli(),
            ),
        )
        if (insertedId == -1L) return Result.success()

        createChannel(notificationManager)
        val notificationId = notificationId(candidate.dateEpochDay)
        val noPaymentIntent = Intent(applicationContext, MissedPaymentResponseReceiver::class.java).apply {
            action = ACTION_NO_PAYMENT
            putExtra(EXTRA_DATE_EPOCH_DAY, candidate.dateEpochDay)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val addPaymentIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = ACTION_ADD_PAYMENT
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("결제 기록을 확인해 주세요")
            .setContentText(
                "%02d:00~%02d:00에 평소 결제가 있었지만 오늘 기록이 없어요.".format(
                    candidate.startHour,
                    candidate.endHour,
                ),
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText("혹시 기록하지 않은 결제가 있나요? 결제하지 않았다면 알려주거나 바로 입력할 수 있어요."))
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    notificationId,
                    addPaymentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                0,
                "결제 안 했어요",
                PendingIntent.getBroadcast(
                    applicationContext,
                    notificationId,
                    noPaymentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                0,
                "직접 입력",
                PendingIntent.getActivity(
                    applicationContext,
                    notificationId + 1,
                    addPaymentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(notificationId, notification)
        return Result.success()
    }

    private fun createChannel(manager: NotificationManager) {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "결제 누락 확인", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "평소 결제가 발생하는 시간대에 기록이 없을 때 확인합니다."
            },
        )
    }

    companion object {
        const val ACTION_NO_PAYMENT = "com.example.buylog.action.NO_PAYMENT"
        const val ACTION_ADD_PAYMENT = "com.example.buylog.action.ADD_PAYMENT"
        const val EXTRA_DATE_EPOCH_DAY = "date_epoch_day"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val STATUS_NOTIFIED = "NOTIFIED"
        const val STATUS_NO_PAYMENT = "NO_PAYMENT"
        private const val CHANNEL_ID = "missed_payment_checks"

        fun notificationId(dateEpochDay: Long): Int = 41_000 + (dateEpochDay % 10_000).toInt()
    }
}
