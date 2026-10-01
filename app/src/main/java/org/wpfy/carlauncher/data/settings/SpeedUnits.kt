package org.wpfy.carlauncher.data.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import java.util.TimeZone
import org.wpfy.carlauncher.design.adapters.speedUnitLabelFor
import org.wpfy.carlauncher.design.adapters.usesMilesPerHour

/** The speed unit the driver chose; Automatic follows the device region. */
enum class SpeedUnitSetting(val label: String) {
    Auto("Automatic"),
    KilometersPerHour("km/h"),
    MilesPerHour("mph"),
}

/** The units of the HUD: km/h with metres and kilometres, or mph with feet and miles. */
enum class UnitSystem {
    Metric,
    Imperial,
}

/** The units for [setting]; Automatic uses mph where [locale]'s region (see [unitRegionLocale] for the locale to pass) posts road speeds so (US, UK, Liberia, Myanmar, ...). */
fun resolveUnitSystem(setting: SpeedUnitSetting, locale: Locale): UnitSystem = when (setting) {
    SpeedUnitSetting.KilometersPerHour -> UnitSystem.Metric
    SpeedUnitSetting.MilesPerHour -> UnitSystem.Imperial
    SpeedUnitSetting.Auto -> if (usesMilesPerHour(locale)) UnitSystem.Imperial else UnitSystem.Metric
}

/** The row's value: the chosen unit, or "Automatic · mph" with the unit the region gives. */
fun speedUnitSummary(setting: SpeedUnitSetting, locale: Locale): String =
    if (setting == SpeedUnitSetting.Auto) "${setting.label} · ${speedUnitLabelFor(locale)}" else setting.label

/** The units Home's HUD shows; Imperial until a screen provides the driver's choice. */
val LocalUnitSystem = staticCompositionLocalOf { UnitSystem.Imperial }

/**
 * The locale whose country decides Automatic units: the network country, else the SIM country, else the
 * country of a geographic device time zone (an mph country for the listed zones, otherwise metric), else
 * [locale]'s own country. A head unit often keeps an en-US locale in a metric country, so the locale alone is the last resort.
 */
fun unitRegionLocale(networkIso: String?, simIso: String?, timeZoneId: String?, locale: Locale): Locale {
    val country = networkIso?.trim()?.takeIf { it.isNotEmpty() }
        ?: simIso?.trim()?.takeIf { it.isNotEmpty() }
        ?: timeZoneId?.let(::countryOfTimeZone)
        ?: return locale
    return Locale(locale.language, country.uppercase(Locale.ROOT))
}

private val UsAmericaZones = setOf(
    "New_York", "Chicago", "Denver", "Los_Angeles", "Phoenix", "Anchorage", "Juneau", "Sitka", "Metlakatla",
    "Yakutat", "Nome", "Adak", "Boise", "Detroit", "Menominee", "Indianapolis", "Louisville", "Fort_Wayne",
    "Knox_IN", "Shiprock",
)
private val UsAmericaPrefixes = listOf("Indiana/", "Kentucky/", "North_Dakota/")

private val FixedZoneCountries = mapOf(
    "America/Puerto_Rico" to "PR", "America/St_Thomas" to "VI", "America/Virgin" to "VI",
    "Pacific/Guam" to "GU", "Pacific/Saipan" to "MP", "Pacific/Pago_Pago" to "AS", "Pacific/Samoa" to "AS",
    "Pacific/Honolulu" to "US", "Pacific/Johnston" to "US", "Pacific/Midway" to "US",
    "Europe/London" to "GB", "Europe/Belfast" to "GB", "GB" to "GB", "Europe/Guernsey" to "GG",
    "Europe/Jersey" to "JE", "Europe/Isle_of_Man" to "IM", "Africa/Monrovia" to "LR",
    "Asia/Yangon" to "MM", "Asia/Rangoon" to "MM", "Atlantic/Stanley" to "FK",
    "America/Nassau" to "BS", "America/Belize" to "BZ", "America/Cayman" to "KY", "America/Tortola" to "VG",
    "America/Grand_Turk" to "TC", "America/Antigua" to "AG", "America/Dominica" to "DM",
    "America/Grenada" to "GD", "America/St_Kitts" to "KN", "America/St_Lucia" to "LC",
    "America/St_Vincent" to "VC",
)

private val GeographicZoneAreas = listOf(
    "Africa/", "America/", "Antarctica/", "Arctic/", "Asia/", "Atlantic/", "Australia/", "Europe/", "Indian/", "Pacific/",
)

/** An mph country for a listed zone, "" (no mph country, so metric) for any other geographic zone, null for UTC, Etc/ and unknown ids. */
private fun countryOfTimeZone(id: String): String? {
    FixedZoneCountries[id]?.let { return it }
    if (id.startsWith("US/")) return "US"
    if (id.startsWith("America/")) {
        val rest = id.removePrefix("America/")
        if (rest in UsAmericaZones || UsAmericaPrefixes.any(rest::startsWith)) return "US"
    }
    return if (GeographicZoneAreas.any(id::startsWith)) "" else null
}

/** The locale that decides Automatic units on this device: network or SIM country, else time zone, else the app locale. */
@Composable
fun rememberUnitRegionLocale(): Locale {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var zoneChanges by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                zoneChanges++
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIMEZONE_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return remember(context, locale, zoneChanges) {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        unitRegionLocale(
            networkIso = runCatching { telephony?.networkCountryIso }.getOrNull(),
            simIso = runCatching { telephony?.simCountryIso }.getOrNull(),
            timeZoneId = TimeZone.getDefault().id,
            locale = locale,
        )
    }
}
