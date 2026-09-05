package com.openlauncher.app.feature.navigation.external

import com.openlauncher.app.launcher.LauncherApp

internal interface EmbeddedNavigationView {
    val supportsInteractiveEmbedding: Boolean

    val supportsEmbeddedBack: Boolean
        get() = false

    fun bind(
        app: LauncherApp?,
        onStateChanged: (EmbeddingHostState) -> Unit,
    )

    fun ensureRunning()

    fun performBackPress(): Boolean = false

    fun stop(): Boolean = false

    fun restart(): Boolean = false

    fun release() = Unit
}
