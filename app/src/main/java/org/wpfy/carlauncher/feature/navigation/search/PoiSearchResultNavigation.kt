package org.wpfy.carlauncher.feature.navigation.search

import org.wpfy.carlauncher.feature.navigation.NavigationDestination

fun PoiSearchResult.toNavigationDestination(): NavigationDestination {
    val destinationAddress = listOfNotNull(
        address?.takeIf(String::isNotBlank),
        locality?.takeIf(String::isNotBlank),
        region?.takeIf(String::isNotBlank),
    ).distinct().joinToString(", ").ifBlank { null }
    return NavigationDestination(
        label = name,
        address = destinationAddress,
        latitude = latitude,
        longitude = longitude,
    )
}
