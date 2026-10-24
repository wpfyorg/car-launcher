package org.wpfy.carlauncher.shell

/** What the system Back key does at the shell level. Inner screens with their own handlers take priority. */
internal enum class ShellBackAction {
    /** Collapse the expanded navigation/search card (Home only). */
    CollapseNavigation,

    /** Return to Home from any other destination. */
    GoHome,

    /** Swallow Back: this is the Home activity and must never finish. */
    Consume,
}

internal fun shellBackAction(
    destination: ShellDestination,
    navigationExpanded: Boolean,
): ShellBackAction = when {
    destination != ShellDestination.Home -> ShellBackAction.GoHome
    navigationExpanded -> ShellBackAction.CollapseNavigation
    else -> ShellBackAction.Consume
}
