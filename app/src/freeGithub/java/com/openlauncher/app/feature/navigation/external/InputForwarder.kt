package com.openlauncher.app.feature.navigation.external

import android.view.MotionEvent

/** Public-build input boundary; privileged injection is intentionally not implemented here. */
internal object InputForwarder {
    val isSupported: Boolean = false

    fun forward(event: MotionEvent): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val ignored = event
        return false
    }
}
