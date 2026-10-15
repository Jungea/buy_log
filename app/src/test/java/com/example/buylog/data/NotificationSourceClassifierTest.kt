package com.example.buylog.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSourceClassifierTest {
    @Test
    fun `known sms apps are classified as sms notifications`() {
        assertEquals(
            NotificationSourceType.SMS_NOTIFICATION,
            NotificationSourceClassifier.classify("com.google.android.apps.messaging"),
        )
        assertEquals(
            NotificationSourceType.SMS_NOTIFICATION,
            NotificationSourceClassifier.classify("com.samsung.android.messaging"),
        )
    }

    @Test
    fun `other apps are classified as app notifications`() {
        assertEquals(
            NotificationSourceType.APP_NOTIFICATION,
            NotificationSourceClassifier.classify("com.example.card"),
        )
    }
}
