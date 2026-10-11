package org.wpfy.carlauncher.feature.calls

import android.graphics.Bitmap

sealed interface CallState {
    data object None : CallState

    data class Incoming(
        val callerName: String?,
        val number: String?,
        val avatar: Bitmap?,
        val sourcePackage: String,
        val canAnswer: Boolean,
        val canDecline: Boolean,
    ) : CallState

    data class Active(
        val callerName: String?,
        val number: String?,
        val avatar: Bitmap?,
        val sourcePackage: String,
        val startedAtElapsedMs: Long?,
        val canHangUp: Boolean,
    ) : CallState
}
