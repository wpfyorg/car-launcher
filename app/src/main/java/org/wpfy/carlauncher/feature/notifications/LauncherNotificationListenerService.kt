package org.wpfy.carlauncher.feature.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.wpfy.carlauncher.core.diagnostics.AppLog
import org.wpfy.carlauncher.feature.calls.CallRepository
import java.util.concurrent.CopyOnWriteArraySet

class LauncherNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationListenerBridge.attach(this)
        refreshCallSnapshot()
    }

    override fun onListenerDisconnected() {
        NotificationListenerBridge.ifOwner(this) {
            CallRepository.getInstance(applicationContext).clear()
        }
        NotificationListenerBridge.detach(this)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshCallSnapshot()
        NotificationListenerBridge.notifyChanged()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshCallSnapshot()
        NotificationListenerBridge.notifyChanged()
    }

    override fun onDestroy() {
        NotificationListenerBridge.ifOwner(this) {
            CallRepository.getInstance(applicationContext).clear()
        }
        NotificationListenerBridge.detach(this)
        super.onDestroy()
    }

    private fun refreshCallSnapshot() {
        val repository = CallRepository.getInstance(applicationContext)
        val snapshot = runCatching { activeNotifications?.toList() }
        NotificationListenerBridge.ifOwner(this) {
            if (snapshot.isSuccess) {
                repository.updateNotifications(snapshot.getOrNull().orEmpty())
            } else {
                AppLog.w(Tag, "call notification snapshot failed")
                repository.clear()
            }
        }
    }

    private companion object {
        const val Tag = "Calls"
    }
}

internal object NotificationListenerBridge {
    @Volatile
    var service: LauncherNotificationListenerService? = null
        private set

    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    fun attach(service: LauncherNotificationListenerService) {
        this.service = service
        notifyChanged()
    }

    fun detach(service: LauncherNotificationListenerService) {
        if (this.service === service) {
            this.service = null
        }
        notifyChanged()
    }

    fun ifOwner(service: LauncherNotificationListenerService, action: () -> Unit) {
        if (this.service === service) action()
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun notifyChanged() {
        listeners.forEach { it.invoke() }
    }
}
