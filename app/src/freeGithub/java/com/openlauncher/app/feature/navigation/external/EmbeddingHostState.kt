package com.openlauncher.app.feature.navigation.external

internal sealed interface EmbeddingHostState {
    data object Idle : EmbeddingHostState
    data object SurfaceReady : EmbeddingHostState
    data object Launching : EmbeddingHostState
    data object Stopping : EmbeddingHostState
    data object Stopped : EmbeddingHostState
    data class Running(val displayId: Int) : EmbeddingHostState
    data class Unavailable(val reason: String) : EmbeddingHostState
    data class Failed(val reason: String) : EmbeddingHostState
}
