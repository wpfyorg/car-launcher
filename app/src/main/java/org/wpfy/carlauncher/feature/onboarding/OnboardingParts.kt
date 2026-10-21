package org.wpfy.carlauncher.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.R
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.penStyle
import org.wpfy.carlauncher.design.kit.PenScreens.Onboarding
import org.wpfy.carlauncher.design.kit.PenScreens.Onboarding.CompactPermissions.Panel.Notifications as CompactNotification
import org.wpfy.carlauncher.design.kit.PenScreens.Onboarding.PortraitPermissions.Panel.Location as PortraitLocation
import org.wpfy.carlauncher.design.kit.PenScreens.Onboarding.PortraitPermissions.Panel.Notifications as PortraitNotification

internal enum class OnboardingStructure { Standard, Compact, Ultrawide, Portrait }
internal enum class OnboardingChoiceKind { HomeScreen, RegularApp }
internal enum class OnboardingThemeChoice { Auto, Dark, Light }
internal enum class OnboardingDockSide { Left, Right }

private fun child(tag: String, name: String) = "$tag/$name"

private data class ScaffoldSpec(
    val fill: Color,
    val introX: Dp,
    val introY: Dp,
    val introW: Dp,
    val introH: Dp,
    val panelX: Dp,
    val panelY: Dp,
    val panelW: Dp,
    val panelH: Dp,
    val panelPadStart: Dp,
    val panelPadTop: Dp,
    val panelPadEnd: Dp,
    val panelPadBottom: Dp,
    val panelRadius: Dp,
    val panelStroke: Dp,
    val panelStrokeColor: Color,
    val panelFill: Color,
)

private fun scaffoldSpec(structure: OnboardingStructure, welcome: Boolean): ScaffoldSpec = when (structure) {
    OnboardingStructure.Standard -> ScaffoldSpec(
        Onboarding.Welcome.Fill,
        Onboarding.Welcome.Intro.X, Onboarding.Welcome.Intro.Y,
        Onboarding.Welcome.Intro.W, Onboarding.Welcome.Intro.H,
        Onboarding.Welcome.Panel.X, Onboarding.Welcome.Panel.Y,
        Onboarding.Welcome.Panel.W, Onboarding.Welcome.Panel.H,
        if (welcome) Onboarding.Welcome.Panel.PadStart else Onboarding.Permissions.Panel.PadStart,
        if (welcome) Onboarding.Welcome.Panel.PadTop else Onboarding.Permissions.Panel.PadTop,
        if (welcome) Onboarding.Welcome.Panel.PadEnd else Onboarding.Permissions.Panel.PadEnd,
        if (welcome) Onboarding.Welcome.Panel.PadBottom else Onboarding.Permissions.Panel.PadBottom,
        Onboarding.Welcome.Panel.Radius, Onboarding.Welcome.Panel.Stroke,
        Onboarding.Welcome.Panel.StrokeColor, Onboarding.Welcome.Panel.Fill,
    )
    OnboardingStructure.Compact -> ScaffoldSpec(
        Onboarding.CompactPermissions.Fill,
        Onboarding.CompactPermissions.Intro.X, Onboarding.CompactPermissions.Intro.Y,
        Onboarding.CompactPermissions.Intro.W, Onboarding.CompactPermissions.Intro.H,
        Onboarding.CompactPermissions.Panel.X, Onboarding.CompactPermissions.Panel.Y,
        Onboarding.CompactPermissions.Panel.W, Onboarding.CompactPermissions.Panel.H,
        Onboarding.CompactPermissions.Panel.PadStart, Onboarding.CompactPermissions.Panel.PadTop,
        Onboarding.CompactPermissions.Panel.PadEnd, Onboarding.CompactPermissions.Panel.PadBottom,
        Onboarding.CompactPermissions.Panel.Radius, Onboarding.CompactPermissions.Panel.Stroke,
        Onboarding.CompactPermissions.Panel.StrokeColor, Onboarding.CompactPermissions.Panel.Fill,
    )
    OnboardingStructure.Ultrawide -> ScaffoldSpec(
        Onboarding.UltrawideWelcome.Fill,
        Onboarding.UltrawideWelcome.Intro.X, Onboarding.UltrawideWelcome.Intro.Y,
        Onboarding.UltrawideWelcome.Intro.W, Onboarding.UltrawideWelcome.Intro.H,
        Onboarding.UltrawideWelcome.Panel.X, Onboarding.UltrawideWelcome.Panel.Y,
        Onboarding.UltrawideWelcome.Panel.W, Onboarding.UltrawideWelcome.Panel.H,
        if (welcome) Onboarding.UltrawideWelcome.Panel.PadStart else Onboarding.UltrawideLayout.Panel.PadStart,
        if (welcome) Onboarding.UltrawideWelcome.Panel.PadTop else Onboarding.UltrawideLayout.Panel.PadTop,
        if (welcome) Onboarding.UltrawideWelcome.Panel.PadEnd else Onboarding.UltrawideLayout.Panel.PadEnd,
        if (welcome) Onboarding.UltrawideWelcome.Panel.PadBottom else Onboarding.UltrawideLayout.Panel.PadBottom,
        Onboarding.UltrawideWelcome.Panel.Radius, Onboarding.UltrawideWelcome.Panel.Stroke,
        Onboarding.UltrawideWelcome.Panel.StrokeColor, Onboarding.UltrawideWelcome.Panel.Fill,
    )
    OnboardingStructure.Portrait -> ScaffoldSpec(
        Onboarding.PortraitHomeOrApp.Fill,
        Onboarding.PortraitHomeOrApp.Intro.X, Onboarding.PortraitHomeOrApp.Intro.Y,
        Onboarding.PortraitHomeOrApp.Intro.W, Onboarding.PortraitHomeOrApp.Intro.H,
        Onboarding.PortraitHomeOrApp.Panel.X, Onboarding.PortraitHomeOrApp.Panel.Y,
        Onboarding.PortraitHomeOrApp.Panel.W, Onboarding.PortraitHomeOrApp.Panel.H,
        Onboarding.PortraitHomeOrApp.Panel.PadStart, Onboarding.PortraitHomeOrApp.Panel.PadTop,
        Onboarding.PortraitHomeOrApp.Panel.PadEnd, Onboarding.PortraitHomeOrApp.Panel.PadBottom,
        Onboarding.PortraitHomeOrApp.Panel.Radius, Onboarding.PortraitHomeOrApp.Panel.Stroke,
        Onboarding.PortraitHomeOrApp.Panel.StrokeColor, Onboarding.PortraitHomeOrApp.Panel.Fill,
    )
}

@Composable
internal fun OnboardingScaffold(
    intro: @Composable () -> Unit,
    panel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "Onboarding",
    structure: OnboardingStructure = OnboardingStructure.Standard,
    welcome: Boolean = false,
    tagRoot: Boolean = true,
    actions: (@Composable () -> Unit)? = null,
) {
    val spec = scaffoldSpec(structure, welcome)
    Box(if (tagRoot) modifier.fillMaxSize().background(spec.fill).testTag(tag)
        else modifier.fillMaxSize().background(spec.fill)) {
        Box(
            Modifier.offset(spec.introX, spec.introY).size(spec.introW, spec.introH),
        ) { intro() }
        Box(
            Modifier.offset(spec.panelX, spec.panelY).size(spec.panelW, spec.panelH)
                .clip(RoundedCornerShape(spec.panelRadius)).background(spec.panelFill)
                .border(spec.panelStroke, spec.panelStrokeColor, RoundedCornerShape(spec.panelRadius))
                .testTag(child(tag, "Panel"))
                .padding(spec.panelPadStart, spec.panelPadTop, spec.panelPadEnd, spec.panelPadBottom),
        ) { panel() }
        if (structure == OnboardingStructure.Portrait && actions != null) {
            Box(
                Modifier.offset(Onboarding.PortraitHomeOrApp.Actions.X, Onboarding.PortraitHomeOrApp.Actions.Y)
                    .size(Onboarding.PortraitHomeOrApp.Actions.W, Onboarding.PortraitHomeOrApp.Actions.H)
                    .testTag(child(tag, "Actions")),
            ) { actions() }
        }
    }
}

private data class IntroSpec(
    val width: Dp,
    val height: Dp,
    val padStart: Dp,
    val padTop: Dp,
    val padEnd: Dp,
    val padBottom: Dp,
    val brandGap: Dp,
    val markW: Dp,
    val markRadius: Dp,
    val markFill: Color,
    val glyphW: Dp,
    val glyphFill: Color,
    val nameFill: Color,
    val nameStyle: TextStyle,
    val progressPadTop: Dp,
    val progressPadBottom: Dp,
    val progressGap: Dp,
    val segmentH: Dp,
    val segmentRadius: Dp,
    val activeFill: Color,
    val inactiveFill: Color,
    val stepFill: Color,
    val stepStyle: TextStyle,
    val headingGap: Dp,
    val headingFill: Color,
    val headingStyle: TextStyle,
    val bodyGap: Dp,
    val bodyFill: Color,
    val bodyStyle: TextStyle,
    val actionGap: Dp,
)

private fun introSpec(structure: OnboardingStructure): IntroSpec = when (structure) {
    OnboardingStructure.Standard -> IntroSpec(
        Onboarding.Welcome.Intro.W, Onboarding.Welcome.Intro.H,
        Onboarding.Welcome.Intro.PadStart, Onboarding.Welcome.Intro.PadTop,
        Onboarding.Welcome.Intro.PadEnd, Onboarding.Welcome.Intro.PadBottom,
        Onboarding.Welcome.Intro.Brand.Gap,
        Onboarding.Welcome.Intro.Brand.Mark.W, Onboarding.Welcome.Intro.Brand.Mark.Radius,
        Onboarding.Welcome.Intro.Brand.Mark.Fill, Onboarding.Welcome.Intro.Brand.Mark.Glyph.W,
        Onboarding.Welcome.Intro.Brand.Mark.Glyph.Fill, Onboarding.Welcome.Intro.Brand.Name.Fill,
        Onboarding.Welcome.Intro.Brand.Name.Style,
        Onboarding.Welcome.Intro.Progress.PadTop, Onboarding.Welcome.Intro.Progress.PadBottom,
        Onboarding.Welcome.Intro.Progress.Gap, Onboarding.Welcome.Intro.Progress.Seg1.H,
        Onboarding.Welcome.Intro.Progress.Seg1.Radius, Onboarding.Welcome.Intro.Progress.Seg1.Fill,
        Onboarding.Welcome.Intro.Progress.Seg2.Fill, Onboarding.Welcome.Intro.Step.Fill,
        Onboarding.Welcome.Intro.Step.Style,
        Onboarding.Welcome.Intro.Heading.Y - Onboarding.Welcome.Intro.Step.Y - Onboarding.Welcome.Intro.Step.H,
        Onboarding.Welcome.Intro.Heading.Fill, Onboarding.Welcome.Intro.Heading.Style,
        Onboarding.Welcome.Intro.Body.Y - Onboarding.Welcome.Intro.Heading.Y - Onboarding.Welcome.Intro.Heading.H,
        Onboarding.Welcome.Intro.Body.Fill, Onboarding.Welcome.Intro.Body.Style,
        Onboarding.Welcome.Intro.Actions.Gap,
    )
    OnboardingStructure.Compact -> IntroSpec(
        Onboarding.CompactPermissions.Intro.W, Onboarding.CompactPermissions.Intro.H,
        Onboarding.CompactPermissions.Intro.PadStart, Onboarding.CompactPermissions.Intro.PadTop,
        Onboarding.CompactPermissions.Intro.PadEnd, Onboarding.CompactPermissions.Intro.PadBottom,
        Onboarding.Welcome.Intro.Brand.Gap,
        Onboarding.Welcome.Intro.Brand.Mark.W, Onboarding.Welcome.Intro.Brand.Mark.Radius,
        Onboarding.Welcome.Intro.Brand.Mark.Fill, Onboarding.Welcome.Intro.Brand.Mark.Glyph.W,
        Onboarding.Welcome.Intro.Brand.Mark.Glyph.Fill, Onboarding.Welcome.Intro.Brand.Name.Fill,
        Onboarding.Welcome.Intro.Brand.Name.Style,
        Onboarding.CompactPermissions.Intro.Progress.PadTop, Onboarding.CompactPermissions.Intro.Progress.PadBottom,
        Onboarding.CompactPermissions.Intro.Progress.Gap, Onboarding.CompactPermissions.Intro.Progress.Seg1.H,
        Onboarding.CompactPermissions.Intro.Progress.Seg1.Radius, Onboarding.CompactPermissions.Intro.Progress.Seg1.Fill,
        Onboarding.CompactPermissions.Intro.Progress.Seg3.Fill, Onboarding.CompactPermissions.Intro.Step.Fill,
        Onboarding.CompactPermissions.Intro.Step.Style,
        Onboarding.CompactPermissions.Intro.Heading.Y - Onboarding.CompactPermissions.Intro.Step.Y - Onboarding.CompactPermissions.Intro.Step.H,
        Onboarding.CompactPermissions.Intro.Heading.Fill, Onboarding.CompactPermissions.Intro.Heading.Style,
        Onboarding.Welcome.Intro.Body.Y - Onboarding.Welcome.Intro.Heading.Y - Onboarding.Welcome.Intro.Heading.H,
        Onboarding.Welcome.Intro.Body.Fill, Onboarding.Welcome.Intro.Body.Style,
        Onboarding.CompactPermissions.Intro.Actions.Gap,
    )
    OnboardingStructure.Ultrawide -> IntroSpec(
        Onboarding.UltrawideWelcome.Intro.W, Onboarding.UltrawideWelcome.Intro.H,
        Onboarding.UltrawideWelcome.Intro.PadStart, Onboarding.UltrawideWelcome.Intro.PadTop,
        Onboarding.UltrawideWelcome.Intro.PadEnd, Onboarding.UltrawideWelcome.Intro.PadBottom,
        Onboarding.UltrawideWelcome.Intro.Brand.Gap,
        Onboarding.UltrawideWelcome.Intro.Brand.Mark.W, Onboarding.UltrawideWelcome.Intro.Brand.Mark.Radius,
        Onboarding.UltrawideWelcome.Intro.Brand.Mark.Fill, Onboarding.UltrawideWelcome.Intro.Brand.Mark.Glyph.W,
        Onboarding.UltrawideWelcome.Intro.Brand.Mark.Glyph.Fill, Onboarding.UltrawideWelcome.Intro.Brand.Name.Fill,
        Onboarding.UltrawideWelcome.Intro.Brand.Name.Style,
        Onboarding.UltrawideWelcome.Intro.Progress.PadTop, Onboarding.UltrawideWelcome.Intro.Progress.PadBottom,
        Onboarding.UltrawideWelcome.Intro.Progress.Gap, Onboarding.UltrawideWelcome.Intro.Progress.Seg1.H,
        Onboarding.UltrawideWelcome.Intro.Progress.Seg1.Radius, Onboarding.UltrawideWelcome.Intro.Progress.Seg1.Fill,
        Onboarding.UltrawideWelcome.Intro.Progress.Seg2.Fill, Onboarding.UltrawideWelcome.Intro.Step.Fill,
        Onboarding.UltrawideWelcome.Intro.Step.Style,
        Onboarding.UltrawideWelcome.Intro.Heading.Y - Onboarding.UltrawideWelcome.Intro.Step.Y - Onboarding.UltrawideWelcome.Intro.Step.H,
        Onboarding.UltrawideWelcome.Intro.Heading.Fill, Onboarding.UltrawideWelcome.Intro.Heading.Style,
        Onboarding.UltrawideWelcome.Intro.Body.Y - Onboarding.UltrawideWelcome.Intro.Heading.Y - Onboarding.UltrawideWelcome.Intro.Heading.H,
        Onboarding.UltrawideWelcome.Intro.Body.Fill, Onboarding.UltrawideWelcome.Intro.Body.Style,
        Onboarding.UltrawideWelcome.Intro.Actions.Gap,
    )
    OnboardingStructure.Portrait -> IntroSpec(
        Onboarding.PortraitHomeOrApp.Intro.W, Onboarding.PortraitHomeOrApp.Intro.H,
        Onboarding.PortraitHomeOrApp.Intro.PadStart, Onboarding.PortraitHomeOrApp.Intro.PadTop,
        Onboarding.PortraitHomeOrApp.Intro.PadEnd, Onboarding.PortraitHomeOrApp.Intro.PadBottom,
        Onboarding.PortraitHomeOrApp.Intro.Brand.Gap,
        Onboarding.PortraitHomeOrApp.Intro.Brand.Mark.W, Onboarding.PortraitHomeOrApp.Intro.Brand.Mark.Radius,
        Onboarding.PortraitHomeOrApp.Intro.Brand.Mark.Fill, Onboarding.PortraitHomeOrApp.Intro.Brand.Mark.Glyph.W,
        Onboarding.PortraitHomeOrApp.Intro.Brand.Mark.Glyph.Fill, Onboarding.PortraitHomeOrApp.Intro.Brand.Name.Fill,
        Onboarding.PortraitHomeOrApp.Intro.Brand.Name.Style,
        Onboarding.PortraitHomeOrApp.Intro.Progress.PadTop, Onboarding.PortraitHomeOrApp.Intro.Progress.PadBottom,
        Onboarding.PortraitHomeOrApp.Intro.Progress.Gap, Onboarding.PortraitHomeOrApp.Intro.Progress.Seg1.H,
        Onboarding.PortraitHomeOrApp.Intro.Progress.Seg1.Radius, Onboarding.PortraitHomeOrApp.Intro.Progress.Seg1.Fill,
        Onboarding.PortraitHomeOrApp.Intro.Progress.Seg4.Fill, Onboarding.PortraitHomeOrApp.Intro.Step.Fill,
        Onboarding.PortraitHomeOrApp.Intro.Step.Style,
        Onboarding.PortraitHomeOrApp.Intro.Heading.Y - Onboarding.PortraitHomeOrApp.Intro.Step.Y - Onboarding.PortraitHomeOrApp.Intro.Step.H,
        Onboarding.PortraitHomeOrApp.Intro.Heading.Fill, Onboarding.PortraitHomeOrApp.Intro.Heading.Style,
        Onboarding.PortraitHomeOrApp.Intro.Body.Y - Onboarding.PortraitHomeOrApp.Intro.Heading.Y - Onboarding.PortraitHomeOrApp.Intro.Heading.H,
        Onboarding.PortraitHomeOrApp.Intro.Body.Fill, Onboarding.PortraitHomeOrApp.Intro.Body.Style,
        Onboarding.PortraitHomeOrApp.Actions.Gap,
    )
}

@Composable
internal fun Intro(
    step: String,
    title: String,
    body: String,
    progressStep: Int,
    modifier: Modifier = Modifier,
    tag: String = "Intro",
    progressCount: Int = ONBOARDING_PROGRESS_STEP_COUNT,
    structure: OnboardingStructure = OnboardingStructure.Standard,
    showBrand: Boolean = true,
    showBody: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val spec = introSpec(structure)
    // Multi-line Pen text nodes export the height of the whole node as lineHeight.
    // Use the corresponding single-line Pen tokens for Compose's per-line height.
    val headingStyle = spec.headingStyle.copy(lineHeight = when (structure) {
        OnboardingStructure.Standard -> Onboarding.Permissions.Intro.Heading.Style.lineHeight
        OnboardingStructure.Ultrawide -> Onboarding.UltrawideLayout.Intro.Heading.Style.lineHeight
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Intro.Heading.Style.lineHeight
        OnboardingStructure.Compact -> Onboarding.CompactPermissions.Intro.Heading.Style.lineHeight
    })
    val bodyStyle = spec.bodyStyle.copy(lineHeight = Onboarding.UltrawideLayout.Intro.Body.Style.lineHeight)
    val stepWidth = when (structure) {
        OnboardingStructure.Standard -> when (progressStep) {
            1 -> Onboarding.Welcome.Intro.Step.W
            2 -> Onboarding.Permissions.Intro.Step.W
            3 -> Onboarding.HomeOrApp.Intro.Step.W
            4 -> when (title) {
                "Make it yours" -> Onboarding.ThemePro.Intro.Step.W
                "Choose your layout" -> Onboarding.Layout.Intro.Step.W
                else -> Onboarding.MapAppFree.Intro.Step.W
            }
            else -> Onboarding.Layout.Intro.Step.W
        }
        OnboardingStructure.Compact -> if (progressStep == 2) Onboarding.CompactPermissions.Intro.Step.W else Onboarding.CompactHomeOrApp.Intro.Step.W
        OnboardingStructure.Ultrawide -> if (progressStep == 1) Onboarding.UltrawideWelcome.Intro.Step.W else Onboarding.UltrawideLayout.Intro.Step.W
        OnboardingStructure.Portrait -> if (progressStep == 2) Onboarding.PortraitPermissions.Intro.Step.W else Onboarding.PortraitHomeOrApp.Intro.Step.W
    }
    val headingHeight = when (structure) {
        OnboardingStructure.Standard -> when (progressStep) {
            1 -> Onboarding.Welcome.Intro.Heading.H
            2 -> Onboarding.Permissions.Intro.Heading.H
            3 -> Onboarding.HomeOrApp.Intro.Heading.H
            4 -> when (title) {
                "Make it yours" -> Onboarding.ThemePro.Intro.Heading.H
                "Choose your layout" -> Onboarding.Layout.Intro.Heading.H
                else -> Onboarding.MapAppFree.Intro.Heading.H
            }
            else -> Onboarding.Layout.Intro.Heading.H
        }
        OnboardingStructure.Compact -> if (progressStep == 2) Onboarding.CompactPermissions.Intro.Heading.H else Onboarding.CompactHomeOrApp.Intro.Heading.H
        OnboardingStructure.Ultrawide -> if (progressStep == 1) Onboarding.UltrawideWelcome.Intro.Heading.H else Onboarding.UltrawideLayout.Intro.Heading.H
        OnboardingStructure.Portrait -> if (progressStep == 2) Onboarding.PortraitPermissions.Intro.Heading.H else Onboarding.PortraitHomeOrApp.Intro.Heading.H
    }
    val bodyHeight = when (structure) {
        OnboardingStructure.Standard -> when (progressStep) {
            1 -> Onboarding.Welcome.Intro.Body.H
            2 -> Onboarding.Permissions.Intro.Body.H
            3 -> Onboarding.HomeOrApp.Intro.Body.H
            4 -> when (title) {
                "Make it yours" -> Onboarding.ThemePro.Intro.Body.H
                "Choose your layout" -> Onboarding.Layout.Intro.Body.H
                else -> Onboarding.MapAppFree.Intro.Body.H
            }
            else -> Onboarding.Layout.Intro.Body.H
        }
        OnboardingStructure.Compact -> Onboarding.Welcome.Intro.Body.H
        OnboardingStructure.Ultrawide -> if (progressStep == 1) Onboarding.UltrawideWelcome.Intro.Body.H else Onboarding.UltrawideLayout.Intro.Body.H
        OnboardingStructure.Portrait -> if (progressStep == 2) Onboarding.PortraitPermissions.Intro.Body.H else Onboarding.PortraitHomeOrApp.Intro.Body.H
    }
    Column(
        modifier.size(spec.width, spec.height).testTag(tag)
            .padding(spec.padStart, spec.padTop, spec.padEnd, spec.padBottom),
    ) {
        if (showBrand) {
            Row(
                Modifier.size(Onboarding.Welcome.Intro.Brand.W, Onboarding.Welcome.Intro.Brand.H)
                    .testTag(child(tag, "Brand")),
                horizontalArrangement = Arrangement.spacedBy(spec.brandGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(spec.markW).clip(RoundedCornerShape(spec.markRadius))
                        .background(spec.markFill).testTag(child(tag, "Brand/Mark")),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PenIcons.DirectionsCar, null, Modifier.size(spec.glyphW).testTag(child(tag, "Brand/Mark/Glyph")), spec.glyphFill)
                }
                Text("Car Launcher", Modifier.testTag(child(tag, "Brand/Name")), color = spec.nameFill, style = spec.nameStyle, maxLines = 1)
            }
        }
        Row(
            Modifier.fillMaxWidth().testTag(child(tag, "Progress"))
                .padding(top = spec.progressPadTop, bottom = spec.progressPadBottom),
            horizontalArrangement = Arrangement.spacedBy(spec.progressGap),
        ) {
            repeat(progressCount) { index ->
                Box(
                    Modifier.weight(1f).height(spec.segmentH).clip(RoundedCornerShape(spec.segmentRadius))
                        .background(if (index < progressStep) spec.activeFill else spec.inactiveFill)
                        .testTag(child(tag, "Progress/Seg${index + 1}")),
                )
            }
        }
        Text(step, Modifier.size(stepWidth, Onboarding.Welcome.Intro.Step.H).testTag(child(tag, "Step")),
            color = spec.stepFill, style = spec.stepStyle, maxLines = 1)
        Spacer(Modifier.size(Onboarding.Welcome.Intro.StepGap.W, spec.headingGap)
            .testTag(child(tag, "StepGap")))
        Box(Modifier.fillMaxWidth().height(headingHeight).testTag(child(tag, "Heading"))) {
            val headingModifier = if (structure == OnboardingStructure.Compact) {
                Modifier.wrapContentWidth(unbounded = true).wrapContentHeight(unbounded = true)
            } else Modifier.fillMaxWidth().wrapContentHeight(unbounded = true)
            Text(title, headingModifier, color = spec.headingFill, style = penStyle(headingStyle))
        }
        if (showBody && structure != OnboardingStructure.Compact) {
            Spacer(Modifier.size(Onboarding.Welcome.Intro.HeadingGap.W, spec.bodyGap)
                .testTag(child(tag, "HeadingGap")))
            Box(Modifier.fillMaxWidth().height(bodyHeight).testTag(child(tag, "Body"))) {
                Text(body, Modifier.fillMaxWidth().wrapContentHeight(unbounded = true),
                    color = spec.bodyFill, style = penStyle(bodyStyle))
            }
        } else if (structure == OnboardingStructure.Compact) {
            Spacer(Modifier.size(Onboarding.CompactPermissions.Intro.HeadingGap.W,
                Onboarding.CompactPermissions.Intro.HeadingGap.H).testTag(child(tag, "HeadingGap")))
        }
        if (actions != null) {
            Spacer(Modifier.fillMaxWidth().weight(1f).testTag(child(tag, "Spacer")))
            Row(
                Modifier.fillMaxWidth().testTag(child(tag, "Actions")),
                horizontalArrangement = Arrangement.spacedBy(spec.actionGap),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
internal fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "Continue",
    enabled: Boolean = true,
    structure: OnboardingStructure = OnboardingStructure.Standard,
    fullWidth: Boolean = false,
    showArrow: Boolean = true,
) {
    val width = when {
        structure == OnboardingStructure.Compact -> Onboarding.CompactPermissions.Intro.Actions.Continue.W
        structure == OnboardingStructure.Ultrawide -> if (fullWidth) Onboarding.UltrawideWelcome.Intro.Actions.Continue.W else Onboarding.UltrawideLayout.Intro.Actions.Continue.W
        structure == OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Actions.Continue.W
        fullWidth -> Onboarding.Welcome.Intro.Actions.Continue.W
        else -> Onboarding.Layout.Intro.Actions.Continue.W
    }
    val height = if (structure == OnboardingStructure.Compact) Onboarding.CompactPermissions.Intro.Actions.Continue.H else Onboarding.Layout.Intro.Actions.Continue.H
    val radius = if (structure == OnboardingStructure.Compact) Onboarding.CompactPermissions.Intro.Actions.Continue.Radius else Onboarding.Layout.Intro.Actions.Continue.Radius
    val fill = if (enabled) Onboarding.PermissionsAllowed.Intro.Actions.Continue.Fill else Onboarding.Permissions.Intro.Actions.Continue.Fill
    val labelFill = if (enabled) Onboarding.PermissionsAllowed.Intro.Actions.Continue.Label.Fill else Onboarding.Permissions.Intro.Actions.Continue.Label.Fill
    val arrowFill = if (enabled) Onboarding.PermissionsAllowed.Intro.Actions.Continue.Arrow.Fill else Onboarding.Permissions.Intro.Actions.Continue.Arrow.Fill
    Row(
        modifier.size(width, height).clip(RoundedCornerShape(radius)).background(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.testTag(child(tag, "Label")), color = labelFill, style = Onboarding.Layout.Intro.Actions.Continue.Label.Style, maxLines = 1)
        if (showArrow) {
            Icon(
                PenIcons.ArrowForward, null,
                Modifier.padding(start = Onboarding.Layout.Intro.Actions.Continue.Gap)
                    .size(Onboarding.Layout.Intro.Actions.Continue.Arrow.W).testTag(child(tag, "Arrow")),
                arrowFill,
            )
        }
    }
}

@Composable
internal fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "Back",
    enabled: Boolean = true,
    structure: OnboardingStructure = OnboardingStructure.Standard,
) {
    val size = if (structure == OnboardingStructure.Compact) Onboarding.CompactPermissions.Intro.Actions.Back.W else Onboarding.Layout.Intro.Actions.Back.W
    val iconSize = if (structure == OnboardingStructure.Compact) Onboarding.CompactPermissions.Intro.Actions.Back.Icon.W else Onboarding.Layout.Intro.Actions.Back.Icon.W
    Box(
        modifier.size(size).clip(RoundedCornerShape(Onboarding.Layout.Intro.Actions.Back.Radius))
            .background(Onboarding.Layout.Intro.Actions.Back.Fill)
            .border(Onboarding.Layout.Intro.Actions.Back.Stroke, Onboarding.Layout.Intro.Actions.Back.StrokeColor, RoundedCornerShape(Onboarding.Layout.Intro.Actions.Back.Radius))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(PenIcons.ArrowBack, "Back", Modifier.size(iconSize).testTag(child(tag, "Icon")), Onboarding.Layout.Intro.Actions.Back.Icon.Fill)
    }
}

private data class PermissionSpec(
    val height: Dp,
    val padStart: Dp,
    val padTop: Dp,
    val padEnd: Dp,
    val padBottom: Dp,
    val gap: Dp,
    val radius: Dp,
    val fill: Color,
    val leadW: Dp,
    val leadRadius: Dp,
    val leadFill: Color,
    val iconW: Dp,
    val iconFill: Color,
    val textGap: Dp,
    val titleFill: Color,
    val titleStyle: TextStyle,
    val subtitleFill: Color,
    val subtitleStyle: TextStyle,
    val trailPadStart: Dp,
    val trailPadTop: Dp,
    val trailPadEnd: Dp,
    val trailPadBottom: Dp,
    val trailRadius: Dp,
    val trailFill: Color,
    val trailLabelFill: Color,
    val trailLabelStyle: TextStyle,
)

private fun permissionSpec(allowed: Boolean, structure: OnboardingStructure, notifications: Boolean): PermissionSpec {
    if (structure == OnboardingStructure.Portrait) {
        return if (notifications) PermissionSpec(
            PortraitNotification.H,
            PortraitNotification.PadStart, PortraitNotification.PadTop,
            PortraitNotification.PadEnd, PortraitNotification.PadBottom,
            PortraitNotification.Gap, PortraitNotification.Radius, PortraitNotification.Fill,
            PortraitNotification.Lead.W, PortraitNotification.Lead.Radius, PortraitNotification.Lead.Fill,
            PortraitNotification.Lead.Icon.W, PortraitNotification.Lead.Icon.Fill,
            PortraitNotification.Text.Gap,
            PortraitNotification.Text.TitleRow.Title.Fill, PortraitNotification.Text.TitleRow.Title.Style,
            PortraitNotification.Text.Subtitle.Fill,
            PortraitLocation.Text.Subtitle.Style,
            PortraitNotification.Trail.PadStart, PortraitNotification.Trail.PadTop,
            PortraitNotification.Trail.PadEnd, PortraitNotification.Trail.PadBottom,
            PortraitNotification.Trail.Radius, PortraitNotification.Trail.Fill,
            PortraitNotification.Trail.Label.Fill, PortraitNotification.Trail.Label.Style,
        ) else PermissionSpec(
            PortraitLocation.H,
            PortraitLocation.PadStart, PortraitLocation.PadTop,
            PortraitLocation.PadEnd, PortraitLocation.PadBottom,
            PortraitLocation.Gap, PortraitLocation.Radius, PortraitLocation.Fill,
            PortraitLocation.Lead.W, PortraitLocation.Lead.Radius,
            if (allowed) PortraitLocation.Lead.Fill else Onboarding.Permissions.Panel.Location.Lead.Fill,
            PortraitLocation.Lead.Icon.W,
            if (allowed) PortraitLocation.Lead.Icon.Fill else Onboarding.Permissions.Panel.Location.Lead.Icon.Fill,
            PortraitLocation.Text.Gap,
            PortraitLocation.Text.TitleRow.Title.Fill, PortraitLocation.Text.TitleRow.Title.Style,
            PortraitLocation.Text.Subtitle.Fill, PortraitLocation.Text.Subtitle.Style,
            PortraitLocation.Trail.PadStart, PortraitLocation.Trail.PadTop,
            PortraitLocation.Trail.PadEnd, PortraitLocation.Trail.PadBottom,
            PortraitLocation.Trail.Radius, PortraitLocation.Trail.Fill,
            PortraitLocation.Trail.Label.Fill, PortraitLocation.Trail.Label.Style,
        )
    }
    if (structure == OnboardingStructure.Compact && notifications) {
        return PermissionSpec(
            CompactNotification.H,
            CompactNotification.PadStart, CompactNotification.PadTop,
            CompactNotification.PadEnd, CompactNotification.PadBottom,
            CompactNotification.Gap, CompactNotification.Radius, CompactNotification.Fill,
            CompactNotification.Lead.W, CompactNotification.Lead.Radius, CompactNotification.Lead.Fill,
            CompactNotification.Lead.Icon.W, CompactNotification.Lead.Icon.Fill,
            CompactNotification.Text.Gap,
            CompactNotification.Text.TitleRow.Title.Fill, CompactNotification.Text.TitleRow.Title.Style,
            Onboarding.Permissions.Panel.Notifications.Text.Subtitle.Fill,
            Onboarding.Permissions.Panel.Notifications.Text.Subtitle.Style,
            CompactNotification.Trail.PadStart, CompactNotification.Trail.PadTop,
            CompactNotification.Trail.PadEnd, CompactNotification.Trail.PadBottom,
            CompactNotification.Trail.Radius, CompactNotification.Trail.Fill,
            CompactNotification.Trail.Label.Fill, CompactNotification.Trail.Label.Style,
        )
    }
    if (structure == OnboardingStructure.Compact) {
        return PermissionSpec(
            Onboarding.CompactPermissions.Panel.Location.H,
            Onboarding.CompactPermissions.Panel.Location.PadStart, Onboarding.CompactPermissions.Panel.Location.PadTop,
            Onboarding.CompactPermissions.Panel.Location.PadEnd, Onboarding.CompactPermissions.Panel.Location.PadBottom,
            Onboarding.CompactPermissions.Panel.Location.Gap, Onboarding.CompactPermissions.Panel.Location.Radius,
            Onboarding.CompactPermissions.Panel.Location.Fill, Onboarding.CompactPermissions.Panel.Location.Lead.W,
            Onboarding.CompactPermissions.Panel.Location.Lead.Radius,
            if (allowed) Onboarding.CompactPermissions.Panel.Location.Lead.Fill else Onboarding.CompactPermissions.Panel.Notifications.Lead.Fill,
            Onboarding.CompactPermissions.Panel.Location.Lead.Icon.W,
            if (allowed) Onboarding.CompactPermissions.Panel.Location.Lead.Icon.Fill else Onboarding.CompactPermissions.Panel.Notifications.Lead.Icon.Fill,
            Onboarding.CompactPermissions.Panel.Location.Text.Gap,
            Onboarding.CompactPermissions.Panel.Location.Text.TitleRow.Title.Fill,
            Onboarding.CompactPermissions.Panel.Location.Text.TitleRow.Title.Style,
            Onboarding.Permissions.Panel.Location.Text.Subtitle.Fill,
            Onboarding.Permissions.Panel.Location.Text.Subtitle.Style,
            Onboarding.CompactPermissions.Panel.Location.Trail.PadStart, Onboarding.CompactPermissions.Panel.Location.Trail.PadTop,
            Onboarding.CompactPermissions.Panel.Location.Trail.PadEnd, Onboarding.CompactPermissions.Panel.Location.Trail.PadBottom,
            Onboarding.CompactPermissions.Panel.Location.Trail.Radius,
            if (allowed) Onboarding.CompactPermissions.Panel.Location.Trail.Fill else Onboarding.CompactPermissions.Panel.Notifications.Trail.Fill,
            if (allowed) Onboarding.CompactPermissions.Panel.Location.Trail.Label.Fill else Onboarding.CompactPermissions.Panel.Notifications.Trail.Label.Fill,
            Onboarding.CompactPermissions.Panel.Location.Trail.Label.Style,
        )
    }
    return if (allowed) PermissionSpec(
        Onboarding.PermissionsAllowed.Panel.Location.H,
        Onboarding.PermissionsAllowed.Panel.Location.PadStart, Onboarding.PermissionsAllowed.Panel.Location.PadTop,
        Onboarding.PermissionsAllowed.Panel.Location.PadEnd, Onboarding.PermissionsAllowed.Panel.Location.PadBottom,
        Onboarding.PermissionsAllowed.Panel.Location.Gap, Onboarding.PermissionsAllowed.Panel.Location.Radius,
        Onboarding.PermissionsAllowed.Panel.Location.Fill, Onboarding.PermissionsAllowed.Panel.Location.Lead.W,
        Onboarding.PermissionsAllowed.Panel.Location.Lead.Radius, Onboarding.PermissionsAllowed.Panel.Location.Lead.Fill,
        Onboarding.PermissionsAllowed.Panel.Location.Lead.Icon.W, Onboarding.PermissionsAllowed.Panel.Location.Lead.Icon.Fill,
        Onboarding.PermissionsAllowed.Panel.Location.Text.Gap,
        Onboarding.PermissionsAllowed.Panel.Location.Text.TitleRow.Title.Fill,
        Onboarding.PermissionsAllowed.Panel.Location.Text.TitleRow.Title.Style,
        Onboarding.PermissionsAllowed.Panel.Location.Text.Subtitle.Fill,
        Onboarding.PermissionsAllowed.Panel.Location.Text.Subtitle.Style,
        Onboarding.PermissionsAllowed.Panel.Location.Trail.PadStart, Onboarding.PermissionsAllowed.Panel.Location.Trail.PadTop,
        Onboarding.PermissionsAllowed.Panel.Location.Trail.PadEnd, Onboarding.PermissionsAllowed.Panel.Location.Trail.PadBottom,
        Onboarding.PermissionsAllowed.Panel.Location.Trail.Radius, Onboarding.PermissionsAllowed.Panel.Location.Trail.Fill,
        Onboarding.PermissionsAllowed.Panel.Location.Trail.Label.Fill, Onboarding.PermissionsAllowed.Panel.Location.Trail.Label.Style,
    ) else PermissionSpec(
        Onboarding.Permissions.Panel.Location.H,
        Onboarding.Permissions.Panel.Location.PadStart, Onboarding.Permissions.Panel.Location.PadTop,
        Onboarding.Permissions.Panel.Location.PadEnd, Onboarding.Permissions.Panel.Location.PadBottom,
        Onboarding.Permissions.Panel.Location.Gap, Onboarding.Permissions.Panel.Location.Radius,
        Onboarding.Permissions.Panel.Location.Fill, Onboarding.Permissions.Panel.Location.Lead.W,
        Onboarding.Permissions.Panel.Location.Lead.Radius, Onboarding.Permissions.Panel.Location.Lead.Fill,
        Onboarding.Permissions.Panel.Location.Lead.Icon.W, Onboarding.Permissions.Panel.Location.Lead.Icon.Fill,
        Onboarding.Permissions.Panel.Location.Text.Gap,
        Onboarding.Permissions.Panel.Location.Text.TitleRow.Title.Fill,
        Onboarding.Permissions.Panel.Location.Text.TitleRow.Title.Style,
        Onboarding.Permissions.Panel.Location.Text.Subtitle.Fill,
        Onboarding.Permissions.Panel.Location.Text.Subtitle.Style,
        Onboarding.Permissions.Panel.Location.Trail.PadStart, Onboarding.Permissions.Panel.Location.Trail.PadTop,
        Onboarding.Permissions.Panel.Location.Trail.PadEnd, Onboarding.Permissions.Panel.Location.Trail.PadBottom,
        Onboarding.Permissions.Panel.Location.Trail.Radius, Onboarding.Permissions.Panel.Location.Trail.Fill,
        Onboarding.Permissions.Panel.Location.Trail.Label.Fill, Onboarding.Permissions.Panel.Location.Trail.Label.Style,
    )
}

@Composable
internal fun PermissionRow(
    title: String,
    subtitle: String,
    allowed: Boolean,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "Permission",
    allowedLabel: String = "Allowed",
    allowLabel: String = "Allow",
    icon: ImageVector = PenIcons.MyLocation,
    structure: OnboardingStructure = OnboardingStructure.Standard,
    showSubtitle: Boolean = true,
    notifications: Boolean = false,
) {
    val spec = permissionSpec(allowed, structure, notifications)
    Row(
        modifier.fillMaxWidth().height(spec.height).clip(RoundedCornerShape(spec.radius))
            .background(spec.fill).clickable(enabled = !allowed, onClick = onAllow)
            .padding(spec.padStart, spec.padTop, spec.padEnd, spec.padBottom).testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(spec.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(spec.leadW).clip(RoundedCornerShape(spec.leadRadius)).background(spec.leadFill)
                .testTag(child(tag, "Lead")), contentAlignment = Alignment.Center,
        ) {
            Icon(if (allowed) PenIcons.Check else icon, null, Modifier.size(spec.iconW).testTag(child(tag, "Lead/Icon")), spec.iconFill)
        }
        val textModifier = if (notifications && structure != OnboardingStructure.Compact) {
            val width = if (structure == OnboardingStructure.Portrait) PortraitNotification.Text.W
                else Onboarding.Permissions.Panel.Notifications.Text.W
            Modifier.width(width)
        } else Modifier.weight(1f)
        Column(textModifier.testTag(child(tag, "Text")), verticalArrangement = Arrangement.spacedBy(spec.textGap)) {
            Box(Modifier.testTag(child(tag, "Text/TitleRow"))) {
                val titleModifier = if (structure == OnboardingStructure.Compact && !notifications) {
                    Modifier.requiredWidth(Onboarding.CompactPermissions.Panel.Location.Text.TitleRow.Title.W)
                } else Modifier
                Text(title, titleModifier.testTag(child(tag, "Text/TitleRow/Title")), color = spec.titleFill, style = spec.titleStyle, maxLines = 1)
            }
            if (showSubtitle) {
                val subtitleHeight = when {
                    structure == OnboardingStructure.Portrait && notifications -> PortraitNotification.Text.Subtitle.H
                    structure == OnboardingStructure.Portrait -> PortraitLocation.Text.Subtitle.H
                    notifications -> Onboarding.Permissions.Panel.Notifications.Text.Subtitle.H
                    allowed -> Onboarding.PermissionsAllowed.Panel.Location.Text.Subtitle.H
                    else -> Onboarding.Permissions.Panel.Location.Text.Subtitle.H
                }
                Text(subtitle, Modifier.fillMaxWidth().height(subtitleHeight).testTag(child(tag, "Text/Subtitle")), color = spec.subtitleFill, style = spec.subtitleStyle, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            Modifier.clip(RoundedCornerShape(spec.trailRadius)).background(spec.trailFill)
                .testTag(child(tag, "Trail"))
                .padding(spec.trailPadStart, spec.trailPadTop, spec.trailPadEnd, spec.trailPadBottom),
        ) {
            Text(if (allowed) allowedLabel else allowLabel, Modifier.testTag(child(tag, "Trail/Label")), color = spec.trailLabelFill, style = spec.trailLabelStyle, maxLines = 1)
        }
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier, tag: String = "Section") {
    val width = if (text == "REQUIRED") Onboarding.Permissions.Panel.SectionRequired.W else if (text == "RECOMMENDED") Onboarding.Permissions.Panel.SectionRecommended.W else Onboarding.MapAppFree.Panel.Section.W
    Text(text, modifier.requiredWidth(width).testTag(tag), color = Onboarding.Permissions.Panel.SectionRequired.Fill, style = Onboarding.Permissions.Panel.SectionRequired.Style, maxLines = 1)
}

@Composable
internal fun ChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "Choice",
    kind: OnboardingChoiceKind = OnboardingChoiceKind.HomeScreen,
    structure: OnboardingStructure = OnboardingStructure.Standard,
    points: List<String> = emptyList(),
    showDetails: Boolean = structure != OnboardingStructure.Compact,
) {
    val portrait = structure == OnboardingStructure.Portrait
    val compact = structure == OnboardingStructure.Compact
    val width = when {
        portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.W
        compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.W
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.W
    }
    val height = when {
        portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.H
        compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.H
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.H
    }
    val pad = when {
        portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.PadStart
        compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.PadStart
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.PadStart
    }
    val gap = when {
        portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Gap
        compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Gap
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Gap
    }
    val radius = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Radius
    val fill = if (selected) Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Fill else Onboarding.HomeOrApp.Panel.Modes.RegularApp.Fill
    var cardModifier = modifier.size(width, height).clip(RoundedCornerShape(radius)).background(fill)
    if (selected) cardModifier = cardModifier.border(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Stroke, Onboarding.HomeOrApp.Panel.Modes.HomeScreen.StrokeColor, RoundedCornerShape(radius))
    cardModifier = cardModifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .padding(pad).testTag(tag)
    if (portrait) {
        Row(cardModifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            ChoiceVisual(kind, structure, child(tag, "Visual"))
            ChoiceCopy(title, subtitle, selected, kind, structure, points, showDetails, child(tag, "Copy"), Modifier.weight(1f))
        }
    } else {
        Column(cardModifier, verticalArrangement = Arrangement.spacedBy(gap)) {
            ChoiceVisual(kind, structure, child(tag, "Visual"))
            ChoiceCopy(title, subtitle, selected, kind, structure, points, showDetails, child(tag, "Copy"), Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ChoiceVisual(kind: OnboardingChoiceKind, structure: OnboardingStructure, tag: String) {
    val portrait = structure == OnboardingStructure.Portrait
    val compact = structure == OnboardingStructure.Compact
    val width = when {
        portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Visual.W
        compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Visual.W
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.W
    }
    val height = when {
        portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Visual.H
        compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Visual.H
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.H
    }
    val radius = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Radius
    Box(
        Modifier.size(width, height).clip(RoundedCornerShape(radius))
            .border(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Stroke, Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.StrokeColor, RoundedCornerShape(radius))
            .background(if (kind == OnboardingChoiceKind.RegularApp) Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.Fill else Onboarding.Welcome.Fill)
            .testTag(tag),
    ) {
        if (kind == OnboardingChoiceKind.HomeScreen) {
            Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().testTag(child(tag, "Image")),
                contentScale = ContentScale.Crop,
            )
            if (!compact && !portrait) {
                Box(
                    Modifier.offset(
                        Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.X,
                        Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.Y,
                    ).size(
                        Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.W,
                        Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.H,
                    ).clip(RoundedCornerShape(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.Radius))
                        .background(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.Fill)
                        .border(
                            Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.Stroke,
                            Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.StrokeColor,
                            RoundedCornerShape(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.Radius),
                        ).testTag(child(tag, "Tag"))
                        .padding(
                            start = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.PadStart,
                            top = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.PadTop,
                        ),
                ) {
                    Text(
                        "Best for head units",
                        Modifier.testTag(child(tag, "Tag/Label")),
                        color = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.Label.Fill,
                        style = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Visual.Tag.Label.Style,
                        maxLines = 1,
                    )
                }
            }
        } else {
            RegularAppGrid(structure, tag)
        }
    }
}

@Composable
private fun RegularAppGrid(structure: OnboardingStructure, tag: String) {
    val appW = when (structure) {
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.W
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.W
        else -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.W
    }
    val appRadius = when (structure) {
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.Radius
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.Radius
        else -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.Radius
    }
    val icons = listOf(PenIcons.Mail, PenIcons.PhotoCamera, PenIcons.DirectionsCar, PenIcons.Chat, PenIcons.Settings, PenIcons.MusicNote, PenIcons.CalendarMonth, PenIcons.Map, PenIcons.Call, PenIcons.Apps)
    val visualPadding = when (structure) {
        OnboardingStructure.Portrait -> with(Onboarding.PortraitHomeOrApp.Panel.Modes.RegularApp.Visual) { PaddingValues(PadStart, PadTop, PadEnd, PadBottom) }
        OnboardingStructure.Compact -> with(Onboarding.CompactHomeOrApp.Panel.Modes.RegularApp.Visual) { PaddingValues(PadStart, PadTop, PadEnd, PadBottom) }
        else -> with(Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual) { PaddingValues(PadStart, PadTop, PadEnd, PadBottom) }
    }
    val visualGap = when (structure) {
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.RegularApp.Visual.Gap
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.RegularApp.Visual.Gap
        else -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.Gap
    }
    val rowW = when (structure) {
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.W
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.W
        else -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.W
    }
    val rowH = when (structure) {
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.H
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.H
        else -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.H
    }
    Column(
        Modifier.fillMaxSize().padding(visualPadding),
        verticalArrangement = Arrangement.spacedBy(visualGap, Alignment.CenterVertically),
    ) {
        icons.chunked(icons.size / 2).forEachIndexed { rowIndex, rowIcons ->
            val rowTag = child(tag, "IconRow${rowIndex + 1}")
            Box(Modifier.size(rowW, rowH).testTag(rowTag)) {
                rowIcons.forEachIndexed { index, icon ->
                    Box(
                        Modifier.offset(x = (rowW - appW) * index / (rowIcons.size - 1))
                            .size(appW).clip(RoundedCornerShape(appRadius))
                            .background(Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.Fill)
                            .testTag(child(rowTag, "App${index + 1}")),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, Modifier.size(Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.Icon.W).testTag(child(rowTag, "App${index + 1}/Icon")), Onboarding.HomeOrApp.Panel.Modes.RegularApp.Visual.IconRow1.App1.Icon.Fill)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceCopy(
    title: String,
    subtitle: String,
    selected: Boolean,
    kind: OnboardingChoiceKind,
    structure: OnboardingStructure,
    points: List<String>,
    showDetails: Boolean,
    tag: String,
    modifier: Modifier,
) {
    val compact = structure == OnboardingStructure.Compact
    val portrait = structure == OnboardingStructure.Portrait
    val gap = when (structure) {
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Copy.Gap
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Copy.Gap
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Gap
    }
    val titleStyle = when (structure) {
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Title.Style
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Title.Style
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Title.Style
    }
    val subtitleStyle = when (structure) {
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Copy.Subtitle.Style
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Copy.Subtitle.Style
        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Subtitle.Style
    }.copy(lineHeight = Onboarding.MapAppFree.Panel.GoogleMaps.Text.Subtitle.Style.lineHeight)
    val copyTokens = when (structure) {
        OnboardingStructure.Compact -> if (kind == OnboardingChoiceKind.HomeScreen) Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Copy.Subtitle.H else Onboarding.CompactHomeOrApp.Panel.Modes.RegularApp.Copy.Subtitle.H
        OnboardingStructure.Portrait -> if (kind == OnboardingChoiceKind.HomeScreen) Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Copy.Subtitle.H else Onboarding.PortraitHomeOrApp.Panel.Modes.RegularApp.Copy.Subtitle.H
        else -> if (kind == OnboardingChoiceKind.HomeScreen) Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Subtitle.H else Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.Subtitle.H
    }
    val titleWidth = when (structure) {
        OnboardingStructure.Compact -> if (kind == OnboardingChoiceKind.HomeScreen) Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Title.W else Onboarding.CompactHomeOrApp.Panel.Modes.RegularApp.Copy.TitleRow.Title.W
        OnboardingStructure.Portrait -> if (kind == OnboardingChoiceKind.HomeScreen) Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Title.W else Onboarding.PortraitHomeOrApp.Panel.Modes.RegularApp.Copy.TitleRow.Title.W
        else -> if (kind == OnboardingChoiceKind.HomeScreen) Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Title.W else Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.TitleRow.Title.W
    }
    val markFill = if (selected) Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Mark.Fill else Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.TitleRow.Mark.Fill
    val copyPadding = when (structure) {
        OnboardingStructure.Compact -> with(Onboarding.CompactHomeOrApp.Panel.Modes.HomeScreen.Copy) { PaddingValues(PadStart, PadTop, PadEnd, PadBottom) }
        OnboardingStructure.Portrait -> with(Onboarding.PortraitHomeOrApp.Panel.Modes.HomeScreen.Copy) { PaddingValues(PadStart, PadTop, PadEnd, PadBottom) }
        else -> with(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy) { PaddingValues(PadStart, PadTop, PadEnd, PadBottom) }
    }
    Column(
        modifier.testTag(tag).padding(copyPadding),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(
            Modifier.fillMaxWidth().testTag(child(tag, "TitleRow")),
            horizontalArrangement = Arrangement.spacedBy(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.requiredWidth(titleWidth).testTag(child(tag, "TitleRow/Title")), color = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Title.Fill, style = titleStyle, maxLines = 1)
            Spacer(Modifier.weight(1f).height(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Spacer.H).testTag(child(tag, "TitleRow/Spacer")))
            if (!compact) Icon(if (selected) PenIcons.CheckCircle else PenIcons.RadioButtonUnchecked, null, Modifier.size(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.TitleRow.Mark.W).testTag(child(tag, "TitleRow/Mark")), markFill)
        }
        Box(Modifier.fillMaxWidth().height(copyTokens).testTag(child(tag, "Subtitle"))) {
            Text(subtitle, Modifier.fillMaxWidth().wrapContentHeight(unbounded = true),
                color = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Subtitle.Fill, style = penStyle(subtitleStyle))
        }
        if (showDetails && !portrait && !compact && points.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().testTag(child(tag, "Points")), verticalArrangement = Arrangement.spacedBy(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Gap)) {
                points.forEachIndexed { index, point ->
                    val rowHeight = if (kind == OnboardingChoiceKind.HomeScreen) when (index) {
                        0 -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point1.H
                        1 -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point2.H
                        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point3.H
                    } else when (index) {
                        0 -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.Points.Point1.H
                        1 -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.Points.Point1.H
                        else -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.Points.Point1.H
                    }
                    val textHeight = if (kind == OnboardingChoiceKind.HomeScreen) when (index) {
                        0 -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point1.Text.H
                        1 -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point2.Text.H
                        else -> Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point3.Text.H
                    } else when (index) {
                        0 -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.Points.Point1.Text.H
                        1 -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.Points.Point1.Text.H
                        else -> Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.Points.Point1.Text.H
                    }
                    Row(Modifier.fillMaxWidth().height(rowHeight).testTag(child(tag, "Points/Point${index + 1}")), horizontalArrangement = Arrangement.spacedBy(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point1.Gap), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            PenIcons.Check, null,
                            Modifier.size(Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point1.Icon.W).testTag(child(tag, "Points/Point${index + 1}/Icon")),
                            if (kind == OnboardingChoiceKind.HomeScreen) Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point1.Icon.Fill else Onboarding.HomeOrApp.Panel.Modes.RegularApp.Copy.Points.Point1.Icon.Fill,
                        )
                        Text(point, Modifier.weight(1f).height(textHeight).testTag(child(tag, "Points/Point${index + 1}/Text")), color = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point1.Text.Fill, style = Onboarding.HomeOrApp.Panel.Modes.HomeScreen.Copy.Points.Point1.Text.Style)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "AppChoice",
    icon: ImageVector = PenIcons.Navigation,
    showSubtitle: Boolean = true,
) {
    var rowModifier = modifier.fillMaxWidth().height(Onboarding.MapAppFree.Panel.GoogleMaps.H)
        .clip(RoundedCornerShape(Onboarding.MapAppFree.Panel.GoogleMaps.Radius))
        .background(if (selected) Onboarding.MapAppFree.Panel.GoogleMaps.Fill else Onboarding.MapAppFree.Panel.Waze.Fill)
    if (selected) rowModifier = rowModifier.border(Onboarding.MapAppFree.Panel.GoogleMaps.Stroke, Onboarding.MapAppFree.Panel.GoogleMaps.StrokeColor, RoundedCornerShape(Onboarding.MapAppFree.Panel.GoogleMaps.Radius))
    Row(
        rowModifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Onboarding.MapAppFree.Panel.GoogleMaps.PadStart, Onboarding.MapAppFree.Panel.GoogleMaps.PadTop, Onboarding.MapAppFree.Panel.GoogleMaps.PadEnd, Onboarding.MapAppFree.Panel.GoogleMaps.PadBottom)
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(Onboarding.MapAppFree.Panel.GoogleMaps.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(Onboarding.MapAppFree.Panel.GoogleMaps.AppIcon.W)
                .clip(RoundedCornerShape(Onboarding.MapAppFree.Panel.GoogleMaps.AppIcon.Radius))
                .background(Onboarding.MapAppFree.Panel.GoogleMaps.AppIcon.Fill).testTag(child(tag, "AppIcon")),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(Onboarding.Permissions.Panel.Location.Lead.Icon.W).testTag(child(tag, "AppIcon/Icon")), Onboarding.MapAppFree.Panel.GoogleMaps.Text.Title.Fill)
        }
        Column(Modifier.weight(1f).testTag(child(tag, "Text")), verticalArrangement = Arrangement.spacedBy(Onboarding.MapAppFree.Panel.GoogleMaps.Text.Gap)) {
            Text(title, Modifier.testTag(child(tag, "Text/Title")), color = Onboarding.MapAppFree.Panel.GoogleMaps.Text.Title.Fill, style = Onboarding.MapAppFree.Panel.GoogleMaps.Text.Title.Style, maxLines = 1)
            if (showSubtitle) Text(subtitle, Modifier.requiredWidth(Onboarding.MapAppFree.Panel.GoogleMaps.Text.Subtitle.W).height(Onboarding.MapAppFree.Panel.GoogleMaps.Text.Subtitle.H).testTag(child(tag, "Text/Subtitle")), color = Onboarding.MapAppFree.Panel.GoogleMaps.Text.Subtitle.Fill, style = Onboarding.MapAppFree.Panel.GoogleMaps.Text.Subtitle.Style, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(if (selected) PenIcons.CheckCircle else PenIcons.RadioButtonUnchecked, null, Modifier.size(Onboarding.MapAppFree.Panel.GoogleMaps.Mark.W).testTag(child(tag, "Mark")), if (selected) Onboarding.MapAppFree.Panel.GoogleMaps.Mark.Fill else Onboarding.MapAppFree.Panel.Waze.Mark.Fill)
    }
}

private data class SwatchHalfSpec(
    val width: Dp,
    val height: Dp,
    val pad: Dp,
    val gap: Dp,
    val fill: Color,
    val railW: Dp,
    val railH: Dp,
    val railRadius: Dp,
    val railFill: Color,
    val bodyW: Dp,
    val bodyH: Dp,
    val bodyGap: Dp,
    val barW: Dp,
    val barH: Dp,
    val barRadius: Dp,
    val barFill: Color,
    val contentW: Dp,
    val contentH: Dp,
    val contentGap: Dp,
    val mapW: Dp,
    val mapH: Dp,
    val mapRadius: Dp,
    val mapFill: Color,
    val cardFill: Color,
    val cardGap: Dp,
    val speedFill: Color,
    val speedStyle: TextStyle,
    val levelW: Dp,
    val levelH: Dp,
    val levelRadius: Dp,
    val levelFill: Color,
)

private fun lightHalfSpec(full: Boolean): SwatchHalfSpec = if (full) SwatchHalfSpec(
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.W, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.H,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.PadStart, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Gap,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Fill,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Rail.W, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Rail.H,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Rail.Radius, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Rail.Fill,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.W, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.H,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Gap, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Bar.W,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Bar.H, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Bar.Radius,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Bar.Fill, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.W,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.H, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Gap,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Map.W, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Map.H,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Map.Radius, Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Map.Fill,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Card.Fill,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Card.Gap,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Card.Speed.Fill,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Card.Speed.Style,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Card.Accent.W,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Card.Accent.H,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Card.Accent.Radius,
    Onboarding.ThemePro.Panel.Themes.Light.Swatch.LightHalf.Body.Content.Card.Accent.Fill,
) else SwatchHalfSpec(
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.W, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.PadStart, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Gap,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Rail.W, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Rail.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Rail.Radius, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Rail.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.W, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Gap, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Bar.W,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Bar.H, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Bar.Radius,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Bar.Fill, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.W,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.H, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Gap,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Map.W, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Map.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Map.Radius, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Map.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Card.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Card.Gap,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Card.Speed.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Card.Speed.Style,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Card.Accent.W,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Card.Accent.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Card.Accent.Radius,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.LightHalf.Body.Content.Card.Accent.Fill,
)

private fun darkHalfSpec(full: Boolean): SwatchHalfSpec = if (full) SwatchHalfSpec(
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.W, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.H,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.PadStart, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Gap,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Fill,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Rail.W, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Rail.H,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Rail.Radius, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Rail.Fill,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.W, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.H,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Gap, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Bar.W,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Bar.H, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Bar.Radius,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Bar.Fill, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.W,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.H, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Gap,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Map.W, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Map.H,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Map.Radius, Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Map.Fill,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Card.Fill,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Card.Gap,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Card.Speed.Fill,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Card.Speed.Style,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Card.Accent.W,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Card.Accent.H,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Card.Accent.Radius,
    Onboarding.ThemePro.Panel.Themes.Dark.Swatch.DarkHalf.Body.Content.Card.Accent.Fill,
) else SwatchHalfSpec(
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.W, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.PadStart, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Gap,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Rail.W, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Rail.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Rail.Radius, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Rail.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.W, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Gap, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Bar.W,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Bar.H, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Bar.Radius,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Bar.Fill, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.W,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.H, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Gap,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Map.W, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Map.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Map.Radius, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Map.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Card.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Card.Gap,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Card.Speed.Fill,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Card.Speed.Style,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Card.Accent.W,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Card.Accent.H,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Card.Accent.Radius,
    Onboarding.ThemePro.Panel.Themes.Auto.Swatch.DarkHalf.Body.Content.Card.Accent.Fill,
)

@Composable
internal fun ThemeRow(
    title: String,
    subtitle: String,
    choice: OnboardingThemeChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "Theme",
) {
    var rowModifier = modifier.fillMaxWidth().height(Onboarding.ThemePro.Panel.Themes.Auto.H)
        .clip(RoundedCornerShape(Onboarding.ThemePro.Panel.Themes.Auto.Radius))
        .background(if (selected) Onboarding.ThemePro.Panel.Themes.Auto.Fill else Onboarding.ThemePro.Panel.Themes.Dark.Fill)
    if (selected) rowModifier = rowModifier.border(Onboarding.ThemePro.Panel.Themes.Auto.Stroke, Onboarding.ThemePro.Panel.Themes.Auto.StrokeColor, RoundedCornerShape(Onboarding.ThemePro.Panel.Themes.Auto.Radius))
    Row(
        rowModifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Onboarding.ThemePro.Panel.Themes.Auto.PadStart, Onboarding.ThemePro.Panel.Themes.Auto.PadTop, Onboarding.ThemePro.Panel.Themes.Auto.PadEnd, Onboarding.ThemePro.Panel.Themes.Auto.PadBottom)
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(Onboarding.ThemePro.Panel.Themes.Auto.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSwatch(choice, child(tag, "Swatch"))
        Row(Modifier.weight(1f).testTag(child(tag, "Label")), horizontalArrangement = Arrangement.spacedBy(Onboarding.ThemePro.Panel.Themes.Auto.Label.Gap), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).testTag(child(tag, "Label/Text")), verticalArrangement = Arrangement.spacedBy(Onboarding.ThemePro.Panel.Themes.Auto.Label.Text.Gap)) {
                Text(title, Modifier.testTag(child(tag, "Label/Text/Title")), color = Onboarding.ThemePro.Panel.Themes.Auto.Label.Text.Title.Fill, style = Onboarding.ThemePro.Panel.Themes.Auto.Label.Text.Title.Style, maxLines = 1)
                Text(subtitle, Modifier.testTag(child(tag, "Label/Text/Sub")), color = Onboarding.ThemePro.Panel.Themes.Auto.Label.Text.Sub.Fill, style = Onboarding.ThemePro.Panel.Themes.Auto.Label.Text.Sub.Style, maxLines = 1)
            }
            Icon(if (selected) PenIcons.CheckCircle else PenIcons.RadioButtonUnchecked, null, Modifier.size(Onboarding.ThemePro.Panel.Themes.Auto.Label.Mark.W).testTag(child(tag, "Label/Mark")), if (selected) Onboarding.ThemePro.Panel.Themes.Auto.Label.Mark.Fill else Onboarding.ThemePro.Panel.Themes.Dark.Label.Mark.Fill)
        }
    }
}

@Composable
private fun ThemeSwatch(choice: OnboardingThemeChoice, tag: String) {
    val width = when (choice) {
        OnboardingThemeChoice.Auto -> Onboarding.ThemePro.Panel.Themes.Auto.Swatch.W
        OnboardingThemeChoice.Dark -> Onboarding.ThemePro.Panel.Themes.Dark.Swatch.W
        OnboardingThemeChoice.Light -> Onboarding.ThemePro.Panel.Themes.Light.Swatch.W
    }
    val shape = RoundedCornerShape(Onboarding.ThemePro.Panel.Themes.Auto.Swatch.Radius)
    Row(
        Modifier.size(width, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.H).clip(shape)
            .border(Onboarding.ThemePro.Panel.Themes.Auto.Swatch.Stroke, Onboarding.ThemePro.Panel.Themes.Auto.Swatch.StrokeColor, shape)
            .testTag(tag),
    ) {
        when (choice) {
            OnboardingThemeChoice.Auto -> {
                ThemeHalf(lightHalfSpec(full = false), child(tag, "LightHalf"))
                ThemeHalf(darkHalfSpec(full = false), child(tag, "DarkHalf"))
            }
            OnboardingThemeChoice.Dark -> ThemeHalf(darkHalfSpec(full = true), child(tag, "DarkHalf"))
            OnboardingThemeChoice.Light -> ThemeHalf(lightHalfSpec(full = true), child(tag, "LightHalf"))
        }
    }
}

@Composable
private fun RowScope.ThemeHalf(spec: SwatchHalfSpec, tag: String) {
    Row(Modifier.size(spec.width, spec.height).background(spec.fill).testTag(tag).padding(spec.pad), horizontalArrangement = Arrangement.spacedBy(spec.gap)) {
        Box(Modifier.size(spec.railW, spec.railH).clip(RoundedCornerShape(spec.railRadius)).background(spec.railFill).testTag(child(tag, "Rail")))
        Column(Modifier.size(spec.bodyW, spec.bodyH).testTag(child(tag, "Body")), verticalArrangement = Arrangement.spacedBy(spec.bodyGap)) {
            Box(Modifier.size(spec.barW, spec.barH).clip(RoundedCornerShape(spec.barRadius)).background(spec.barFill).testTag(child(tag, "Body/Bar")))
            Row(Modifier.size(spec.contentW, spec.contentH).testTag(child(tag, "Body/Content")), horizontalArrangement = Arrangement.spacedBy(spec.contentGap)) {
                Box(Modifier.size(spec.mapW, spec.mapH).clip(RoundedCornerShape(spec.mapRadius)).background(spec.mapFill).testTag(child(tag, "Body/Content/Map")))
                Column(
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(spec.mapRadius)).background(spec.cardFill).testTag(child(tag, "Body/Content/Card")),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spec.cardGap, Alignment.CenterVertically),
                ) {
                    Text("47", Modifier.testTag(child(tag, "Body/Content/Card/Speed")), color = spec.speedFill, style = spec.speedStyle)
                    Box(Modifier.size(spec.levelW, spec.levelH).clip(RoundedCornerShape(spec.levelRadius)).background(spec.levelFill).testTag(child(tag, "Body/Content/Card/Accent")))
                }
            }
        }
    }
}

@Composable
internal fun LayoutRow(
    title: String,
    subtitle: String,
    side: OnboardingDockSide,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "Layout",
    structure: OnboardingStructure = OnboardingStructure.Standard,
) {
    val ultrawide = structure == OnboardingStructure.Ultrawide
    val width = if (ultrawide) Onboarding.UltrawideLayout.Panel.Layouts.DockLeft.W else Onboarding.Layout.Panel.Layouts.DockLeft.W
    val height = if (ultrawide) Onboarding.UltrawideLayout.Panel.Layouts.DockLeft.H else Onboarding.Layout.Panel.Layouts.DockLeft.H
    val pad = if (ultrawide) Onboarding.UltrawideLayout.Panel.Layouts.DockLeft.PadStart else Onboarding.Layout.Panel.Layouts.DockLeft.PadStart
    val gap = if (ultrawide) Onboarding.UltrawideLayout.Panel.Layouts.DockLeft.Gap else Onboarding.Layout.Panel.Layouts.DockLeft.Gap
    val radius = Onboarding.Layout.Panel.Layouts.DockLeft.Radius
    var cardModifier = modifier.size(width, height).clip(RoundedCornerShape(radius)).background(if (selected) Onboarding.Layout.Panel.Layouts.DockLeft.Fill else Onboarding.Layout.Panel.Layouts.DockRight.Fill)
    if (selected) cardModifier = cardModifier.border(Onboarding.Layout.Panel.Layouts.DockLeft.Stroke, Onboarding.Layout.Panel.Layouts.DockLeft.StrokeColor, RoundedCornerShape(radius))
    cardModifier = cardModifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .padding(pad).testTag(tag)
    if (ultrawide) {
        Column(cardModifier, verticalArrangement = Arrangement.spacedBy(gap)) {
            LayoutPreview(side, structure, child(tag, "Preview"))
            LayoutLabel(title, subtitle, side, selected, structure, child(tag, "Label"), Modifier.fillMaxWidth())
        }
    } else {
        Row(cardModifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            LayoutPreview(side, structure, child(tag, "Preview"))
            LayoutLabel(title, subtitle, side, selected, structure, child(tag, "Label"), Modifier.weight(1f))
        }
    }
}

@Composable
private fun LayoutPreview(side: OnboardingDockSide, structure: OnboardingStructure, tag: String) {
    val ultrawide = structure == OnboardingStructure.Ultrawide
    val width = if (ultrawide) Onboarding.UltrawideLayout.Panel.Layouts.DockLeft.Preview.W else Onboarding.Layout.Panel.Layouts.DockLeft.Preview.W
    val height = if (ultrawide) Onboarding.UltrawideLayout.Panel.Layouts.DockLeft.Preview.H else Onboarding.Layout.Panel.Layouts.DockLeft.Preview.H
    val radius = Onboarding.Layout.Panel.Layouts.DockLeft.Preview.Radius
    Box(
        Modifier.size(width, height).clip(RoundedCornerShape(radius))
            .border(Onboarding.Layout.Panel.Layouts.DockLeft.Preview.Stroke, Onboarding.Layout.Panel.Layouts.DockLeft.Preview.StrokeColor, RoundedCornerShape(radius))
            .testTag(tag),
    ) {
        Row(Modifier.fillMaxSize().background(Onboarding.Welcome.Fill).testTag(child(tag, "Image"))) {
            if (side == OnboardingDockSide.Left) DockPreviewRail()
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            if (side == OnboardingDockSide.Right) DockPreviewRail()
        }
    }
}

@Composable
private fun DockPreviewRail() {
    Column(
        Modifier.width(18.dp).fillMaxHeight().background(Color(0xFF1F2937)),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(3) { Box(Modifier.size(7.dp).background(Color(0xFF94A3B8), RoundedCornerShape(3.dp))) }
    }
}

@Composable
private fun LayoutLabel(
    title: String,
    subtitle: String,
    side: OnboardingDockSide,
    selected: Boolean,
    structure: OnboardingStructure,
    tag: String,
    modifier: Modifier,
) {
    val gap = if (structure == OnboardingStructure.Ultrawide) Onboarding.UltrawideLayout.Panel.Layouts.DockLeft.Label.Gap else Onboarding.Layout.Panel.Layouts.DockLeft.Label.Gap
    Row(modifier.testTag(tag), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (side == OnboardingDockSide.Left) PenIcons.LeftPanelOpen else PenIcons.RightPanelOpen, null, Modifier.size(Onboarding.Layout.Panel.Layouts.DockLeft.Label.Icon.W).testTag(child(tag, "Icon")), if (selected) Onboarding.Layout.Panel.Layouts.DockLeft.Label.Icon.Fill else Onboarding.Layout.Panel.Layouts.DockRight.Label.Icon.Fill)
        Column(Modifier.weight(1f).testTag(child(tag, "Text")), verticalArrangement = Arrangement.spacedBy(Onboarding.Layout.Panel.Layouts.DockLeft.Label.Text.Gap)) {
            Text(title, Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(if (side == OnboardingDockSide.Left) Onboarding.Layout.Panel.Layouts.DockLeft.Label.Text.Title.W else Onboarding.Layout.Panel.Layouts.DockRight.Label.Text.Title.W).testTag(child(tag, "Text/Title")), color = Onboarding.Layout.Panel.Layouts.DockLeft.Label.Text.Title.Fill, style = Onboarding.Layout.Panel.Layouts.DockLeft.Label.Text.Title.Style, maxLines = 1)
            Text(subtitle, Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(if (side == OnboardingDockSide.Left) Onboarding.Layout.Panel.Layouts.DockLeft.Label.Text.Sub.W else Onboarding.Layout.Panel.Layouts.DockRight.Label.Text.Sub.W).testTag(child(tag, "Text/Sub")), color = Onboarding.Layout.Panel.Layouts.DockLeft.Label.Text.Sub.Fill, style = Onboarding.Layout.Panel.Layouts.DockLeft.Label.Text.Sub.Style, maxLines = 1)
        }
        Icon(if (selected) PenIcons.CheckCircle else PenIcons.RadioButtonUnchecked, null, Modifier.size(Onboarding.Layout.Panel.Layouts.DockLeft.Label.Mark.W).testTag(child(tag, "Mark")), if (selected) Onboarding.Layout.Panel.Layouts.DockLeft.Label.Mark.Fill else Onboarding.Layout.Panel.Layouts.DockRight.Label.Mark.Fill)
    }
}

@Composable
internal fun InfoNote(
    text: String,
    modifier: Modifier = Modifier,
    tag: String = "Note",
    ready: Boolean = false,
    showText: Boolean = true,
    singleLine: Boolean = false,
) {
    Row(
        modifier.testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(Onboarding.Permissions.Panel.Note.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (ready) PenIcons.CheckCircle else PenIcons.Info, null,
            Modifier.size(Onboarding.Permissions.Panel.Note.Icon.W).testTag(child(tag, "Icon")),
            if (ready) Onboarding.PermissionsAllowed.Panel.Location.Lead.Icon.Fill else Onboarding.Permissions.Panel.Note.Icon.Fill,
        )
        if (showText) Text(text, Modifier.weight(1f).testTag(child(tag, "Text")),
            color = Onboarding.Permissions.Panel.Note.Text.Fill,
            style = Onboarding.Permissions.Panel.Note.Text.Style,
            softWrap = !singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Visible else TextOverflow.Ellipsis)
    }
}
