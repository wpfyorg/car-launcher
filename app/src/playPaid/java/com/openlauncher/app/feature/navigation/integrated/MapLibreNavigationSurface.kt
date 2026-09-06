package com.openlauncher.app.feature.navigation.integrated

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.openlauncher.app.design.theme.CarColors
import com.openlauncher.app.location.FrameworkLocationProvider
import com.openlauncher.app.location.LocationSample
import java.io.File
import kotlinx.coroutines.flow.collect
import org.maplibre.android.MapLibre
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val RouteSourceId = "openlauncher-route-source"
private const val RouteLayerId = "openlauncher-route-layer"
private const val DevMapRelativePath = "navigation/dev-map.pmtiles"

@Composable
fun MapLibreNavigationSurface(
    state: MapRenderState,
    onStateChange: (MapRenderState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = (context as ComponentActivity).lifecycle
    val currentState by rememberUpdatedState(state)
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val locationProvider = remember(context.applicationContext) {
        FrameworkLocationProvider(context.applicationContext)
    }
    var permissionGranted by remember {
        mutableStateOf(hasLocationPermission(context))
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted || hasLocationPermission(context)
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(
            context,
            MapLibreMapOptions.createFromAttributes(context)
                .textureMode(false)
                .logoEnabled(false)
                .attributionEnabled(false)
                .compassEnabled(true)
                .tiltGesturesEnabled(false),
        ).apply {
            onCreate(null)
            setMaximumFps(30)
            getMapAsync { map = it }
        }
    }

    DisposableEffect(mapView, lifecycle) {
        var started = false
        var resumed = false

        fun startIfNeeded() {
            if (!started) {
                mapView.onStart()
                started = true
            }
        }

        fun resumeIfNeeded() {
            startIfNeeded()
            if (!resumed) {
                mapView.onResume()
                resumed = true
            }
        }

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startIfNeeded()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeIfNeeded()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startIfNeeded()
                Lifecycle.Event.ON_RESUME -> resumeIfNeeded()
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    mapView.onPause()
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    mapView.onStop()
                    started = false
                }
                Lifecycle.Event.ON_DESTROY -> Unit
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    DisposableEffect(map) {
        val mapLibreMap = map ?: return@DisposableEffect onDispose { }
        val listener = object : OnCameraTrackingChangedListener {
            override fun onCameraTrackingDismissed() {
                if (currentState.cameraMode != MapCameraMode.Free) {
                    currentOnStateChange(currentState.copy(cameraMode = MapCameraMode.Free))
                }
            }

            override fun onCameraTrackingChanged(currentMode: Int) = Unit
        }
        mapLibreMap.locationComponent.addOnCameraTrackingChangedListener(listener)
        onDispose {
            mapLibreMap.locationComponent.removeOnCameraTrackingChangedListener(listener)
        }
    }

    LaunchedEffect(map, state.night, permissionGranted) {
        val mapLibreMap = map ?: return@LaunchedEffect
        mapLibreMap.setStyle(Style.Builder().fromJson(buildLocalStyle(context, state.night))) { style ->
            addRouteLayer(style, routeFor(state))
            if (permissionGranted) {
                activateLocation(context, mapLibreMap, style, state)
            }
        }
    }

    LaunchedEffect(map, permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect
        val mapLibreMap = map ?: return@LaunchedEffect
        mapLibreMap.getStyle { style -> activateLocation(context, mapLibreMap, style, currentState) }
        locationProvider.updates().collect { sample ->
            currentOnStateChange(currentState.copy(lastFix = sample))
            mapLibreMap.locationComponent
                .takeIf { it.isLocationComponentActivated }
                ?.let { component ->
                    component.forceLocationUpdate(sample.toAndroidLocation())
                    if (
                        currentState.cameraMode != MapCameraMode.Free &&
                        mapLibreMap.cameraPosition.zoom < 10.0
                    ) {
                        runCatching { component.zoomWhileTracking(14.0, 500L) }
                    }
                }
        }
    }

    LaunchedEffect(map, state.cameraMode) {
        val component = map?.locationComponent ?: return@LaunchedEffect
        if (!component.isLocationComponentActivated) return@LaunchedEffect
        component.cameraMode = state.cameraMode.toMapLibreCameraMode()
    }

    LaunchedEffect(map, state.lastFix, state.routeGeometry) {
        map?.getStyle { style ->
            style.getSourceAs<GeoJsonSource>(RouteSourceId)?.setGeoJson(routeFor(state))
        }
    }

    val hasLocalMap = remember(context) { localMapFile(context).isFile }
    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        if (!hasLocalMap) {
            Text(
                text = "Local map POC · copy dev-map.pmtiles to app storage",
                color = CarColors.TextSecondary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(CarColors.Surface.copy(alpha = 0.82f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        } else {
            Text(
                text = "© OpenStreetMap contributors",
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }

        IconButton(
            onClick = {
                currentOnStateChange(state.copy(cameraMode = MapCameraMode.FollowHeading))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 64.dp)
                .background(CarColors.Surface.copy(alpha = 0.9f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = "Recenter map",
                tint = Color.White,
            )
        }

        IconButton(
            onClick = { currentOnStateChange(state.copy(night = !state.night)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(CarColors.Surface.copy(alpha = 0.9f), CircleShape),
        ) {
            Icon(
                imageVector = if (state.night) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = if (state.night) "Use day map" else "Use night map",
                tint = Color.White,
            )
        }
    }
}

private fun activateLocation(
    context: Context,
    map: MapLibreMap,
    style: Style,
    state: MapRenderState,
) {
    val component = map.locationComponent
    if (!component.isLocationComponentActivated) {
        component.activateLocationComponent(
            LocationComponentActivationOptions.builder(context, style)
                .useDefaultLocationEngine(false)
                .build(),
        )
    }
    component.isLocationComponentEnabled = true
    component.renderMode = RenderMode.GPS
    component.cameraMode = state.cameraMode.toMapLibreCameraMode()
    component.setMaxAnimationFps(30)
    state.lastFix?.let { component.forceLocationUpdate(it.toAndroidLocation()) }
}

private fun addRouteLayer(style: Style, lineString: LineString) {
    style.addSource(GeoJsonSource(RouteSourceId, lineString))
    style.addLayer(
        LineLayer(RouteLayerId, RouteSourceId).withProperties(
            lineColor("#578CFF"),
            lineWidth(5f),
        ),
    )
}

private fun routeFor(state: MapRenderState): LineString {
    val coordinates = if (state.routeGeometry.isNotEmpty()) {
        state.routeGeometry
    } else {
        val fix = state.lastFix
        val latitude = fix?.latitude ?: 12.967738
        val longitude = fix?.longitude ?: 77.588912
        listOf(
            MapCoordinate(latitude, longitude),
            MapCoordinate(latitude + 0.004, longitude + 0.006),
            MapCoordinate(latitude + 0.008, longitude + 0.012),
            MapCoordinate(latitude + 0.012, longitude + 0.010),
            MapCoordinate(latitude + 0.016, longitude + 0.017),
        )
    }
    return LineString.fromLngLats(
        coordinates.map { Point.fromLngLat(it.longitude, it.latitude) },
    )
}

private fun buildLocalStyle(context: Context, night: Boolean): String {
    val mapFile = localMapFile(context)
    val background = if (night) "#151616" else "#E8E5DF"
    val water = if (night) "#20384A" else "#A8D5E5"
    val landuse = if (night) "#1D2A22" else "#DCE7D5"
    val road = if (night) "#8D9298" else "#6D7074"
    val boundary = if (night) "#5B626B" else "#8A8E93"

    val source = if (mapFile.isFile) {
        val uri = "pmtiles://${Uri.fromFile(mapFile)}"
        "\"openlauncher-region\":{\"type\":\"vector\",\"url\":\"$uri\"}"
    } else {
        ""
    }
    val regionLayers = if (mapFile.isFile) {
        """,
        {"id":"water","type":"fill","source":"openlauncher-region","source-layer":"water","paint":{"fill-color":"$water"}},
        {"id":"landuse","type":"fill","source":"openlauncher-region","source-layer":"landuse","paint":{"fill-color":"$landuse","fill-opacity":0.65}},
        {"id":"boundaries","type":"line","source":"openlauncher-region","source-layer":"boundary","paint":{"line-color":"$boundary","line-width":1.0}},
        {"id":"roads","type":"line","source":"openlauncher-region","source-layer":"transportation","paint":{"line-color":"$road","line-width":1.8}}
        """.trimIndent()
    } else {
        ""
    }

    return """
        {
          "version": 8,
          "name": "OpenLauncher local navigation",
          "sources": {$source},
          "layers": [
            {"id":"background","type":"background","paint":{"background-color":"$background"}}
            $regionLayers
          ]
        }
    """.trimIndent()
}

private fun localMapFile(context: Context): File = File(context.filesDir, DevMapRelativePath)

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun LocationSample.toAndroidLocation(): Location {
    return Location(provider ?: "openlauncher").apply {
        latitude = this@toAndroidLocation.latitude
        longitude = this@toAndroidLocation.longitude
        time = timestampMillis
        accuracyMeters?.let { accuracy = it }
        altitudeMeters?.let { altitude = it }
        speedMps?.let { speed = it }
        bearingDegrees?.let { bearing = it }
    }
}

private fun MapCameraMode.toMapLibreCameraMode(): Int = when (this) {
    MapCameraMode.Free -> CameraMode.NONE
    MapCameraMode.Follow -> CameraMode.TRACKING_GPS_NORTH
    MapCameraMode.FollowHeading -> CameraMode.TRACKING_GPS
}
