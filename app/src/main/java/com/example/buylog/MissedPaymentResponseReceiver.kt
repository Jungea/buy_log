package com.example.buylog

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.buylog.data.BuyLogDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MissedPaymentResponseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MissedPaymentNotificationWorker.ACTION_NO_PAYMENT) return
        val dateEpochDay = intent.getLongExtra(MissedPaymentNotificationWorker.EXTRA_DATE_EPOCH_DAY, Long.MIN_VALUE)
        if (dateEpochDay == Long.MIN_VALUE) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BuyLogDatabase.getInstance(context).missedPaymentCheckDao().recordResponse(
                    dateEpochDay = dateEpochDay,
                    status = MissedPaymentNotificationWorker.STATUS_NO_PAYMENT,
                    respondedAt = System.currentTimeMillis(),
                )
                context.getSystemService(NotificationManager::class.java).cancel(
                    intent.getIntExtra(MissedPaymentNotificationWorker.EXTRA_NOTIFICATION_ID, 0),
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "결제하지 않은 것으로 기록했어요.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
