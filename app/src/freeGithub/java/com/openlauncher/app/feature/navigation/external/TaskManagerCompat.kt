package com.openlauncher.app.feature.navigation.external

/**
 * Public-SDK task-control boundary for the normal GitHub build.
 *
 * Android 9 focus/move-stack APIs are hidden and stay outside this source set until the
 * YT5760D privilege matrix proves which compatibility artifact is actually required.
 */
internal object TaskManagerCompat {
    val canControlEmbeddedTask: Boolean = false
}
