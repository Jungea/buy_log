package com.example.buylog

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object QaPaymentNotificationSender {
    const val CHANNEL_ID = "buylog_qa_payment"

    fun send(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "QA 결제 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "BuyLog의 결제 알림 수집과 파싱을 확인하는 테스트 채널"
            },
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.buylog.R.drawable.ic_launcher)
            .setContentTitle("[QA카드] 승인")
            .setContentText("BuyLog 테스트 상점 12,000원")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("BuyLog 테스트 상점 12,000원\nQA카드 승인"),
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
