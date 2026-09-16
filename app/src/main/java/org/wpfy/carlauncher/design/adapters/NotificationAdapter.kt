package org.wpfy.carlauncher.design.adapters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.wpfy.carlauncher.design.screens.NotificationItem
import org.wpfy.carlauncher.feature.notifications.LauncherNotification
import org.wpfy.carlauncher.feature.notifications.relativeTime

/*
 * Live notifications -> the kit's list rows. The repository has already filtered what is safe to
 * show while driving, so every notification given is drawn, in the order given. A row's meta line
 * is the app and how long ago, as on the live screen.
 */

/** Maps notifications in input order, using [nowMillis] for age labels, giving non-clearable notifications no dismiss action and cards no click without [onOpen]. */
fun kitNotifications(
    notifications: List<LauncherNotification>,
    nowMillis: Long = System.currentTimeMillis(),
    art: (Bitmap) -> ImageBitmap = Bitmap::asImageBitmap,
    onOpen: ((LauncherNotification) -> Unit)? = null,
    onDismiss: (LauncherNotification) -> Unit = {},
): List<NotificationItem> = notifications.map { notification ->
    NotificationItem(
        node = notification.key,
        meta = "${notification.appLabel} · ${relativeTime(notification.postTimeMillis, nowMillis)}",
        title = notification.title,
        body = notification.text,
        appIcon = notification.appIcon?.let(art),
        // No open action, or no content intent, leaves the card without a click (nothing to announce or press).
        onClick = if (onOpen != null && notification.canOpen) ({ onOpen(notification) }) else null,
        // Android keeps non-clearable notifications, so their row has no dismiss, as live.
        onDismiss = if (notification.isClearable) ({ onDismiss(notification) }) else null,
        dismissDescription = "Dismiss ${notification.title}",
    )
}
