package org.wpfy.carlauncher.core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.wpfy.carlauncher.MainActivity
import org.wpfy.carlauncher.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (SettingsRepository(context).current().startOnBoot) {
                    runCatching {
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
