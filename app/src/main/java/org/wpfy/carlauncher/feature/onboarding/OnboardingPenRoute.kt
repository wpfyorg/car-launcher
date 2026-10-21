package org.wpfy.carlauncher.feature.onboarding

import android.Manifest
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.wpfy.carlauncher.R
import org.wpfy.carlauncher.data.settings.RailPosition
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.PenScreens.Onboarding
import org.wpfy.carlauncher.distribution.DistributionFeatures
import org.wpfy.carlauncher.feature.settings.SettingsStateHolder

/** A Pen frame supplies every measure; the outer viewport only selects and fits its layout. */
@Composable
internal fun OnboardingRoute(stateHolder: SettingsStateHolder, statusOverride: SetupCapabilityStatus? = null) {
    val state = stateHolder.uiState
    val edition = if (DistributionFeatures.isProEdition) OnboardingEdition.Pro else OnboardingEdition.Free
    val steps = remember(edition, DistributionFeatures.supportsThemeSelection) {
        onboardingFlowSteps(edition, DistributionFeatures.supportsThemeSelection)
    }
    var stepName by rememberSaveable { mutableStateOf(OnboardingFlowStep.Welcome.name) }
    val current = restoredOnboardingStep(stepName, steps) ?: OnboardingFlowStep.Welcome
    val stepNumber = steps.indexOf(current) + 1
    var modeName by rememberSaveable { mutableStateOf(HomeMode.HomeScreen.name) }
    val homeMode = HomeMode.valueOf(modeName)
    var appKey by rememberSaveable { mutableStateOf(state.settings.navigationAppKey) }
    var railName by rememberSaveable { mutableStateOf(state.settings.railPosition.name) }
    var themeName by rememberSaveable { mutableStateOf(OnboardingThemeChoice.Auto.name) }
    val theme = OnboardingThemeChoice.valueOf(themeName)

    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    var refresh by remember { mutableIntStateOf(0) }
    var locationAttempted by rememberSaveable { mutableStateOf(false) }
    val permissions = statusOverride ?: remember(context, refresh) { readSetupStatus(context) }
    val requestLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refresh += 1 }
    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh += 1
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    val advance = {
        val next = steps.getOrNull(stepNumber)
        if (next != null) stepName = next.name
    }
    val back = {
        val previous = steps.getOrNull(stepNumber - 2)
        if (previous != null) stepName = previous.name
    }
    val continueStep = {
        when (current) {
            OnboardingFlowStep.Permissions -> if (permissions.canContinue) advance()
            OnboardingFlowStep.HomeMode -> {
                if (homeMode == HomeMode.HomeScreen && !permissions.defaultHome) {
                    openSystemSettings(context, Settings.ACTION_HOME_SETTINGS)
                }
                advance()
            }
            OnboardingFlowStep.RailPosition ->
                stateHolder.completeOnboarding(RailPosition.valueOf(railName))
            else -> advance()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Onboarding.Welcome.Fill)) {
        val standardRatio = Onboarding.Welcome.W.value / Onboarding.Welcome.H.value
        val ultrawideRatio = Onboarding.UltrawideWelcome.W.value / Onboarding.UltrawideWelcome.H.value
        val ratio = maxWidth.value / maxHeight.value
        val viewportStructure = when {
            maxHeight > maxWidth -> OnboardingStructure.Portrait
            ratio >= (standardRatio + ultrawideRatio) / 2f -> OnboardingStructure.Ultrawide
            maxWidth < (Onboarding.Welcome.W + Onboarding.CompactPermissions.W) / 2 ->
                OnboardingStructure.Compact
            else -> OnboardingStructure.Standard
        }
        val structure = onboardingDesignStructure(current, viewportStructure)
        val frameW: Dp
        val frameH: Dp
        when (structure) {
            OnboardingStructure.Portrait -> {
                frameW = Onboarding.PortraitHomeOrApp.W
                frameH = Onboarding.PortraitHomeOrApp.H
            }
            OnboardingStructure.Ultrawide -> {
                frameW = Onboarding.UltrawideWelcome.W
                frameH = Onboarding.UltrawideWelcome.H
            }
            OnboardingStructure.Compact -> {
                frameW = Onboarding.CompactPermissions.W
                frameH = Onboarding.CompactPermissions.H
            }
            OnboardingStructure.Standard -> {
                frameW = Onboarding.Welcome.W
                frameH = Onboarding.Welcome.H
            }
        }
        val scale = minOf(maxWidth / frameW, maxHeight / frameH)
        Box(
            Modifier.offset(
                x = (maxWidth - frameW * scale) / 2,
                y = (maxHeight - frameH * scale) / 2,
            ).wrapContentSize(Alignment.TopStart, unbounded = true).size(frameW, frameH).graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            },
        ) {
            val label = when (current) {
                OnboardingFlowStep.Welcome -> "WELCOME"
                OnboardingFlowStep.Permissions -> "PERMISSIONS"
                OnboardingFlowStep.HomeMode -> "HOME SCREEN"
                OnboardingFlowStep.NavigationApp -> "MAP APP"
                OnboardingFlowStep.Theme -> "APPEARANCE"
                OnboardingFlowStep.RailPosition -> "LAYOUT"
            }
            val title = when (current) {
                OnboardingFlowStep.Welcome -> "Welcome to Car Launcher"
                OnboardingFlowStep.Permissions -> "Allow what it needs"
                OnboardingFlowStep.HomeMode -> "How will you use it?"
                OnboardingFlowStep.NavigationApp -> "Choose your map app"
                OnboardingFlowStep.Theme -> "Make it yours"
                OnboardingFlowStep.RailPosition -> "Choose your layout"
            }
            val description = when (current) {
                OnboardingFlowStep.Welcome -> "Your maps, music and calls in one dashboard built for the road."
                OnboardingFlowStep.Permissions -> "Grant these once. You can change any of them later in Settings."
                OnboardingFlowStep.HomeMode -> "Pick what suits your car. You can switch any time in Settings › Home app."
                OnboardingFlowStep.NavigationApp -> "Car Launcher Free opens this app when you start navigation."
                OnboardingFlowStep.Theme -> "Choose the look for the road. Auto follows day and night."
                OnboardingFlowStep.RailPosition -> "Put the dock on the side nearest the driver."
            }
            val enabled = current != OnboardingFlowStep.Permissions || permissions.canContinue
            val cta = when (current) {
                OnboardingFlowStep.Welcome -> "Get started"
                OnboardingFlowStep.RailPosition -> "Finish setup"
                else -> "Continue"
            }
            val controls: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                val actionTag = if (structure == OnboardingStructure.Portrait) "Screen/Actions" else "Screen/Intro/Actions"
                if (current != OnboardingFlowStep.Welcome) BackButton(
                    onClick = back, tag = "$actionTag/Back", structure = structure,
                )
                PrimaryButton(
                    label = cta, onClick = continueStep, enabled = enabled,
                    tag = "$actionTag/Continue", structure = structure,
                    fullWidth = current == OnboardingFlowStep.Welcome,
                )
            }
            OnboardingScaffold(
                modifier = Modifier.fillMaxSize(),
                tag = "Screen",
                structure = structure,
                welcome = current == OnboardingFlowStep.Welcome,
                intro = {
                    Intro(
                        step = "STEP $stepNumber OF ${steps.size}  ·  $label",
                        title = title, body = description, progressStep = stepNumber,
                        progressCount = steps.size,
                        structure = structure, tag = "Screen/Intro",
                        showBrand = structure != OnboardingStructure.Compact,
                        actions = if (structure == OnboardingStructure.Portrait) null else controls,
                    )
                },
                actions = if (structure == OnboardingStructure.Portrait) {
                    { Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Onboarding.PortraitHomeOrApp.Actions.Gap), content = controls) }
                } else null,
                panel = {
                    when (current) {
                        OnboardingFlowStep.Welcome -> WelcomeContent(structure)
                        OnboardingFlowStep.Permissions -> PermissionContent(
                            structure, permissions,
                            onLocation = {
                                val deniedPermanently = locationAttempted &&
                                    activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == false
                                if (deniedPermanently) openAppSettings(context)
                                else {
                                    locationAttempted = true
                                    requestLocation.launch(arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                    ))
                                }
                            },
                            onNotification = { openSystemSettings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) },
                        )
                        OnboardingFlowStep.HomeMode -> HomeModeContent(structure, homeMode, permissions.defaultHome) {
                            modeName = it.name
                        }
                        OnboardingFlowStep.NavigationApp -> AppContent(
                            state.navigationApps, state.appsLoaded, appKey,
                        ) { app ->
                            appKey = app.stableKey
                            stateHolder.setNavigationApp(app)
                        }
                        OnboardingFlowStep.Theme -> ThemeContent(theme) { themeName = it.name }
                        OnboardingFlowStep.RailPosition -> LayoutContent(structure, railName) { railName = it.name }
                    }
                },
            )
        }
    }
}

/** Use only the responsive variants drawn in Pen; fit its Standard frame for other steps. */
internal fun onboardingDesignStructure(
    step: OnboardingFlowStep,
    viewport: OnboardingStructure,
): OnboardingStructure = when (viewport) {
    OnboardingStructure.Compact, OnboardingStructure.Portrait ->
        if (step == OnboardingFlowStep.Permissions || step == OnboardingFlowStep.HomeMode) viewport
        else OnboardingStructure.Standard
    OnboardingStructure.Ultrawide ->
        if (step == OnboardingFlowStep.Welcome || step == OnboardingFlowStep.RailPosition) viewport
        else OnboardingStructure.Standard
    OnboardingStructure.Standard -> OnboardingStructure.Standard
}

@Composable
internal fun WelcomeContent(structure: OnboardingStructure) {
    val wide = structure == OnboardingStructure.Ultrawide
    val heroW = if (wide) Onboarding.UltrawideWelcome.Panel.Hero.W else Onboarding.Welcome.Panel.Hero.W
    val heroH = if (wide) Onboarding.UltrawideWelcome.Panel.Hero.H else Onboarding.Welcome.Panel.Hero.H
    val highlights: @Composable () -> Unit = {
        val highlightModifier = if (wide) Modifier.size(
            Onboarding.UltrawideWelcome.Panel.Highlights.W, Onboarding.UltrawideWelcome.Panel.Highlights.H,
        ).testTag("Screen/Panel/Highlights").padding(
            start = Onboarding.UltrawideWelcome.Panel.Highlights.PadStart,
            end = Onboarding.UltrawideWelcome.Panel.Highlights.PadEnd,
        ) else Modifier.size(
            Onboarding.Welcome.Panel.Highlights.W, Onboarding.Welcome.Panel.Highlights.H,
        ).testTag("Screen/Panel/Highlights").padding(
            start = Onboarding.Welcome.Panel.Highlights.PadStart,
            top = Onboarding.Welcome.Panel.Highlights.PadTop,
            end = Onboarding.Welcome.Panel.Highlights.PadEnd,
            bottom = Onboarding.Welcome.Panel.Highlights.PadBottom,
        )
        if (wide) {
            Column(highlightModifier,
                verticalArrangement = Arrangement.spacedBy(Onboarding.UltrawideWelcome.Panel.Highlights.Gap)) {
                WelcomeHighlight(0, true)
                WelcomeHighlight(1, true)
                WelcomeHighlight(2, true)
            }
        } else {
            Row(highlightModifier) {
                WelcomeHighlight(0, false)
                WelcomeHighlight(1, false)
                WelcomeHighlight(2, false)
            }
        }
    }
    if (wide) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(
            Onboarding.UltrawideWelcome.Panel.Gap,
        ), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(heroW, heroH)
                .clip(RoundedCornerShape(Onboarding.UltrawideWelcome.Panel.Hero.Radius))
                .testTag("Screen/Panel/Hero"), contentScale = ContentScale.Crop)
            highlights()
        }
    } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Onboarding.Welcome.Panel.Gap)) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(heroW, heroH)
                .clip(RoundedCornerShape(Onboarding.Welcome.Panel.Hero.Radius))
                .testTag("Screen/Panel/Hero"), contentScale = ContentScale.Crop)
            highlights()
        }
    }
}

@Composable
private fun WelcomeHighlight(index: Int, wide: Boolean) {
    val path = "Screen/Panel/Highlights/Highlight${index + 1}"
    val width = if (wide) Onboarding.UltrawideWelcome.Panel.Highlights.Highlight1.W else Onboarding.Welcome.Panel.Highlights.Highlight1.W
    val height = if (!wide && index == 1) Onboarding.Welcome.Panel.Highlights.Highlight2.H else Onboarding.Welcome.Panel.Highlights.Highlight1.H
    val labelWidth = if (wide) Onboarding.UltrawideWelcome.Panel.Highlights.Highlight1.Label.W else Onboarding.Welcome.Panel.Highlights.Highlight1.Label.W
    val labelHeight = if (!wide && index == 1) Onboarding.Welcome.Panel.Highlights.Highlight2.Label.H else Onboarding.Welcome.Panel.Highlights.Highlight1.Label.H
    val label = when (index) {
        0 -> "Turn-by-turn"
        1 -> "Speed and limits"
        else -> "Media controls"
    }
    val icon = when (index) {
        0 -> PenIcons.Navigation
        1 -> PenIcons.Speed
        else -> PenIcons.MusicNote
    }
    Row(Modifier.size(width, height).background(
        Onboarding.Welcome.Panel.Highlights.Highlight1.Fill,
        RoundedCornerShape(Onboarding.Welcome.Panel.Highlights.Highlight1.Radius),
    ).testTag(path).padding(start = Onboarding.Welcome.Panel.Highlights.Highlight1.PadStart,
        end = Onboarding.Welcome.Panel.Highlights.Highlight1.PadEnd), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Onboarding.Welcome.Panel.Highlights.Highlight1.Gap)) {
        Box(Modifier.size(Onboarding.Welcome.Panel.Highlights.Highlight1.Badge.W)
            .background(Onboarding.Welcome.Panel.Highlights.Highlight1.Badge.Fill,
                RoundedCornerShape(Onboarding.Welcome.Panel.Highlights.Highlight1.Badge.Radius))
            .testTag("$path/Badge"), contentAlignment = Alignment.Center) {
            Icon(icon, null,
                Modifier.size(Onboarding.Welcome.Panel.Highlights.Highlight1.Badge.Icon.W).testTag("$path/Badge/Icon"),
                tint = Onboarding.Welcome.Panel.Highlights.Highlight1.Badge.Icon.Fill)
        }
        Text(label, Modifier.size(labelWidth, labelHeight).testTag("$path/Label"),
            color = Onboarding.Welcome.Panel.Highlights.Highlight1.Label.Fill,
            style = Onboarding.Welcome.Panel.Highlights.Highlight1.Label.Style)
    }
}

@Composable
internal fun PermissionContent(
    structure: OnboardingStructure,
    status: SetupCapabilityStatus,
    onLocation: () -> Unit,
    onNotification: () -> Unit,
) {
    val compact = structure == OnboardingStructure.Compact
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(
        if (compact) Onboarding.CompactPermissions.Panel.Gap else Onboarding.Permissions.Panel.Gap,
    )) {
        if (!compact) SectionLabel("REQUIRED", tag = "Screen/Panel/SectionRequired")
        PermissionRow(
            title = "Precise location",
            subtitle = if (status.preciseLocation) "Precise location is on."
                else "Speed, speed limits and navigation need your exact position.",
            allowed = status.preciseLocation, onAllow = onLocation, structure = structure,
            tag = "Screen/Panel/Location",
            showSubtitle = !compact,
        )
        if (!compact) {
            val sectionGapWidth = if (structure == OnboardingStructure.Portrait) {
                Onboarding.PortraitPermissions.Panel.SectionGap.W
            } else Onboarding.Permissions.Panel.SectionGap.W
            val sectionGapHeight = if (structure == OnboardingStructure.Portrait) {
                Onboarding.PortraitPermissions.Panel.SectionGap.H
            } else Onboarding.Permissions.Panel.SectionGap.H
            Spacer(Modifier.size(sectionGapWidth, sectionGapHeight).testTag("Screen/Panel/SectionGap"))
            SectionLabel("RECOMMENDED", tag = "Screen/Panel/SectionRecommended")
        }
        PermissionRow(
            title = "Notification access",
            subtitle = "Shows now playing, media controls and message alerts.",
            allowed = status.notificationAccess, onAllow = onNotification,
            allowLabel = "Enable", structure = structure, tag = "Screen/Panel/Notifications",
            icon = org.wpfy.carlauncher.design.kit.PenIcons.Notifications,
            showSubtitle = !compact,
            notifications = true,
        )
        Spacer(Modifier.weight(1f).fillMaxWidth().testTag("Screen/Panel/Spacer"))
        if (!compact && structure != OnboardingStructure.Portrait) {
            val noteWidth = if (status.canContinue) {
                Onboarding.PermissionsAllowed.Panel.Note.W
            } else {
                Onboarding.Permissions.Panel.Note.W
            }
            InfoNote(
                if (status.canContinue) "Ready to continue. You can allow the rest later in Settings."
                else "Only precise location is needed to continue.",
                modifier = Modifier.width(noteWidth),
                tag = "Screen/Panel/Note", ready = status.canContinue, singleLine = true,
            )
        }
    }
}

@Composable
internal fun HomeModeContent(
    structure: OnboardingStructure,
    mode: HomeMode,
    defaultHome: Boolean,
    select: (HomeMode) -> Unit,
) {
    val vertical = structure == OnboardingStructure.Portrait
    val home: @Composable () -> Unit = {
        ChoiceCard(
            "Home screen", "Car Launcher replaces your home screen.",
            mode == HomeMode.HomeScreen, { select(HomeMode.HomeScreen) },
            tag = "Screen/Panel/Modes/HomeScreen", structure = structure,
            points = listOf("Opens when the car starts", "Home button returns here", "Android asks you to confirm"),
        )
    }
    val regular: @Composable () -> Unit = {
        ChoiceCard(
            "Regular app", "Keep your home screen. Open Car Launcher from your apps when you drive.",
            mode == HomeMode.RegularApp, { select(HomeMode.RegularApp) },
            tag = "Screen/Panel/Modes/RegularApp", structure = structure,
            kind = OnboardingChoiceKind.RegularApp,
            points = listOf("Nothing on the system changes", "Great for phones and tablets", "Set as Home later anytime"),
        )
    }
    val panelGap = when (structure) {
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Gap
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Gap
        else -> Onboarding.HomeOrApp.Panel.Gap
    }
    val gap = when (structure) {
        OnboardingStructure.Portrait -> Onboarding.PortraitHomeOrApp.Panel.Modes.Gap
        OnboardingStructure.Compact -> Onboarding.CompactHomeOrApp.Panel.Modes.Gap
        else -> Onboarding.HomeOrApp.Panel.Modes.Gap
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(panelGap)) {
        if (vertical) Column(Modifier.testTag("Screen/Panel/Modes"), verticalArrangement = Arrangement.spacedBy(gap)) {
            home()
            regular()
        } else Row(Modifier.testTag("Screen/Panel/Modes"), horizontalArrangement = Arrangement.spacedBy(gap)) {
            home()
            regular()
        }
        if (!vertical) InfoNote(
            when {
                mode == HomeMode.RegularApp -> "Continue keeps your current home screen. You can change this later in Settings."
                defaultHome -> "Car Launcher is already your home screen. Continue to choose your layout."
                else -> "Continue opens Android's Home picker. Choose Car Launcher, then Always."
            },
            tag = "Screen/Panel/Note",
        )
    }
}

@Composable
internal fun AppContent(
    apps: List<org.wpfy.carlauncher.launcher.LauncherApp>,
    loaded: Boolean,
    selectedKey: String?,
    choose: (org.wpfy.carlauncher.launcher.LauncherApp) -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Onboarding.MapAppFree.Panel.Gap)) {
        SectionLabel("INSTALLED NAVIGATION APPS", tag = "Screen/Panel/Section")
        apps.take(4).forEach { app ->
            AppChoiceRow(app.label, app.packageName, app.stableKey == selectedKey, { choose(app) },
                tag = "Screen/Panel/${app.label.filter { it.isLetterOrDigit() }}")
        }
        Spacer(Modifier.weight(1f).fillMaxWidth().testTag("Screen/Panel/Spacer"))
        InfoNote(
            if (loaded) "Don't see yours? Install it, then pick it in Settings › Navigation app."
            else "Looking for installed navigation apps.",
            tag = "Screen/Panel/Note",
        )
    }
}

@Composable
internal fun ThemeContent(
    selected: OnboardingThemeChoice,
    select: (OnboardingThemeChoice) -> Unit,
) {
    Column(Modifier.fillMaxSize().testTag("Screen/Panel/Themes"),
        verticalArrangement = Arrangement.spacedBy(Onboarding.ThemePro.Panel.Themes.Gap)) {
        ThemeRow("Auto", "Switches at sunset", OnboardingThemeChoice.Auto,
            selected == OnboardingThemeChoice.Auto, { select(OnboardingThemeChoice.Auto) },
            tag = "Screen/Panel/Themes/Auto")
        ThemeRow("Dark", "Easy on the eyes", OnboardingThemeChoice.Dark,
            selected == OnboardingThemeChoice.Dark, { select(OnboardingThemeChoice.Dark) },
            tag = "Screen/Panel/Themes/Dark")
        ThemeRow("Light", "For bright days", OnboardingThemeChoice.Light,
            selected == OnboardingThemeChoice.Light, { select(OnboardingThemeChoice.Light) },
            tag = "Screen/Panel/Themes/Light")
    }
}

@Composable
internal fun LayoutContent(structure: OnboardingStructure, selected: String, select: (RailPosition) -> Unit) {
    val wide = structure == OnboardingStructure.Ultrawide
    val gap = if (wide) Onboarding.UltrawideLayout.Panel.Layouts.Gap else Onboarding.Layout.Panel.Layouts.Gap
    val left: @Composable () -> Unit = {
        LayoutRow("Dock left", "Left-hand drive", OnboardingDockSide.Left,
            selected == RailPosition.Left.name, { select(RailPosition.Left) },
            tag = "Screen/Panel/Layouts/DockLeft", structure = structure)
    }
    val right: @Composable () -> Unit = {
        LayoutRow("Dock right", "Right-hand drive", OnboardingDockSide.Right,
            selected == RailPosition.Right.name, { select(RailPosition.Right) },
            tag = "Screen/Panel/Layouts/DockRight", structure = structure)
    }
    if (wide) Row(Modifier.fillMaxSize().testTag("Screen/Panel/Layouts"),
        horizontalArrangement = Arrangement.spacedBy(gap)) {
        left()
        right()
    } else Column(Modifier.fillMaxSize().testTag("Screen/Panel/Layouts"),
        verticalArrangement = Arrangement.spacedBy(gap)) {
        left()
        right()
    }
}
