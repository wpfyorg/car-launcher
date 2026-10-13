package org.wpfy.carlauncher.feature.notifications

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import org.wpfy.carlauncher.design.adapters.kitNotifications
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.screenGroupFor
import org.wpfy.carlauncher.design.screens.NotificationsListScreen
import org.wpfy.carlauncher.design.screens.ScreenState
import org.wpfy.carlauncher.design.screens.SystemScreenSpec
import org.wpfy.carlauncher.design.theme.LocalCarWindow

/*
 * The live notifications screen: the Pen kit list of the window's screen group (the pane only: the
 * shell draws the dock), fed by the notification repository. Dismiss (only clearable ones) and clear
 * all are the live behaviour; the access and empty states have no Pen frame.
 */

@Composable
fun NotificationRoute(
    state: NotificationState,
    repository: NotificationRepository,
) {
    val context = LocalContext.current
    Notifications(
        state = state,
        onRequestAccess = {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        },
        onOpen = repository::open,
        onDismiss = repository::dismiss,
        onClearAll = repository::clearAllVisible,
    )
}

/** Whether Clear all is offered: access is granted and there is something Android lets go of. */
internal fun canClearNotifications(state: NotificationState): Boolean =
    state.accessStatus != NotificationAccessStatus.PermissionRequired && state.notifications.any(LauncherNotification::isClearable)

@Composable
fun Notifications(
    state: NotificationState,
    onRequestAccess: () -> Unit,
    onDismiss: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: ((String) -> Unit)? = null,
) {
    val window = LocalCarWindow.current
    val spec = SystemScreenSpec.forGroup(screenGroupFor(window))
    val items = kitNotifications(state.notifications, onOpen = onOpen?.let { open -> { n -> open(n.key) } }, onDismiss = { onDismiss(it.key) })
    val screenState = run {
        when {
            state.accessStatus == NotificationAccessStatus.PermissionRequired -> ScreenState(
                title = "Allow notification access",
                message = "Enable Car Launcher under Notification access to show a driving-safe notification list.",
                icon = PenIcons.Settings,
                actionLabel = "Open notification access",
                onAction = onRequestAccess,
            )
            state.notifications.isEmpty() -> ScreenState(
                title = "No driving-safe notifications",
                message = "Messages, reminders, events, and navigation alerts will appear here.",
                icon = PenIcons.NotificationsOff,
            )
            else -> null
        }
    }
    NotificationsListScreen(
        spec = spec,
        title = "Notifications",
        clearAll = "Clear all",
        items = items,
        hint = "Only driving-safe notifications are shown while the car is moving.",
        modifier = modifier.fillMaxSize(),
        onClearAll = onClearAll,
        showDock = false,
        showClearAll = canClearNotifications(state),
        state = screenState,
        compactClearAll = notificationClearIconOnly(window.widthDp, LocalDensity.current.fontScale),
        lazy = true,
    )
}

/** Below this window width the header cannot fit the title and the labelled Clear all side by side. */
private const val ClearIconOnlyMaxWidthDp = 420f
private const val ClearIconOnlyLargeTextMaxWidthDp = 720f
private const val LargeTextScale = 1.3f

/** Whether Clear all shows its glyph alone: a narrow window, or large text in a medium one. */
internal fun notificationClearIconOnly(widthDp: Float, fontScale: Float): Boolean =
    widthDp < ClearIconOnlyMaxWidthDp || (fontScale >= LargeTextScale && widthDp < ClearIconOnlyLargeTextMaxWidthDp)
