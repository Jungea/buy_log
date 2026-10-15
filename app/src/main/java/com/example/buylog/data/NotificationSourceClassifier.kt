package com.example.buylog.data

enum class NotificationSourceType {
    APP_NOTIFICATION,
    SMS_NOTIFICATION,
}

object NotificationSourceClassifier {
    private val smsPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.android.messaging",
    )

    fun classify(packageName: String): NotificationSourceType =
        if (packageName in smsPackages) {
            NotificationSourceType.SMS_NOTIFICATION
        } else {
            NotificationSourceType.APP_NOTIFICATION
        }
}
