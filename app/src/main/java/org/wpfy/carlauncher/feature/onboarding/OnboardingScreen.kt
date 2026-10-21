package org.wpfy.carlauncher.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.wpfy.carlauncher.data.settings.AppTheme
import org.wpfy.carlauncher.data.settings.RailPosition
import org.wpfy.carlauncher.design.theme.CarShape
import org.wpfy.carlauncher.distribution.DistributionFeatures
import org.wpfy.carlauncher.feature.settings.SettingsStateHolder
import org.wpfy.carlauncher.launcher.LauncherApp

// Pencil onboarding palette (warm amber on near-black).
private val ObBackground = Color(0xFF090B0F)
private val ObPanel = Color(0xFF11151C)
private val ObSurface = Color(0xFF181D25)
private val ObBorder = Color(0xFF252C36)
private val ObText = Color(0xFFF5F7FA)
private val ObSubtitle = Color(0xFFBAC2CD)
private val ObMuted = Color(0xFF97A1AF)
private val ObFaint = Color(0xFF727C8B)
private val ObAccent = Color(0xFFFFB13D)
private val ObOnAccent = Color(0xFF17120A)
private val ObAccentTint = Color(0xFF2A2011)
private val ObDot = Color(0xFF3A424E)
private val ObRoute = Color(0xFF4F8CFF)

/**
 * Layout scale. The Pencil frames are 1600x720 px (800x360 dp on a 240 dpi head unit); every size
 * is written in those frame pixels and scaled to the window, so the same composition fits a small
 * 540x324 dp head unit and a large one.
 */
private val LocalObScale = compositionLocalOf { 1f }

@Composable
private fun d(px: Float, min: Float = 0f): Dp = maxOf(px * 0.5f * LocalObScale.current, min).dp

@Composable
private fun d(px: Int, min: Float = 0f): Dp = d(px.toFloat(), min)

@Composable
private fun fs(px: Float, min: Float): TextUnit = maxOf(px * 0.5f * LocalObScale.current, min).sp

@Composable
private fun LegacyOnboardingRoute(stateHolder: SettingsStateHolder) {
    val state = stateHolder.uiState
    var showWelcome by rememberSaveable { mutableStateOf(true) }
    var stepName by rememberSaveable { mutableStateOf(OnboardingStep.Setup.name) }
    val steps = remember {
        onboardingSteps(
            supportsExternalNavigation = DistributionFeatures.supportsExternalNavigation,
            supportsThemeSelection = DistributionFeatures.supportsThemeSelection,
        )
    }
    val step = restoredOnboardingStep(stepName, steps)
    var selectedNavigationAppKey by rememberSaveable { mutableStateOf(state.settings.navigationAppKey) }
    var selectedRailName by rememberSaveable { mutableStateOf(state.settings.railPosition.name) }
    var selectedThemeName by rememberSaveable { mutableStateOf(state.settings.appTheme.name) }

    OnboardingBackground {
        // The welcome screen is step 1; configuration steps follow it.
        val stepCount = steps.size + 1
        if (showWelcome) {
            WelcomeScreen(stepCount = stepCount, onContinue = { showWelcome = false })
            return@OnboardingBackground
        }

        val stepIndex = steps.indexOf(step).coerceAtLeast(0)
        val stepNumber = stepIndex + 2
        val previousStep = steps.getOrNull(stepIndex - 1)
        when (step) {
            OnboardingStep.Setup -> SetupScreen(
                stepNumber = stepNumber,
                stepCount = stepCount,
                onBack = { showWelcome = true },
                onContinue = {
                    stepName = nextOnboardingStep(
                        current = OnboardingStep.Setup,
                        supportsExternalNavigation = DistributionFeatures.supportsExternalNavigation,
                        supportsThemeSelection = DistributionFeatures.supportsThemeSelection,
                    ).name
                },
            )

            OnboardingStep.NavigationApp -> NavigationAppScreen(
                stepNumber = stepNumber,
                stepCount = stepCount,
                apps = state.navigationApps,
                appsLoaded = state.appsLoaded,
                selectedAppKey = selectedNavigationAppKey,
                onSelected = { app ->
                    selectedNavigationAppKey = app.stableKey
                    stateHolder.setNavigationApp(app)
                },
                onBack = { stepName = previousStep?.name ?: OnboardingStep.Setup.name },
                onContinue = { stepName = OnboardingStep.RailPosition.name },
            )

            OnboardingStep.Theme -> ThemeScreen(
                stepNumber = stepNumber,
                stepCount = stepCount,
                selectedTheme = runCatching { AppTheme.valueOf(selectedThemeName) }
                    .getOrDefault(AppTheme.Default),
                onSelected = { theme ->
                    selectedThemeName = theme.name
                    stateHolder.setAppTheme(theme)
                },
                onBack = { stepName = previousStep?.name ?: OnboardingStep.Setup.name },
                onContinue = { stepName = OnboardingStep.RailPosition.name },
            )

            OnboardingStep.RailPosition -> RailPositionScreen(
                stepNumber = stepNumber,
                stepCount = stepCount,
                selectedPosition = runCatching { RailPosition.valueOf(selectedRailName) }
                    .getOrDefault(RailPosition.Left),
                onSelected = { selectedRailName = it.name },
                onBack = { stepName = previousStep?.name ?: OnboardingStep.Setup.name },
                onContinue = stateHolder::completeOnboarding,
            )
        }
    }
}

// region Steps

@Composable
private fun WelcomeScreen(stepCount: Int, onContinue: () -> Unit) {
    OnboardingFrame(
        stepNumber = 1,
        stepCount = stepCount,
        title = "Welcome to Car Launcher",
        subtitle = "Your apps. Your music. Your drive.\nAll in one simple dashboard.",
        continueLabel = "Get Started",
        onContinue = onContinue,
        panelLabel = null,
        fillPanel = true,
    ) {
        DashboardMock(
            rail = null,
            detailed = true,
            modifier = Modifier.fillMaxWidth().aspectRatio(PreviewAspect),
        )
    }
}

@Composable
private fun SetupScreen(
    stepNumber: Int,
    stepCount: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    var refreshGeneration by remember { mutableIntStateOf(0) }
    var locationRequestAttempted by rememberSaveable { mutableStateOf(false) }
    val status = remember(context, refreshGeneration) { readSetupStatus(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshGeneration += 1 }

    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshGeneration += 1
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    OnboardingFrame(
        stepNumber = stepNumber,
        stepCount = stepCount,
        title = "Give Car Launcher what it needs",
        subtitle = "Complete these Android settings once. Tap any item that still needs attention.",
        panelLabel = "Android setup",
        continueEnabled = status.canContinue,
        onBack = onBack,
        onContinue = onContinue,
    ) {
        CapabilityRow(
            title = "Precise location",
            subtitle = if (status.preciseLocation) "Granted" else "Required for location and navigation",
            satisfied = status.preciseLocation,
            onClick = {
                if (status.preciseLocation) return@CapabilityRow
                val permanentlyDenied = locationRequestAttempted &&
                    activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == false
                if (permanentlyDenied) {
                    openAppSettings(context)
                } else {
                    locationRequestAttempted = true
                    locationPermissionLauncher.launch(onboardingLocationPermissions())
                }
            },
        )
        CapabilityRow(
            title = "Notification access",
            subtitle = if (status.notificationAccess) "Enabled" else "Optional. Without it, media controls, now playing and notifications will not work. You can enable it later in Settings > Notification access.",
            satisfied = status.notificationAccess,
            onClick = { openSystemSettings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) },
        )
        CapabilityRow(
            title = "Default Home app",
            subtitle = if (status.defaultHome) {
                "Car Launcher is the default Home app"
            } else {
                "Choose Car Launcher as the default Home app"
            },
            satisfied = status.defaultHome,
            onClick = { openSystemSettings(context, Settings.ACTION_HOME_SETTINGS) },
        )
    }
}

internal fun onboardingLocationPermissions(): Array<String> = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

@Composable
private fun NavigationAppScreen(
    stepNumber: Int,
    stepCount: Int,
    apps: List<LauncherApp>,
    appsLoaded: Boolean,
    selectedAppKey: String?,
    onSelected: (LauncherApp) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingFrame(
        stepNumber = stepNumber,
        stepCount = stepCount,
        title = "Choose your map app",
        subtitle = "Pick the navigation app you want Car Launcher Free to use while you drive.",
        panelLabel = "Navigation",
        onBack = onBack,
        continueEnabled = appsLoaded && (apps.isEmpty() || selectedAppKey != null),
        onContinue = onContinue,
    ) {
        when {
            !appsLoaded -> InfoCard("Looking for installed map apps…")
            apps.isEmpty() -> InfoCard("No compatible map apps are installed. You can choose one later in Settings.")
            else -> apps.forEach { app ->
                SelectionRow(
                    title = app.label,
                    subtitle = app.packageName,
                    selected = app.stableKey == selectedAppKey,
                    onClick = { onSelected(app) },
                )
            }
        }
    }
}

@Composable
private fun ThemeScreen(
    stepNumber: Int,
    stepCount: Int,
    selectedTheme: AppTheme,
    onSelected: (AppTheme) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingFrame(
        stepNumber = stepNumber,
        stepCount = stepCount,
        title = "Make it yours",
        subtitle = "Choose the look you want for Car Launcher.",
        panelLabel = "Theme preview",
        fillPanel = true,
        onBack = onBack,
        onContinue = onContinue,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d(24, 8f))) {
            AppTheme.entries.forEach { theme ->
                ThemeChoiceCard(
                    theme = theme,
                    selected = theme == selectedTheme,
                    onClick = { onSelected(theme) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RailPositionScreen(
    stepNumber: Int,
    stepCount: Int,
    selectedPosition: RailPosition,
    onSelected: (RailPosition) -> Unit,
    onBack: () -> Unit,
    onContinue: (RailPosition) -> Unit,
) {
    OnboardingFrame(
        stepNumber = stepNumber,
        stepCount = stepCount,
        title = "Choose your layout",
        subtitle = "Pick the side that keeps your controls easiest to reach.",
        panelLabel = "Live layout preview",
        fillPanel = true,
        continueLabel = "Finish setup",
        onBack = onBack,
        onContinue = { onContinue(selectedPosition) },
        leftExtra = {
            val options: @Composable (Modifier) -> Unit = { m ->
                RailPosition.entries.forEach { position ->
                    RailOption(
                        position = position,
                        selected = position == selectedPosition,
                        onClick = { onSelected(position) },
                        modifier = m,
                    )
                }
            }
            if (tightLayout()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options(Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d(16, 6f))) {
                    options(Modifier.weight(1f))
                }
            }
        },
    ) {
        DashboardMock(
            rail = selectedPosition,
            detailed = false,
            modifier = Modifier.fillMaxWidth().aspectRatio(PreviewAspect),
        )
        Row(
            modifier = Modifier.padding(top = d(8, 6f)).clip(CarShape.Pill).background(ObAccentTint)
                .border(1.dp, ObAccent.copy(alpha = 0.35f), CarShape.Pill)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Check, null, tint = ObAccent, modifier = Modifier.size(14.dp))
            Text(
                "Rail anchored ${selectedPosition.label.lowercase()}",
                color = ObAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// endregion

// region Frame

private const val PreviewAspect = 16f / 9f

/**
 * One onboarding page: brand, title and subtitle on the left with the step indicator and actions
 * pinned underneath, and a preview panel on the right. On a portrait window the panel moves below
 * the copy and the whole page scrolls except the actions.
 */
@Composable
private fun OnboardingFrame(
    stepNumber: Int,
    stepCount: Int,
    title: String,
    subtitle: String,
    panelLabel: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    continueLabel: String = "Continue",
    continueEnabled: Boolean = true,
    onBack: (() -> Unit)? = null,
    /** Panel content is centred in the panel instead of listed from the top. */
    fillPanel: Boolean = false,
    leftExtra: @Composable ColumnScope.() -> Unit = {},
    panel: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val scale = if (landscape) {
            minOf(maxWidth.value / 800f, maxHeight.value / 360f).coerceIn(0.62f, 1.6f)
        } else {
            1.2f
        }
        CompositionLocalProvider(LocalObScale provides scale) {
            if (landscape) {
                Row(
                    Modifier.fillMaxSize().padding(start = d(72), end = d(80), top = d(58), bottom = d(48)),
                    horizontalArrangement = Arrangement.spacedBy(d(44)),
                ) {
                    Column(Modifier.weight(0.46f).fillMaxHeight()) {
                        BrandRow()
                        if (!tightLayout()) Spacer(Modifier.height(d(28, 12f)))
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                            TitleBlock(title, subtitle)
                            Column(Modifier.padding(top = d(28, 12f)), content = leftExtra)
                        }
                        Spacer(Modifier.height(d(20, 8f)))
                        Footer(stepNumber, stepCount, continueLabel, continueEnabled, onContinue, onBack)
                    }
                    if (panelLabel == null && fillPanel) {
                        // Welcome: the dashboard mock is its own framed surface; wrapping it in a
                        // second panel only added empty bands above and below it.
                        Column(
                            Modifier.weight(0.54f).fillMaxHeight().padding(top = d(22), bottom = d(4)),
                            verticalArrangement = Arrangement.Center,
                            content = panel,
                        )
                    } else {
                        PreviewPanel(
                            label = panelLabel,
                            centered = fillPanel,
                            scrollable = true,
                            modifier = Modifier.weight(0.54f).fillMaxHeight().padding(top = d(22), bottom = d(4)),
                            content = panel,
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 20.dp)) {
                    BrandRow()
                    Spacer(Modifier.height(20.dp))
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        TitleBlock(title, subtitle)
                        Column(content = leftExtra)
                        PreviewPanel(
                            label = panelLabel,
                            centered = false,
                            scrollable = false,
                            modifier = Modifier.fillMaxWidth(),
                            content = panel,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Footer(stepNumber, stepCount, continueLabel, continueEnabled, onContinue, onBack)
                }
            }
        }
    }
}

/** Short window with enlarged system text: drop decoration so the title and actions keep the room. */
@Composable
private fun tightLayout(): Boolean = LocalObScale.current < 0.8f && LocalDensity.current.fontScale > 1.15f

@Composable
private fun BrandRow() {
    if (tightLayout()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d(14, 8f))) {
        Box(
            Modifier.size(d(56, 32f)).clip(CarShape.Control).background(Color(0xFF342815))
                .border(1.dp, ObAccent.copy(alpha = 0.4f), CarShape.Control),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Dashboard, null, tint = ObAccent, modifier = Modifier.size(d(29, 18f)))
        }
        Text(
            "CAR LAUNCHER",
            color = ObMuted,
            fontSize = fs(15f, 11f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun TitleBlock(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(d(20, 8f))) {
        Text(
            title,
            color = ObText,
            fontSize = fs(50f, 20f),
            lineHeight = fs(55f, 24f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        Text(
            subtitle,
            color = ObSubtitle,
            fontSize = fs(22f, 13f),
            lineHeight = fs(32f, 19f),
        )
    }
}

@Composable
private fun Footer(
    stepNumber: Int,
    stepCount: Int,
    continueLabel: String,
    continueEnabled: Boolean,
    onContinue: () -> Unit,
    onBack: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(d(14, 6f))) {
        if (!tightLayout()) {
            Text(
                "STEP $stepNumber OF $stepCount",
                color = ObFaint,
                fontSize = fs(12f, 11f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
        }
        StepDots(stepNumber - 1, stepCount)
        Row(
            horizontalArrangement = Arrangement.spacedBy(d(14, 8f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContinueButton(continueLabel, continueEnabled, onContinue, Modifier.weight(1f, fill = false))
            if (onBack != null) BackButton(onBack)
        }
    }
}

@Composable
private fun ContinueButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CarShape.Control,
        colors = ButtonDefaults.buttonColors(
            containerColor = ObAccent,
            contentColor = ObOnAccent,
            disabledContainerColor = Color(0xFF282D34),
            disabledContentColor = Color(0xFF6F7885),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.widthIn(min = d(240, 112f)).heightIn(min = d(64, 48f)),
    ) {
        Text(label, fontSize = fs(34f, 15f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(d(36, 18f)))
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    // Small head units keep the label for the primary action and show Back as an arrow only.
    val compact = LocalObScale.current < 0.8f
    Button(
        onClick = onClick,
        shape = CarShape.Control,
        colors = ButtonDefaults.buttonColors(containerColor = ObPanel, contentColor = ObText),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObBorder),
        contentPadding = PaddingValues(horizontal = if (compact) 0.dp else 14.dp, vertical = 8.dp),
        modifier = Modifier
            .widthIn(min = if (compact) 48.dp else d(132, 84f))
            .heightIn(min = d(64, 48f))
            .semantics { if (compact) contentDescription = "Back" },
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = ObMuted, modifier = Modifier.size(d(32, 20f)))
        if (!compact) {
            Spacer(Modifier.width(8.dp))
            Text("Back", fontSize = fs(32f, 15f), fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun StepDots(activeIndex: Int, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Step ${activeIndex + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(d(10, 5f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == activeIndex
            Box(
                Modifier.width(if (active) d(36, 22f) else d(12, 6f)).height(d(8, 4f)).clip(CircleShape)
                    .background(if (active) ObAccent else ObDot),
            )
        }
    }
}

@Composable
private fun PreviewPanel(
    label: String?,
    centered: Boolean,
    scrollable: Boolean,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.clip(CarShape.Panel).background(ObPanel).border(1.dp, ObBorder, CarShape.Panel)
            .padding(horizontal = d(28, 14f), vertical = d(24, 12f)),
    ) {
        if (label != null) {
            Text(
                label.uppercase(),
                color = ObMuted,
                fontSize = fs(13f, 11f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = d(16, 8f)),
            )
        }
        val arrangement = if (centered) Arrangement.Center else Arrangement.spacedBy(d(12, 8f))
        if (scrollable) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = arrangement,
                    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                    content = content,
                )
            }
        } else {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(d(12, 8f)),
                horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                content = content,
            )
        }
    }
}

@Composable
private fun OnboardingBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(ObBackground)
            drawRect(
                Brush.radialGradient(
                    listOf(ObAccent.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.08f),
                    radius = size.minDimension * 0.62f,
                ),
            )
        }
        content()
    }
}

// endregion

// region Choices

@Composable
private fun CapabilityRow(title: String, subtitle: String, satisfied: Boolean, onClick: () -> Unit) {
    SelectionRow(
        title = title,
        subtitle = subtitle,
        selected = satisfied,
        selectedDescription = capabilityStatusDescription(satisfied),
        onClick = onClick,
    )
}

@Composable
private fun SelectionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    selectedDescription: String = selectionStatusDescription(selected),
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CarShape.Card)
            .background(if (selected) ObAccentTint else ObSurface)
            .border(1.dp, if (selected) ObAccent.copy(alpha = 0.72f) else ObBorder, CarShape.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = d(22, 12f), vertical = d(16, 10f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ObText, fontSize = fs(28f, 15f), fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                color = ObMuted,
                fontSize = fs(22f, 13f),
                lineHeight = fs(30f, 17f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = selectedDescription,
            tint = if (selected) ObAccent else ObMuted,
            modifier = Modifier.size(d(40, 24f)),
        )
    }
}

@Composable
private fun RailOption(
    position: RailPosition,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CarShape.Control)
            .background(if (selected) ObAccentTint else ObPanel)
            .border(if (selected) 2.dp else 1.dp, if (selected) ObAccent else ObBorder, CarShape.Control)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = selectionStatusDescription(selected) }
            .padding(horizontal = d(16, 8f), vertical = d(14, 8f)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(position.label, color = ObText, fontSize = fs(26f, 14f), fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (LocalObScale.current >= 0.8f) {
            Text(
                if (selected) "Selected" else "Available",
                color = if (selected) ObAccent else ObMuted,
                fontSize = fs(20f, 12f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ThemeChoiceCard(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(CarShape.Card)
            .background(if (selected) ObAccentTint else ObSurface)
            .border(if (selected) 2.dp else 1.dp, if (selected) ObAccent else ObBorder, CarShape.Card)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(d(16, 8f)),
        verticalArrangement = Arrangement.spacedBy(d(12, 6f)),
    ) {
        DashboardMock(
            rail = RailPosition.Left,
            detailed = false,
            route = if (theme == AppTheme.Default) ObRoute else ObAccent,
            modifier = Modifier.fillMaxWidth().aspectRatio(PreviewAspect),
        )
        Text(theme.label, color = ObText, fontSize = fs(30f, 15f), fontWeight = FontWeight.SemiBold, maxLines = 1)
        Row(
            modifier = Modifier.clip(CarShape.Pill)
                .background(if (selected) ObAccent else ObPanel)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = ObOnAccent, modifier = Modifier.size(14.dp))
            Text(
                if (selected) "Selected" else "Available",
                color = if (selected) ObOnAccent else ObMuted,
                fontSize = fs(22f, 12f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Text(
        text = text,
        color = ObMuted,
        fontSize = fs(24f, 14f),
        modifier = Modifier.fillMaxWidth().clip(CarShape.Card).background(ObSurface)
            .border(1.dp, ObBorder, CarShape.Card).padding(16.dp),
    )
}

// endregion

// region Previews

/**
 * Decorative miniature of the launcher. Drawn at font scale 1 so large system text cannot overflow
 * its small cards.
 */
@Composable
private fun DashboardMock(
    rail: RailPosition?,
    detailed: Boolean,
    modifier: Modifier = Modifier,
    route: Color = ObAccent,
) {
    // Laid out once at a fixed design size and scaled as a picture, so text, icons and cards keep
    // their proportions in any panel instead of reflowing and clipping.
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = minOf(
            maxWidth / MockDesignWidth,
            if (constraints.hasBoundedHeight) maxHeight / MockDesignHeight else Float.MAX_VALUE,
        )
        Box(
            Modifier.requiredSize(MockDesignWidth, MockDesignHeight)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            DashboardMockContent(rail, detailed, route)
        }
    }
}

private val MockDesignWidth = 560.dp
private val MockDesignHeight = 315.dp

@Composable
private fun DashboardMockContent(rail: RailPosition?, detailed: Boolean, route: Color) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        val body: @Composable (Modifier) -> Unit = { m ->
            Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewNavigation(Modifier.weight(1.9f).fillMaxHeight(), route, detailed)
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewMedia(Modifier.weight(1f).fillMaxWidth(), route, detailed)
                    if (detailed) PreviewDock(Modifier.fillMaxWidth())
                }
            }
        }
        Box(
            Modifier.fillMaxSize().clip(CarShape.Card).background(Color(0xFF0D1117))
                .border(1.dp, ObBorder, CarShape.Card).padding(10.dp),
        ) {
            when (rail) {
                null -> body(Modifier.fillMaxSize())
                RailPosition.Left -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PreviewRail(horizontal = false, Modifier.fillMaxHeight().width(24.dp))
                    body(Modifier.weight(1f).fillMaxHeight())
                }
                RailPosition.Right -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    body(Modifier.weight(1f).fillMaxHeight())
                    PreviewRail(horizontal = false, Modifier.fillMaxHeight().width(24.dp))
                }
                RailPosition.Bottom -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    body(Modifier.weight(1f).fillMaxWidth())
                    PreviewRail(horizontal = true, Modifier.fillMaxWidth().height(22.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewRail(horizontal: Boolean, modifier: Modifier) {
    val dot = @Composable { Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFE8EAED))) }
    val accent = @Composable { Box(Modifier.size(9.dp).clip(CircleShape).background(ObAccent)) }
    val shape = CarShape.Small
    if (horizontal) {
        Row(
            modifier.clip(shape).background(Color(0xFF10151D)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) { dot(); dot(); dot(); accent() }
    } else {
        Column(
            modifier.clip(shape).background(Color(0xFF10151D)),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { dot(); dot(); dot(); accent() }
    }
}

@Composable
private fun PreviewNavigation(modifier: Modifier, route: Color, detailed: Boolean) {
    Box(modifier.clip(CarShape.Small).background(Color(0xFF10151D))) {
        Canvas(Modifier.fillMaxSize()) {
            val road = Color(0xFF313844)
            drawLine(road, Offset(0f, size.height * .28f), Offset(size.width, size.height * .28f), 2.dp.toPx())
            drawLine(road, Offset(0f, size.height * .62f), Offset(size.width, size.height * .62f), 2.dp.toPx())
            drawLine(road, Offset(size.width * .28f, 0f), Offset(size.width * .28f, size.height), 2.dp.toPx())
            drawLine(road, Offset(size.width * .58f, 0f), Offset(size.width * .58f, size.height), 2.dp.toPx())
            val path = Path().apply {
                moveTo(size.width * .1f, size.height * .78f)
                cubicTo(size.width * .3f, size.height * .74f, size.width * .36f, size.height * .28f, size.width * .78f, size.height * .2f)
            }
            drawPath(path, route, style = Stroke(width = 3.dp.toPx()))
            drawCircle(Color(0xFF1976D2), radius = 5.dp.toPx(), center = Offset(size.width * .1f, size.height * .78f))
        }
        if (detailed) {
            Row(
                Modifier.padding(10.dp).clip(CarShape.Small).background(Color(0xE80A0C10)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Navigation, null, tint = route, modifier = Modifier.size(24.dp))
                Column {
                    Text("400 m", color = ObText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Turn right onto Marine Drive", color = ObMuted, fontSize = 12.sp, maxLines = 1)
                }
            }
            Row(
                Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(CircleShape)
                    .background(Color(0xE80A0C10)).padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("18 min", color = ObText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("· 9.4 km", color = ObMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PreviewMedia(modifier: Modifier, accent: Color, detailed: Boolean) {
    Column(
        modifier.clip(CarShape.Small).background(ObSurface).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(if (detailed) 44.dp else 30.dp).clip(CarShape.Small)
                    .background(Brush.linearGradient(listOf(accent, Color(0xFFE0575F)))),
            )
            if (detailed) {
                Column {
                    Text("Midnight City", color = ObText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("M83 · Spotify", color = ObMuted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF2A323D), CircleShape)) {
            Box(Modifier.fillMaxWidth(.62f).fillMaxHeight().background(accent, CircleShape))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Rounded.SkipPrevious, null, tint = ObMuted, modifier = Modifier.size(20.dp))
            Icon(Icons.Rounded.PlayArrow, null, tint = accent, modifier = Modifier.size(24.dp))
            Icon(Icons.Rounded.SkipNext, null, tint = ObMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PreviewDock(modifier: Modifier) {
    Row(
        modifier.clip(CarShape.Small).background(ObSurface).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            Icons.Rounded.Call to "Phone",
            Icons.Rounded.MusicNote to "Music",
            Icons.Rounded.LocationOn to "Maps",
            Icons.Rounded.Podcasts to "Radio",
            Icons.Rounded.Apps to "Apps",
        ).forEach { (icon, label) ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box(
                    Modifier.size(30.dp).clip(CarShape.Small).background(Color(0xFF1D2531)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = if (label == "Maps") ObAccent else ObText, modifier = Modifier.size(18.dp))
                }
                Text(label, color = ObMuted, fontSize = 10.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

// endregion

internal fun selectionStatusDescription(selected: Boolean): String =
    if (selected) "Selected" else "Not selected"

internal fun capabilityStatusDescription(satisfied: Boolean): String =
    if (satisfied) "Complete" else "Not complete"

internal fun readSetupStatus(context: Context): SetupCapabilityStatus = SetupCapabilityStatus(
    preciseLocation = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
    notificationAccess = notificationAccessEnabled(context),
    defaultHome = isDefaultHome(context),
)

private fun isDefaultHome(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
    val resolved = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}

private fun notificationAccessEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()
    return enabledListeners.split(':').any { component ->
        component.substringBefore('/').equals(context.packageName, ignoreCase = true)
    }
}

internal fun openSystemSettings(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action)) }
        .recoverCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
}

internal fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    runCatching { context.startActivity(intent) }
}

internal fun Context.findComponentActivity(): ComponentActivity? {
    var current: Context? = this
    while (current != null) {
        if (current is ComponentActivity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
