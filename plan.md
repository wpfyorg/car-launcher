# OpenLauncher — Fresh Implementation Plan

_Last updated: 2026-09-01_

## 0. Mission

Rebuild OpenLauncher from the ground up using `Launcher UI Kit.pen` as the visual and interaction source of truth.

The existing tracked Kotlin UI is **not** the foundation for the new implementation. It may be inspected only to recover behavior that is still wanted later. The new launcher must be designed and implemented cleanly around the UI kit, Android 9/API 28 compatibility, the YT5760D/AC8257 head unit, and a clear separation between the Free external-navigation build and the Paid native-navigation build.

The implementation order is:

```text
UI kit → design system → static screens → navigation shell → launcher behavior
→ media/system integrations → Free embedded navigation → Paid native navigation
→ offline maps/search → hardware hardening → releases
```

The first milestone is not “make the old app look different.” It is “prove a clean new launcher shell from the UI kit on Android 9 hardware.”

---

# 1. Hard Requirements

## Target device

Primary hardware:

- YT5760D / AC8257 family
- Android 9 / API 28
- arm64-v8a
- 1600 × 720 display
- approximately 160 dpi on the tested unit
- PowerVR Rogue GE8300-class GPU
- 4 GB RAM class device

The launcher must remain usable if other Android 9 head units differ in DPI, aspect ratio, nav-bar behavior, or available system APIs.

## Android baseline

- Minimum supported Android: **Android 9 / API 28**.
- Kotlin + Jetpack Compose are acceptable if every dependency used remains API-28 compatible.
- The Free/GitHub build may have a compatibility target/configuration specifically for Android 9 task embedding.
- The Google Play build must target the current Play-required SDK and must not contain hidden/system task APIs.

## Product split

One repository, shared launcher shell, two product paths:

### OpenLauncher Free / GitHub

- full launcher UI
- installed-app launcher
- media integration
- system/status integrations
- external navigation app embedded into the launcher where the head unit supports it
- no bundled native offline navigation stack
- may ship a normal APK plus a YT5760D-specific compatibility artifact if testing proves privileges are required

### OpenLauncher Paid / Google Play

- same launcher UI and shared navigation state/UI
- integrated MapLibre map renderer
- Ferrostar navigation/session layer
- Valhalla routing
- downloadable offline regional data
- offline POI search
- Play Services may be used in this build where beneficial
- no privileged task embedding or hidden Android APIs

---

# 2. Source of Truth: `Launcher UI Kit.pen`

`Launcher UI Kit.pen` is the design reference. Do not manually reinterpret it from memory when implementing a component that exists in the file.

The file currently contains Android-for-Cars style components and complete layout examples including:

- list rows
- grid items
- headers
- header leading/trailing actions
- row actions
- switches, radio buttons, checkboxes
- sign-in patterns
- pane templates
- tabs
- search
- messages/alerts
- sectioned lists
- app grids
- media screens
- map/navigation components
- ETA cards
- trip info
- turn cards
- map content actions
- canonical layouts
- widescreen layouts
- notification center layouts
- vertical rail/status layouts

Complete example layouts exist at roughly:

- 748 × 450
- 800 × 480
- 880 × 480
- 1120 × 480
- 1280 × 480
- 790 × 686
- 800 × 880
- 1920 × 584

The YT5760D target is 1600 × 720. The new launcher must therefore use the kit as a **component/layout system**, not hard-code a screenshot at a single reference size.

---

# 3. Design Translation Strategy

## 3.1 Do not pixel-scale the entire UI

Build adaptive Compose components from the kit.

Use these principles:

- preserve hierarchy and proportions;
- preserve touch target sizes;
- preserve rail/content relationships;
- preserve typography hierarchy;
- preserve card radius and spacing rhythm;
- adapt widths using constraints rather than one global scale factor;
- let map/media/list content consume additional widescreen space;
- use all eight `Ui.pen` reference breakpoints to validate the adaptive shell; the 1600 × 720 head unit is an additional hardware target that adapts from the closest widescreen structures by constraints rather than pixel scaling.

## 3.2 YT5760D reference viewport

Create a development preview configuration for:

```text
1600 × 720
landscape
160 dpi reference
```

This becomes the primary screenshot/regression viewport.

Also maintain previews for:

```text
748 × 450   compact canonical landscape / horizontal rail
800 × 480   canonical landscape / horizontal rail
880 × 480   canonical landscape / vertical rail
1120 × 480  large canonical landscape / vertical rail
1280 × 480  widescreen / vertical rail
790 × 686   short portrait / horizontal rail / no split screen
800 × 880   standard portrait / horizontal rail / split screen
1920 × 584  BMW iX large widescreen / vertical rail
```

These eight `Ui.pen` sizes are the responsive regression matrix. The app should interpolate between them without special-casing the exact device model in UI code.

## 3.3 Extract reusable design tokens

The `.pen` file repeatedly uses values around:

- spacing: 4, 8, 10, 12, 16, 24, 32
- corner radii: 16, 24, 36, 48, 56, 64 and pill/circle shapes
- typography sizes centered around 24, 28, 32, 34, 36, 38, 42 in the source design
- dark surfaces around `#242629`, `#252626`, `#393E45`
- light surfaces around `#F1F0F4`, `#DEE3EB`
- blue accent families including `#578CFF`, `#0057CC`, `#1A73E8`, `#B6D8FF`

Do not blindly copy every raw value. Consolidate them into a deliberate token set after comparing the reusable kit components.

Proposed Compose token files:

```text
ui/theme/CarColors.kt
ui/theme/CarTypography.kt
ui/theme/CarSpacing.kt
ui/theme/CarShapes.kt
ui/theme/CarDimensions.kt
ui/theme/CarTheme.kt
```

---

# 4. Fresh Codebase Boundary

The first implementation commit should create a real clean boundary.

## Keep

Keep only infrastructure that is genuinely reusable:

- Gradle wrapper
- top-level Gradle files
- application package/application ID unless there is a deliberate reason to change it
- launcher icon if still wanted
- repository history
- `.gitignore`
- `Launcher UI Kit.pen`

## Rebuild

Rebuild from scratch:

- `MainActivity`
- navigation graph/router
- UI theme
- rail
- home/dashboard
- app grid
- settings
- media UI
- notification UI
- search UI
- system/status models
- view models/state holders
- repositories/services
- launcher app discovery
- navigation architecture

## Do not copy old implementation wholesale

If a prior feature is reintroduced later, reimplement it behind the new contracts. This avoids pulling the old oversized `LauncherViewModel`, old widget architecture, old styling system, and old UI assumptions into the fresh design.

---

# 5. Proposed Package Structure

```text
com.openlauncher.app/
│
├── MainActivity.kt
├── OpenLauncherApp.kt
│
├── core/
│   ├── model/
│   ├── platform/
│   ├── permissions/
│   ├── util/
│   └── logging/
│
├── design/
│   ├── theme/
│   ├── component/
│   ├── icon/
│   └── preview/
│
├── shell/
│   ├── LauncherShell.kt
│   ├── Rail.kt
│   ├── StatusArea.kt
│   ├── ShellState.kt
│   └── ShellDestination.kt
│
├── feature/
│   ├── home/
│   ├── apps/
│   ├── settings/
│   ├── media/
│   ├── notifications/
│   ├── search/
│   └── navigation/
│
├── launcher/
│   ├── AppCatalog.kt
│   ├── AppLauncher.kt
│   └── LauncherRoleController.kt
│
├── media/
│   ├── MediaRepository.kt
│   ├── MediaSessionController.kt
│   └── NotificationListener.kt
│
├── location/
│   ├── LocationProvider.kt
│   └── FrameworkLocationProvider.kt
│
└── data/
    ├── settings/
    └── persistence/
```

Free- and Paid-only implementations should live in flavor-specific source sets later rather than leaking distribution logic into shared UI packages.

---

# 6. State Architecture

Use small state holders instead of one all-purpose ViewModel.

Suggested state groups:

```text
ShellState
AppCatalogState
MediaState
SystemStatusState
SettingsState
NotificationState
NavigationState
SearchState
```

Use immutable UI state + explicit events.

Example:

```text
AppsUiState
- loading
- apps
- category/filter
- selectedIndex
- searchQuery

AppsAction
- OpenApp
- Search
- ScrollToLetter
- OpenSettings
```

The screen composables should render state and emit actions. Platform code should not be called directly from arbitrary UI composables.

---

# 7. Phase 1 — Empty Fresh Launcher Skeleton

Goal: remove dependency on the old UI and prove a clean application shell on Android 9.

## Work

1. Create fresh theme and root activity.
2. Create `LauncherShell`.
3. Force/handle landscape cleanly.
4. Handle immersive mode and head-unit system bars.
5. Create a blank rail + content surface.
6. Add a debug/device info overlay available only in debug builds.
7. Set `minSdk = 28` once the fresh shell compiles.
8. Keep target SDK decision separated for Free vs Play variants later.

## Verify

- `./gradlew assembleDebug`
- install on API 28 arm64 emulator
- cold launch
- rotate/config-change test where possible
- install on YT5760D
- no blank screen
- no fatal logcat exception
- 1600 × 720 content fills the usable area correctly

## Exit gate

A black/neutral shell with rail/content regions renders correctly on the real head unit from entirely new code.

---

# 8. Phase 2 — Design System From the UI Kit

Build components before complete screens.

## 8.1 Theme

Implement:

- dark car theme first
- optional light theme only after dark theme is accurate
- primary/accent colors
- surface hierarchy
- primary/secondary text
- disabled states
- focus/pressed states
- divider/stroke colors
- elevation/shadow treatment

## 8.2 Typography

Create semantic text styles such as:

```text
Display
ScreenTitle
SectionTitle
CardTitle
BodyPrimary
BodySecondary
ButtonLabel
StatusLabel
NavigationInstruction
NavigationDistance
ETA
```

Do not expose raw font-size constants throughout feature code.

## 8.3 Core controls

Implement and preview:

- `CarIconButton`
- `CarPrimaryButton`
- `CarSecondaryButton`
- `CarListRow`
- `CarGridItem`
- `CarHeader`
- `CarSwitch`
- `CarRadioButton`
- `CarCheckbox`
- `CarBadge`
- `CarAlert`
- `CarSearchField`
- `CarTabRow`
- `CarSectionHeader`
- `CarScrim`

## 8.4 Navigation-specific components

From the kit:

- `TurnCard`
- `EtaCard`
- `TripInfo`
- `MapContentHeader`
- `MapContentActions`
- zoom controls
- map recenter control
- route action buttons

## Verify

For each component:

- Compose previews across the eight `Ui.pen` reference breakpoints, plus the 1600 × 720 hardware target
- pressed/disabled state
- long text
- minimum touch target
- dark-theme contrast

## Exit gate

All common visual primitives needed by the first four screens exist independently of feature logic.

---

# 9. Phase 3 — Launcher Shell / Navigation Rail

The UI kit uses the same shell semantics with a breakpoint-driven rail orientation: 748 × 450 and 800 × 480 compact landscape references plus both portrait references use a horizontal bottom rail; 880 × 480 and wider landscape references use a vertical left rail. Build one adaptive shell/rail model and reflow it by layout class rather than maintaining separate navigation systems.

## Rail responsibilities

Keep this semantic order fixed. Render it **left → right** on the horizontal rail and **top → bottom** on the vertical rail:

1. **App launcher** — opens the installed-app grid.
2. **Digital assistant** — launches the configured assistant/voice action.
3. **Context/content area** — weather when reliable weather data is available; otherwise media takes over this area. Notifications never occupy this slot.
4. **App dock** — user-selected shortcuts with user-controlled ordering; persist both membership and order.
5. **Navigation bar / navigation affordance** — route/navigation controls appropriate to the current state/layout.
6. **Notification center badge** — badge/count only in the rail; activating it opens Notification Center. Do not render a notification card/tile in the dashboard context area.
7. **Clock**.

The rail may compact spacing or icon treatment for a smaller viewport, but it must not reorder these semantic regions.

## Status responsibilities

Display what Android permits reliably:

- clock
- Wi-Fi state
- Bluetooth state
- volume representation if available
- GPS/location indicator
- notification badge/count if useful

Avoid pretending to control OEM status functions that ordinary apps cannot actually read or change.

## Navigation model

Use a small sealed destination model:

```text
Dashboard
Apps
Notifications
NavigationFullScreen
Search
```

`Dashboard` is the default/root surface rather than a dedicated fixed rail icon. Assistant, app-dock entries, and the context area are shell actions/content, not top-level destinations. OpenLauncher settings can be reached from the app launcher/app dock instead of consuming another fixed rail slot.

Do not pull in Navigation Compose unless it materially simplifies this single-activity launcher. A simple explicit shell state may be more reliable on API 28.

## Exit gate

Rail navigation works instantly between placeholder screens on emulator and YT5760D with no old application code involved.

---

# 10. Phase 4 — App Grid / Real Launcher Behavior

The app grid is the first complete functional feature.

## UI

Use the UI kit grid patterns:

- large icon
- primary label
- optional secondary/badge
- grid spacing consistent with widescreen examples
- smooth vertical scrolling
- search action
- alphabetical jump if the app list is large enough

## Platform behavior

Implement `AppCatalog` using `PackageManager`.

Requirements:

- query launchable apps
- exclude OpenLauncher itself from normal results where appropriate
- sort by user-visible name
- stable package/component identifier
- launch selected activity
- cache icons efficiently
- refresh when packages are added/removed/updated

## User features

V1:

- all apps
- favorites/pinned apps
- app search
- long-press menu with pin/unpin and app info

Later:

- custom app order
- categories
- hidden apps

## Exit gate

OpenLauncher can be set as HOME and the user can reliably launch installed applications from the new grid.

---

# 11. Phase 5 — Home / Dashboard

Build the dashboard from all eight responsive reference breakpoints demonstrated in `Ui.pen`. Do not use the previous arbitrary widget grid and do not force every viewport into the same split-screen composition.

## Eight `Ui.pen` reference breakpoints

Treat these as reference layouts/regression anchors, not eight hard-coded `if width == ...` device checks. Select and interpolate layout behavior from available width, height, aspect ratio, rail fit, and minimum usable pane/touch-target sizes.

| Reference | `Ui.pen` structure | Rail | Planning role |
| --- | --- | --- | --- |
| 748 × 450 | canonical | horizontal bottom | minimum compact landscape |
| 800 × 480 | canonical | horizontal bottom | standard compact landscape |
| 880 × 480 | canonical | vertical left | first vertical-rail landscape reference |
| 1120 × 480 | canonical | vertical left | large canonical landscape |
| 1280 × 480 | widescreen | vertical left | widescreen reference |
| 790 × 686 | portrait | horizontal bottom | short portrait; no app split screen |
| 800 × 880 | portrait | horizontal bottom | standard portrait; split-screen capable |
| 1920 × 584 | BMW iX | vertical left | largest widescreen / side-by-side reference |

The eight references collapse into three behavioral families for implementation: compact/canonical landscape, portrait, and widescreen. Rail orientation must follow the `Ui.pen` transition demonstrated by the references instead of orientation alone: 748 × 450 and 800 × 480 remain horizontal, while 880 × 480 and wider landscape references use the vertical rail.

### Portrait behavior — 790 × 686 and 800 × 880

#### Short portrait — reference 790 × 686

This is the minimum portrait layout and **does not support split-screen app panes**.

```text
┌──────────────────────────────┐
│                              │
│      Primary surface         │
│   map / dashboard / apps     │
│                              │
├──────────────┬───────────────┤
│ compact      │ context       │
│ trip/status  │ weather/media │
├──────────────────────────────┤
│ horizontal shell rail 1 → 7 │
└──────────────────────────────┘
```

- Use a horizontal bottom rail.
- Only one primary app/surface owns the main area at a time.
- Opening Apps replaces the main map/dashboard surface rather than creating a second app pane.
- Compact dashboard cards may still share the dashboard’s own lower content row; this is not treated as app split screen.

#### Standard portrait — reference 800 × 880

This is the first portrait layout with enough vertical room for **split-screen support**.

```text
┌──────────────────────────────┐
│                              │
│   Primary map/navigation     │
│                              │
├──────────────────────────────┤
│ secondary pane / context     │
│ apps, trip info, or media    │
├──────────────────────────────┤
│ horizontal shell rail 1 → 7 │
└──────────────────────────────┘
```

- Keep the horizontal bottom rail.
- Prefer a vertical primary/secondary split when the second pane is useful.
- Map/navigation remains the primary pane while the lower pane can host Apps or contextual dashboard content.
- A screen may still expand to the full content region when split screen would make either pane unusable.

### Landscape behavior — 748 × 450 through 1920 × 584

For landscape, preserve the exact rail transition shown by `Ui.pen`:

- 748 × 450 and 800 × 480 use the horizontal bottom rail;
- 880 × 480 and 1120 × 480 use the canonical vertical-left rail;
- 1280 × 480 uses the widescreen vertical-left layout;
- 1920 × 584 uses the BMW iX large-widescreen vertical-left layout.

Content should gain space progressively across these references. Do not jump directly from the 800 × 480 composition to the BMW iX composition.

#### Large widescreen — reference BMW iX 1920 × 584

This is the large widescreen family and uses the `.pen` side-by-side composition.

```text
┌──────┬───────────────────────────┬──────────────────┐
│      │                           │                  │
│ rail │ primary map/navigation    │ companion pane   │
│ 1    │                           │ apps/context/    │
│ ↓    │                           │ weather/media    │
│ 7    │                           │                  │
└──────┴───────────────────────────┴──────────────────┘
```

- Use a vertical left rail.
- Split horizontally: map/navigation is the primary surface and a companion pane uses the remaining width.
- Apps may replace the companion pane while navigation/map stays visible, matching the widescreen `.pen` examples.
- The YT5760D 1600 × 720 target should use the widescreen behavior between the 1280 × 480 and 1920 × 584 references, adapted by constraints rather than scaled from either reference pixel-for-pixel.

## Persistent shell/content rules

Across all breakpoints/families:

- rail order is always App launcher → Digital assistant → Context/content → App dock → Navigation bar → Notification badge → Clock;
- the context/content region prefers **weather** when available and reliable, otherwise **media** takes over;
- notifications are never a dashboard/context tile; only the rail badge opens Notification Center;
- the app dock is user-configurable and user-arrangeable;
- active navigation gives the map/navigation surface priority;
- switching layout family changes placement and split behavior, not feature availability or state ownership.

Implement all eight `Ui.pen` references as Compose previews/regression fixtures at 748 × 450, 800 × 480, 880 × 480, 1120 × 480, 1280 × 480, 790 × 686, 800 × 880, and 1920 × 584, in addition to the primary 1600 × 720 hardware preview.

## Dashboard states

Implement separately:

1. parked/no route/no media
2. media playing/no navigation
3. active navigation
4. GPS unavailable
5. navigation app unavailable in Free
6. route ended/arrival

## Exit gate

Home/dashboard matches the corresponding `Ui.pen` structure at all eight reference breakpoints plus the 1600 × 720 hardware viewport, rail ordering is identical in both orientations, rail orientation transitions match the reference matrix, split-screen activates only where the selected family supports it, and all empty states render without placeholder-looking UI.

---

# 12. Phase 6 — Settings

Use the `.pen` list + settings examples directly.

## Settings V1

### Launcher

- default/home launcher guidance
- start on boot behavior if supported
- startup screen
- pinned apps

### Appearance

- dark/light/automatic later
- UI density preset if head units vary substantially
- text size preset

### Media

- notification access status
- preferred media app shortcut

### Navigation

Free:

- select external navigation app
- embedded-navigation capability status
- compatibility mode
- virtual display DPI later

Paid:

- offline map regions
- voice guidance
- map style
- location provider diagnostics

### System / diagnostics

- Android version
- API level
- ABI
- display resolution
- density
- app version
- granted permissions
- copy/export diagnostics

## Exit gate

Settings persist and survive app process death/reboot.

---

# 13. Phase 7 — Media

Use the media template from the UI kit.

## Architecture

Create:

```text
MediaRepository
MediaSessionController
MediaState
```

Use notification listener/media session APIs where available.

## Media screen

- album art
- title
- artist
- elapsed/duration
- play/pause
- previous/next
- repeat/shuffle when supported
- favorite only when the active session exposes a valid action
- source app identity

## Dashboard media card

A compact now-playing card should use the same state as the full media screen.

## Exit gate

Spotify/YouTube Music/other common media apps can be controlled without breaking when metadata is incomplete.

---

# 14. Phase 8 — Notifications

Use the UI kit notification-center layout.

## V1 scope

- notification listener permission flow
- safe list of notification types suitable for display while driving
- sender/app label
- brief text
- clear/dismiss when permitted
- clear all where permitted
- media notifications should not be duplicated awkwardly with the media feature

Do not implement reply/dictation until interaction safety and API behavior are understood.

## Exit gate

Notification center renders real notifications with stable scrolling and no sensitive accidental expansion from unsupported content types.

---

# 15. Phase 9 — Search

Create one shared search visual pattern from the kit.

Use it for:

- app search
- settings search later
- paid POI/destination search

The search field, results list, empty states, and keyboard behavior should be reusable.

On a head unit, avoid forcing a large soft keyboard until the user explicitly focuses the search field.

---

# 16. Phase 10 — Shared Navigation Contracts

Before implementing either Free or Paid maps, define one shared UI model.

## Models

```text
NavigationState
NavigationDestination
RouteSummary
Maneuver
ManeuverType
NavigationProgress
NavigationError
```

Example state fields:

```text
active
currentRoad
nextManeuver
nextInstruction
nextDistanceMeters
secondaryManeuver
remainingDistanceMeters
etaEpochMillis
speedMps
headingDegrees
rerouting
gpsAvailable
```

The dashboard and turn cards consume this contract regardless of whether navigation comes from an external embedded app or the native paid stack.

---

# 17. Phase 11 — Free Build: External App Embedding POC

Do this as a dedicated technical experiment before wiring it into the dashboard.

The reference LecoAuto implementation strongly indicates this architecture:

```text
SurfaceView
    ↓
VirtualDisplay
    ↓
ActivityOptions.setLaunchDisplayId()
    ↓
external maps/navigation app
    ↓
Surface rendered inside OpenLauncher
```

Input/task control may require:

```text
IInputForwarder
InputManager.injectInputEvent
MotionEvent.setDisplayId
moveStackToDisplay
setFocusedStack
```

## Step 11.1 — controlled activity

First embed an OpenLauncher-owned test activity into a `VirtualDisplay`.

Verify:

- render
- lifecycle
- resize
- teardown
- relaunch

## Step 11.2 — external app

Launch a real installed app onto the virtual display.

Test:

- Google Maps
- Organic Maps or another lightweight navigation app

## Step 11.3 — touch

Prove:

- tap
- drag
- pinch if possible
- long press
- back
- focus

## Step 11.4 — privilege matrix

Record exact results on YT5760D:

| Capability | Normal signed | Platform-compatible | System UID if required |
| --- | --- | --- | --- |
| create display | TBD | TBD | TBD |
| launch on display | TBD | TBD | TBD |
| keep task on display | TBD | TBD | TBD |
| focus | TBD | TBD | TBD |
| touch forwarding | TBD | TBD | TBD |
| back | TBD | TBD | TBD |
| resize | TBD | TBD | TBD |

Do not request privileged permissions just because the reference APK declares them. Add only what testing proves is necessary.

## Exit gate

A real navigation app is visible and touch-interactive inside a standalone OpenLauncher test screen on the YT5760D.

---

# 18. Phase 12 — Product Flavors

Only after the embedding POC tells us what Free needs, create final distribution boundaries.

Suggested dimension:

```text
distribution
```

Potential flavors:

```text
freeGithub
playPaid
```

If required by hardware:

```text
freeGithubNormal
freeGithubYT5760D
playPaid
```

Rules:

- hidden task APIs never compile into Play Paid;
- privileged permissions never appear in Play Paid manifest;
- MapLibre/Ferrostar/Valhalla dependencies do not bloat Free if not used;
- signing configs are isolated;
- private keys never enter Git.

---

# 19. Phase 13 — Integrate Free Navigation Into Home

Once the embedding POC passes:

Create:

```text
EmbeddedNavigationHost
EmbeddedTaskView
VirtualDisplayController
TaskManagerCompat
InputForwarder
ExternalNavigationAppRepository
```

## User flow

1. choose navigation app in Settings;
2. Home creates the embedded surface;
3. selected app is launched/reused on the virtual display;
4. user interacts directly with the embedded map;
5. expand to a larger map mode;
6. collapse back to dashboard;
7. close/restart navigation app if needed.

## Reliability requirements

- survive Compose recomposition;
- avoid relaunch loops;
- recover if the external process dies;
- recover if display surface is recreated;
- restore after OpenLauncher returns to foreground;
- no input forwarding outside embedded bounds.

## Exit gate

Free V1 can be used as a daily launcher with interactive external navigation on the target head unit.

---

# 20. Phase 14 — Paid Build: MapLibre Hardware POC

Do not begin with full offline navigation. First prove the renderer.

## POC

- MapLibre map surface
- current GPS position
- heading
- camera follow mode
- pan/zoom
- day/night map style
- route polyline placeholder

## YT5760D GPU gate

Test specifically for:

- renderer startup
- black/blank surfaces
- context loss
- repeated pan/zoom
- 30-minute map session
- media playback at the same time
- sleep/wake

Do not remove fallback code or advance to the offline stack until MapLibre is stable on PowerVR GE8300 hardware.

---

# 21. Phase 15 — Paid Routing Architecture

Create:

```text
RoutingEngine
ValhallaRoutingEngine
RouteRequest
RoutePlan
RouteProgress
```

The UI never calls Valhalla directly.

First implementation can use a network Valhalla instance to prove the complete navigation UI before native/offline routing is integrated.

## Exit gate

Route request → route line → maneuvers → dashboard NavigationState works with one swappable routing engine.

---

# 22. Phase 16 — Ferrostar Navigation Session

Integrate Ferrostar only after MapLibre and shared routing contracts are stable.

Responsibilities:

- active navigation session
- route progress
- maneuver advancement
- rerouting orchestration
- arrival
- voice instruction events
- map/navigation camera integration

Verify the exact versions selected still support API 28 and required Android ABIs before pinning them.

---

# 23. Phase 17 — Native Offline Valhalla

Build/integrate Valhalla for Android.

Start with:

- arm64-v8a only
- pinned Android NDK
- JNI wrapper isolated behind `RoutingEngine`
- route calculations off the main thread
- explicit native crash/error logging

Offline routing data must be downloaded by region. MBTiles alone do not provide routing.

## Exit gate

With all networking disabled, the YT5760D can calculate a route and reroute inside one installed region.

---

# 24. Phase 18 — Offline Region Packages

Use one OpenLauncher region bundle containing separate map, routing, and search datasets.

Example:

```text
west-bengal.olmap/
├── manifest.json
├── map/
│   └── map.mbtiles
├── routing/
│   └── valhalla/
├── search/
│   └── poi.db
└── style/
    └── optional style assets
```

`manifest.json` should include:

- region ID
- name/country
- bounds
- bundle format version
- map version
- routing version
- POI version
- minimum app version
- compressed size
- installed size
- checksums
- build time

Use West Bengal as the first complete test region.

---

# 25. Phase 19 — Offline Download Manager

Create a real `OfflineRegionRepository`.

Requirements:

- catalog refresh
- download
- progress
- pause/resume where practical
- checksum verification
- available-space check
- atomic activation after validation
- update
- delete
- recover after process death
- clean partial downloads
- optional Wi-Fi-only downloads

Use WorkManager or another durable Android mechanism. Do not make region downloads depend on a screen ViewModel staying alive.

---

# 26. Phase 20 — Offline POI Search

Use a read-only SQLite search database per region.

V1 fields:

```text
id
name
normalizedName
category
subcategory
latitude
longitude
address
locality
region
sourceId
```

V1 search:

- text query
- category
- distance-aware ranking
- recents
- favorites
- home/work

UI must reuse the shared search pattern from the `.pen` kit.

---

# 27. Phase 21 — Voice Guidance

Add only after route progress is reliable.

Create a TTS abstraction:

```text
VoiceGuidanceController
```

Requirements:

- speak maneuver instructions
- configurable mute
- reasonable prompt timing
- media ducking when possible
- no crash if no TTS engine is installed
- recover after audio-focus interruptions

---

# 28. Phase 22 — Location Architecture

Use one shared contract:

```text
LocationProvider
```

Free:

- Android framework `LocationManager`
- no Play Services dependency required

Paid:

- Fused Location Provider where useful
- framework location fallback

Test on the real head unit:

- cold start
- first fix
- update frequency
- tunnel/GPS loss
- recovery
- low-speed heading
- ignition reboot

---

# 29. Phase 23 — Persistence

Use DataStore for simple settings/preferences.

Use Room/SQLite only where structured persistent data benefits from it:

- favorites
- recents
- possibly notification metadata/cache

Do not put transient live state into persistence simply to recreate it after process death. Rebuild live state from platform sources where appropriate.

---

# 30. Phase 24 — Performance Budget

The target hardware is modest. Set budgets early.

## UI

- avoid unnecessary recomposition of the full shell;
- cache app icons;
- lazy-render long lists/grids;
- avoid giant bitmap backgrounds;
- avoid continuous animations with no functional value;
- prefer stable state objects and derived state.

## Map/navigation

- route/native calculations off main thread;
- map lifecycle tied correctly to visible state;
- avoid duplicate map surfaces;
- cap memory used by offline/index caches;
- test map + media simultaneously.

## Measure

On the YT5760D capture:

- process RSS/PSS
- Java heap
- native heap
- CPU during idle dashboard
- CPU during navigation
- frame jank where measurable
- cold launch time
- return-to-home latency

---

# 31. Phase 25 — Hardware/ROM Compatibility Layer

Head units vary more than ordinary phones. Keep ROM-specific behavior isolated.

Create:

```text
HeadUnitProfile
PlatformCapabilities
```

Detect/report capabilities, but do not spread checks such as `if YT5760D` throughout feature code.

Potential capability flags:

- supports embedded virtual-display task
- supports input forwarding
- has hardware radio integration
- has OEM reverse-camera overlay
- has custom status broadcast
- supports sleep/wake callbacks

YT5760D becomes the first profile, not the architecture itself.

---

# 32. Testing Strategy

## Build checks

At every milestone:

```text
./gradlew assembleDebug
```

Later, once flavors exist:

```text
assembleFreeGithubDebug
assemblePlayPaidDebug
```

Add targeted unit tests only for meaningful pure logic such as:

- route-state transformations
- settings migrations
- app sorting/filtering
- region manifest validation

Do not build a large test suite that merely mirrors Compose implementation.

## API 28 emulator gate

For each milestone:

- install succeeds
- activity launches
- 30-second no-crash smoke test
- Home/Back behavior
- no permission-loop crash
- no unsupported API call

## YT5760D hardware gate

Every user-visible milestone must eventually be tested on:

```text
Android 9
API 28
arm64-v8a
1600 × 720
```

The emulator never replaces this test.

---

# 33. Visual Verification

Because the `.pen` file is the source of truth, visual regression is part of implementation.

For every major screen:

1. render at 1600 × 720;
2. capture screenshot;
3. compare against the closest `.pen` layout/component reference;
4. check spacing, hierarchy, corner radii, typography, rail proportions, and empty states;
5. keep screenshots in a dedicated implementation-review folder only if they are intentionally tracked.

Primary screens requiring visual approval:

- shell/rail
- home/dashboard idle
- home/dashboard navigation
- apps grid
- settings
- media
- notifications
- search
- full navigation

---

# 34. Release Gates

## Free V1

Must:

- boot reliably on YT5760D Android 9;
- act as HOME launcher;
- match the new UI system;
- launch installed apps;
- show real media state/control;
- persist settings;
- embed one selected external navigation app on supported hardware;
- accept touch in embedded navigation;
- survive app/embedded-task restart;
- recover after sleep/wake/reboot reasonably;
- clearly report when the current ROM lacks required embedded-task capability.

## Paid V1

Must additionally:

- render MapLibre reliably on YT5760D;
- search downloaded POI data;
- calculate routes using local Valhalla data;
- drive navigation state via Ferrostar/session layer;
- reroute offline;
- download/update/delete at least one offline region;
- work with network disabled after region installation;
- provide voice guidance;
- contain no Free-only privileged/hidden task APIs;
- meet current Play target/policy requirements.

---

# 35. Explicit Non-Goals Before V1

Do not delay V1 for:

- multiple simultaneous embedded apps
- animated 3D dashboard effects
- worldwide offline map coverage
- delta-map update system
- cloud accounts
- cross-device sync
- custom routing engine
- Android Auto protocol emulation
- CarPlay protocol emulation
- arbitrary widget builder
- large theming marketplace
- chat/social features
- deep OEM CAN-bus integration unless needed for a core tested feature

---

# 36. Exact Implementation Order

Follow this sequence.

## Milestone 1 — clean app shell

- fresh `MainActivity`
- fresh theme
- shell frame
- immersive/head-unit handling
- API 28 build
- YT5760D smoke test

**Done when:** new code renders a stable empty launcher shell on hardware.

## Milestone 2 — design system

- tokens
- headers
- buttons
- rows
- grids
- controls
- cards
- navigation primitives

**Done when:** component previews reproduce the `.pen` visual language.

## Milestone 3 — rail/navigation shell

- rail
- status area
- screen state
- placeholder destinations

**Done when:** navigation between screens is stable on API 28.

## Milestone 4 — apps launcher

- PackageManager catalog
- app grid
- launch
- search
- pin/favorite

**Done when:** new OpenLauncher is usable as a basic HOME launcher.

## Milestone 5 — home/dashboard

- idle home
- media state
- navigation state placeholders
- responsive 1600 × 720 composition

**Done when:** the primary dashboard is visually approved.

## Milestone 6 — settings/persistence

- new settings model
- DataStore
- settings list UI
- diagnostics

**Done when:** settings persist cleanly.

## Milestone 7 — media

- notification/media session access
- now playing
- controls
- dashboard card

**Done when:** common music apps work reliably.

## Milestone 8 — notifications/search

- notification center
- app search
- shared search primitives

**Done when:** remaining shared UI kit screens are functional.

## Milestone 9 — shared navigation contracts

- `NavigationState`
- maneuver/ETA/trip UI
- full navigation screen shell

**Done when:** UI no longer cares where navigation data comes from.

## Milestone 10 — Free embedded navigation POC

- `SurfaceView`
- `VirtualDisplay`
- `setLaunchDisplayId`
- test activity
- external app
- input forwarding
- privilege matrix

**Done when:** a real maps app is interactive inside OpenLauncher on YT5760D.

## Milestone 11 — distribution flavors

- Free normal/special decision from evidence
- Paid isolation
- source-set/dependency separation

**Done when:** build variants compile independently.

## Milestone 12 — Free V1 integration

- app selection
- embedded nav home card/full view
- lifecycle recovery
- compatibility diagnostics

**Done when:** Free is releasable.

## Milestone 13 — MapLibre POC

- renderer
- location
- camera
- YT5760D stability

**Done when:** hardware rendering is proven.

## Milestone 14 — online native routing

- `RoutingEngine`
- Valhalla service adapter
- route line/maneuvers
- Ferrostar/session integration

**Done when:** Paid navigation works online on hardware.

## Milestone 15 — native/offline Valhalla

- Android native build
- JNI adapter
- offline graph routing

**Done when:** routing/rerouting works with networking off.

## Milestone 16 — region bundle + downloader

- West Bengal map
- routing graph
- POI database
- manifest/checksum
- durable download/install/update/delete

**Done when:** one region works end-to-end offline.

## Milestone 17 — Paid V1 polish

- POI destination UX
- voice guidance
- long-drive testing
- Play-specific location/billing/policy work

**Done when:** Paid release candidate passes hardware and Play gates.

---

# 37. First Coding Sprint

Do only the first three milestones initially.

## Sprint A — fresh foundation

1. Save current HEAD reference.
2. Preserve `Launcher UI Kit.pen`.
3. Remove old Kotlin UI/source implementation from the app module.
4. Create fresh `MainActivity` and `LauncherShell`.
5. Set the project baseline around API 28 compatibility.
6. Compile.
7. Launch on API 28 emulator.
8. Launch on YT5760D.

## Sprint B — design primitives

Implement only the primitives required for shell, grid, and settings:

```text
CarTheme
CarHeader
CarIconButton
CarRailButton
CarListRow
CarGridItem
CarSwitch
CarBadge
```

Render previews at 1600 × 720.

## Sprint C — first real screen

Implement the app grid first.

Reason: it proves the launcher can perform its core job while exercising the new shell, grid components, PackageManager integration, icon loading, scrolling, and real device touch behavior.

Once the app grid passes on YT5760D, move to Home/dashboard.

---

# 38. Working Rule for Every Future Feature

For each feature:

```text
1. Find the matching component/layout in Launcher UI Kit.pen.
2. Define the state contract.
3. Implement the smallest reusable design components required.
4. Build the static screen.
5. Wire real platform/data behavior.
6. Compile on API 28.
7. Test on YT5760D.
8. Compare screenshot against the kit.
9. Fix only evidence-backed issues.
10. Commit the milestone independently.
```

This keeps the fresh implementation coherent and prevents the old launcher architecture from gradually leaking back into the project.
