package org.wpfy.carlauncher.design.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.wpfy.carlauncher.design.kit.KitNextTurnChip
import org.wpfy.carlauncher.design.kit.KitNowPlayingChip
import org.wpfy.carlauncher.design.kit.KitPageDots
import org.wpfy.carlauncher.design.theme.TileScale

/*
 * The HUD elements the three HUD layouts share, drawn from the state and the actions. Everything
 * the Pen does not show (tap targets, looks, descriptions) is optional and off by default.
 */

@Composable
internal fun HudDots(state: KitHudState, actions: KitHudActions) {
    TileScale {
        KitPageDots(mediaShown = state.mediaPage, modifier = actions.dotsModifier, onClick = actions.onDotsClick, description = state.dotsDescription)
    }
}

@Composable
internal fun HudChip(np: KitHudState.NowPlaying, actions: KitHudActions, textWidth: Dp) {
    TileScale {
        KitNowPlayingChip(
            np.title, np.artist, np.art, np.artFallback, np.playing,
            modifier = actions.chipModifier, textWidth = textWidth, canPlayPause = np.canPlayPause, onPlayPause = actions.onPlayPause,
            onClick = actions.onChipClick, description = np.description,
        )
    }
}

@Composable
internal fun HudNextTurn(guidance: KitHudState.Guidance, state: KitHudState, actions: KitHudActions) {
    TileScale {
        KitNextTurnChip(
            guidance.distance, guidance.road, guidance.arrow,
            modifier = actions.chipModifier, onClick = actions.onNextTurnClick, description = state.nextTurnDescription,
        )
    }
}
