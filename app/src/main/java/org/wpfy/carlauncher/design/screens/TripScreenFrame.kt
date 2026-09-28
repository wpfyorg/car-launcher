package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import org.wpfy.carlauncher.design.kit.KitDockActions
import org.wpfy.carlauncher.design.kit.KitDockLandscape
import org.wpfy.carlauncher.design.kit.KitDockLandscapeCompact
import org.wpfy.carlauncher.design.kit.PenLineText
import org.wpfy.carlauncher.design.kit.PenText
import org.wpfy.carlauncher.design.kit.ScreenGroup
import org.wpfy.carlauncher.design.kit.penShape
import org.wpfy.carlauncher.design.theme.OverlayTier

/*
 * What the drive screens (Trip, Vehicle) share: the frame (dock and pane), the pane's padding, the header's title and
 * its trailing chip. The numbers are each group's Pen frame's own (see TripScreenSpec, VehicleScreenSpec); the rules
 * that place the body are the screens'.
 */

/** A pill in a header: its size, look and the glyph and caption it carries. */
@Immutable
class DriveChip(
    val height: Dp, val padStart: Dp, val padEnd: Dp, val gap: Dp, val radius: Dp, val stroke: Dp, val strokeColor: Color, val fill: Color,
    val iconSize: Dp, val iconFill: Color, val label: TextStyle, val labelFill: Color,
)

/** A glyph beside a chip's caption: its size and tint. */
@Immutable
class DriveGlyph(val size: Dp, val fill: Color)

/**
 * The frame and header every drive screen of a [group] is built on: [frame] is the group's pane place and dock; then the
 * pane's padding and gap, the header's gap and title texts, and the gap between the body's parts.
 */
@Immutable
class DriveScreenSpec(
    val group: ScreenGroup,
    val frame: SystemScreenSpec,
    val padStart: Dp, val padTop: Dp, val padEnd: Dp, val padBottom: Dp, val paneGap: Dp,
    val headerGap: Dp, val titleGap: Dp, val title: TextStyle, val titleFill: Color, val subtitle: TextStyle, val subtitleFill: Color,
    val bodyGap: Dp,
) {
    val portrait: Boolean get() = group.portrait
    val tier: OverlayTier get() = group.tier
}

/**
 * The dock and the pane of a drive screen; [content] fills the pane inside its padding at the Pen's real sizes (the
 * kits scale themselves). Tags: "Screen/Dock", "Screen/Pane". Without [showDock] only the pane is drawn, as large as
 * [modifier] makes it (the live shell draws the dock and leaves the margins).
 */
@Composable
internal fun DriveFrame(
    spec: DriveScreenSpec,
    dock: KitDockState,
    dockActions: KitDockActions,
    tag: String,
    modifier: Modifier,
    showDock: Boolean,
    content: @Composable () -> Unit,
) {
    val frame = spec.frame
    val padding = PaddingValues(start = spec.padStart, top = spec.padTop, end = spec.padEnd, bottom = spec.padBottom)
    if (!showDock) {
        Box(modifier.fillMaxSize().testTag("$tag/Pane").clip(penShape(frame.paneRadius)).background(frame.paneFill).padding(padding)) { content() }
        return
    }
    val portrait = frame.portrait
    if (portrait != null) {
        PortraitFrame(portrait, dock, dockActions, tag, modifier) { areaWidth, areaHeight ->
            Box(Modifier.size(areaWidth, areaHeight).testTag("$tag/Pane").clip(penShape(frame.paneRadius)).background(frame.paneFill).padding(padding)) { content() }
        }
        return
    }
    BoxWithConstraints(modifier.fillMaxSize().background(frame.fill)) {
        val height = maxHeight
        val width = maxWidth
        Box(Modifier.testTag("$tag/Dock")) {
            if (frame.compactDock) KitDockLandscapeCompact(dock.clock, dock.notificationCount, height, actions = dockActions)
            else KitDockLandscape(dock.clock, dock.notificationCount, dock.apps, height, frame.dockScale, actions = dockActions)
        }
        Box(
            Modifier.offset(frame.paneX, frame.paneY).size(width - frame.paneX - frame.paneY, height - frame.paneY * 2).testTag("$tag/Pane")
                .clip(penShape(frame.paneRadius)).background(frame.paneFill).padding(padding),
        ) { content() }
    }
}

/** A header chip: [icon] and [label] in the chip's pill; [trailing] follows the caption. Tapping runs [onClick] (none: not a button). */
@Composable
internal fun DriveChipView(
    chip: DriveChip, icon: ImageVector, label: String, tag: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    description: String? = null, enabled: Boolean = true, trailing: @Composable () -> Unit = {},
) {
    val shape = penShape(chip.radius)
    Row(
        modifier.testTag(tag).height(chip.height).clip(shape).background(chip.fill, shape).border(chip.stroke, chip.strokeColor, shape)
            .then(if (onClick != null || !enabled) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = { onClick?.invoke() }) else Modifier)
            .then(if (description != null || !enabled) Modifier.semantics {
                if (description != null) contentDescription = description
                if (!enabled) disabled()
            } else Modifier)
            .padding(start = chip.padStart, end = chip.padEnd),
        horizontalArrangement = Arrangement.spacedBy(chip.gap), verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.testTag("$tag/Icon").size(chip.iconSize), tint = chip.iconFill)
        PenText(label, chip.label, chip.labelFill, Modifier.testTag("$tag/Label"))
        trailing()
    }
}

/** The header's title column: the title over the subtitle. */
@Composable
internal fun DriveTitle(
    spec: DriveScreenSpec,
    title: String,
    subtitle: String,
    tag: String,
    modifier: Modifier = Modifier,
    fillSubtitleWidth: Boolean = false,
) {
    Column(modifier.testTag(tag), verticalArrangement = Arrangement.spacedBy(spec.titleGap)) {
        PenText(title, spec.title, spec.titleFill, Modifier.testTag("$tag/Title"))
        PenLineText(
            subtitle,
            spec.subtitle,
            spec.subtitleFill,
            (if (fillSubtitleWidth) Modifier.fillMaxWidth() else Modifier).testTag("$tag/Subtitle"),
        )
    }
}
