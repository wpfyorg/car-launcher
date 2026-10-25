package org.wpfy.carlauncher.shell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The time in the system's 12 or 24 hour style. It refreshes every minute, when the time, zone or
 * hour format changes, and when the launcher resumes.
 */
@Composable
fun rememberClockText(): String {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentTime by remember { mutableStateOf(formattedClock(context)) }

    DisposableEffect(context, lifecycleOwner) {
        val refresh = { currentTime = formattedClock(context) }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = refresh()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    return currentTime
}

/** [time] as "HH:mm" in 24 hour style, or "h:mm" (no day-period, the dock clock is narrow) in 12 hour style. */
internal fun formatClock(time: LocalTime, is24Hour: Boolean): String =
    time.format(if (is24Hour) Clock24Formatter else Clock12Formatter)

private fun formattedClock(context: Context): String =
    formatClock(LocalTime.now(), DateFormat.is24HourFormat(context))

private val Clock24Formatter = DateTimeFormatter.ofPattern("HH:mm")
private val Clock12Formatter = DateTimeFormatter.ofPattern("h:mm")
