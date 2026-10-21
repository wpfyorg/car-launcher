package org.wpfy.carlauncher.feature.onboarding

internal enum class OnboardingStep {
    Setup,
    NavigationApp,
    Theme,
    RailPosition,
}

internal enum class OnboardingFlowStep {
    Welcome,
    Permissions,
    HomeMode,
    NavigationApp,
    Theme,
    RailPosition,
}

internal const val ONBOARDING_PROGRESS_STEP_COUNT = 5

internal enum class OnboardingEdition {
    Free,
    Pro,
}

internal enum class HomeMode {
    HomeScreen,
    RegularApp,
}

internal data class HomeModeSelection(
    val mode: HomeMode = HomeMode.HomeScreen,
)

internal data class SetupCapabilityStatus(
    val preciseLocation: Boolean,
    val notificationAccess: Boolean,
    val defaultHome: Boolean,
) {
    val canContinue: Boolean
        get() = preciseLocation
}

internal fun onboardingFlowSteps(
    edition: OnboardingEdition,
    supportsThemeSelection: Boolean = false,
): List<OnboardingFlowStep> = buildList {
    add(OnboardingFlowStep.Welcome)
    add(OnboardingFlowStep.Permissions)
    add(OnboardingFlowStep.HomeMode)
    when (edition) {
        OnboardingEdition.Free -> add(OnboardingFlowStep.NavigationApp)
        OnboardingEdition.Pro -> if (supportsThemeSelection) add(OnboardingFlowStep.Theme)
    }
    add(OnboardingFlowStep.RailPosition)
}

internal fun restoredOnboardingStep(
    name: String,
    steps: Collection<OnboardingFlowStep>,
): OnboardingFlowStep? = steps.firstOrNull { it.name == name }
    ?: steps.firstOrNull()

internal fun nextOnboardingStep(
    current: OnboardingStep,
    supportsExternalNavigation: Boolean,
    supportsThemeSelection: Boolean,
): OnboardingStep {
    return when (current) {
        OnboardingStep.Setup -> if (supportsExternalNavigation) {
            OnboardingStep.NavigationApp
        } else if (supportsThemeSelection) {
            OnboardingStep.Theme
        } else {
            OnboardingStep.RailPosition
        }
        OnboardingStep.NavigationApp -> OnboardingStep.RailPosition
        OnboardingStep.Theme -> OnboardingStep.RailPosition
        OnboardingStep.RailPosition -> OnboardingStep.RailPosition
    }
}

internal fun onboardingSteps(
    supportsExternalNavigation: Boolean,
    supportsThemeSelection: Boolean,
): List<OnboardingStep> = buildList {
    add(OnboardingStep.Setup)
    when {
        supportsExternalNavigation -> add(OnboardingStep.NavigationApp)
        supportsThemeSelection -> add(OnboardingStep.Theme)
    }
    add(OnboardingStep.RailPosition)
}

/** The step a saved [name] resumes at: itself when this build still has it, otherwise the first step (a step can be dropped by an update). */
internal fun restoredOnboardingStep(name: String, steps: List<OnboardingStep>): OnboardingStep =
    runCatching { OnboardingStep.valueOf(name) }.getOrNull()?.takeIf { it in steps } ?: steps.first()
