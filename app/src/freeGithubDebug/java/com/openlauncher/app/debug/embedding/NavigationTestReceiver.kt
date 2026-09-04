package com.openlauncher.app.debug.embedding

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.openlauncher.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Debug-only ADB hook for selecting a navigation provider through the real settings DataStore. */
class NavigationTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == ActionGesture) {
            val ok = ActivityViewNavigationWidget.debugGesture(
                kind = intent.getStringExtra(ExtraKind).orEmpty(),
                xFraction = intent.getFloatExtra(ExtraX, 0.5f),
                yFraction = intent.getFloatExtra(ExtraY, 0.5f),
            )
            Log.i(Tag, "debug gesture ok=$ok")
            return
        }
        if (action == ActionType) {
            val ok = ActivityViewNavigationWidget.debugType(intent.getStringExtra(ExtraText).orEmpty())
            Log.i(Tag, "debug type ok=$ok")
            return
        }
        if (action == ActionBack) {
            val ok = ActivityViewNavigationWidget.debugBack()
            Log.i(Tag, "debug back ok=$ok")
            return
        }
        if (action != ActionSelectProvider) return
        val component = intent.getStringExtra(ExtraComponent)?.takeIf(String::isNotBlank)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                SettingsRepository(context).setNavigationApp(component)
            }.onSuccess {
                Log.i(Tag, "debug provider selected=$component")
            }.onFailure { error ->
                Log.e(Tag, "debug provider selection failed", error)
            }
            pendingResult.finish()
        }
    }

    companion object {
        const val ActionSelectProvider = "com.openlauncher.app.debug.SELECT_NAVIGATION_PROVIDER"
        const val ActionGesture = "com.openlauncher.app.debug.NAVIGATION_GESTURE"
        const val ActionType = "com.openlauncher.app.debug.NAVIGATION_TYPE"
        const val ActionBack = "com.openlauncher.app.debug.NAVIGATION_BACK"
        const val ExtraComponent = "component"
        const val ExtraKind = "kind"
        const val ExtraX = "x"
        const val ExtraY = "y"
        const val ExtraText = "text"
        private const val Tag = "NavigationTestReceiver"
    }
}
