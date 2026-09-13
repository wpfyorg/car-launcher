package org.wpfy.carlauncher.core.platform

enum class HeadUnitProfileId {
    GenericAndroid,
    YT5760D,
    YT5760BRomLabApi28,
}

data class PlatformIdentity(
    val manufacturer: String,
    val model: String,
    val product: String,
    val device: String,
    val fingerprint: String,
    val sdkInt: Int,
    val primaryAbi: String?,
    val displayWidthPx: Int,
    val displayHeightPx: Int,
    val densityDpi: Int,
    val isEmulator: Boolean,
)

data class HeadUnitProfile(
    val profileId: HeadUnitProfileId,
    val manufacturer: String,
    val model: String,
    val product: String,
    val device: String,
    val fingerprint: String,
    val sdkInt: Int,
    val primaryAbi: String?,
    val displayWidthPx: Int,
    val displayHeightPx: Int,
    val densityDpi: Int,
    val isEmulator: Boolean,
)

internal fun classifyHeadUnit(identity: PlatformIdentity): HeadUnitProfileId {
    val identityFields = listOf(
        identity.manufacturer,
        identity.model,
        identity.product,
        identity.device,
        identity.fingerprint,
    ).map(::normalizeForMatching)
    val romLabMarkerFields = listOf(
        identity.product,
        identity.device,
        identity.fingerprint,
    ).map(::normalizeForMatching)

    return when {
        identityFields.any { Yt5760dMarker in it } -> HeadUnitProfileId.YT5760D
        identity.isEmulator &&
            identity.sdkInt == 28 &&
            romLabMarkerFields.any { Yt5760bMarker in it } -> HeadUnitProfileId.YT5760BRomLabApi28
        else -> HeadUnitProfileId.GenericAndroid
    }
}

internal fun PlatformIdentity.toHeadUnitProfile(): HeadUnitProfile = HeadUnitProfile(
    profileId = classifyHeadUnit(this),
    manufacturer = manufacturer,
    model = model,
    product = product,
    device = device,
    fingerprint = fingerprint,
    sdkInt = sdkInt,
    primaryAbi = primaryAbi,
    displayWidthPx = displayWidthPx,
    displayHeightPx = displayHeightPx,
    densityDpi = densityDpi,
    isEmulator = isEmulator,
)

private fun normalizeForMatching(value: String): String = buildString(value.length) {
    value.forEach { character ->
        when (character) {
            in 'A'..'Z' -> append(character.lowercaseChar())
            in 'a'..'z', in '0'..'9' -> append(character)
        }
    }
}

private const val Yt5760dMarker = "yt5760d"
private const val Yt5760bMarker = "yt5760b"
