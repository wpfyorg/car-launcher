package org.wpfy.carlauncher

import android.content.Intent

/**
 * Whether an intent delivered to the running launcher is the Home key: MAIN + HOME, and not a
 * restore from Recents (that one must keep the screen the driver left).
 */
internal fun isHomeKeyIntent(action: String?, categories: Set<String>?, flags: Int): Boolean =
    action == Intent.ACTION_MAIN &&
        categories?.contains(Intent.CATEGORY_HOME) == true &&
        flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0

/**
 * Whether this create follows a process restart: saved state exists, yet no activity was created in
 * this process before. A recreation inside a live process (night mode, locale) is not a restart.
 */
internal fun isProcessRestart(hasSavedState: Boolean, launchedInThisProcess: Boolean): Boolean =
    hasSavedState && !launchedInThisProcess

/**
 * Whether a created activity must open on Home: the process restarted (saved state would reopen the
 * old screen) and the Home key started it. A fresh create keeps the startup screen, and a recreation
 * in a live process keeps the screen the driver is on.
 */
internal fun shouldGoHomeOnCreate(processRestarted: Boolean, action: String?, categories: Set<String>?, flags: Int): Boolean =
    processRestarted && isHomeKeyIntent(action, categories, flags)
