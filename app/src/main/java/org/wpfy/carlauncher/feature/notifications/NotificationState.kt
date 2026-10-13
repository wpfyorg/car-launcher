package org.wpfy.carlauncher.feature.notifications

import android.graphics.Bitmap

enum class NotificationAccessStatus {
    PermissionRequired,
    Available,
}

data class LauncherNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val appIcon: Bitmap? = null,
    val title: String,
    val text: String,
    val postTimeMillis: Long,
    val isClearable: Boolean,
    /** Whether the notification carries a content intent that tapping can send. */
    val canOpen: Boolean = false,
    /** Whether Android would remove the notification once it is opened (FLAG_AUTO_CANCEL). */
    val autoCancel: Boolean = false,
)

data class NotificationState(
    val accessStatus: NotificationAccessStatus = NotificationAccessStatus.PermissionRequired,
    val notifications: List<LauncherNotification> = emptyList(),
)

/** Whether a notification is cancelled after tapping: only when the send went through and it asked for auto-cancel. */
internal fun shouldCancelAfterOpen(sent: Boolean, autoCancel: Boolean): Boolean = sent && autoCancel
