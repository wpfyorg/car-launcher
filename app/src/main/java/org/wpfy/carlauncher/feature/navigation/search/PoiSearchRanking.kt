package org.wpfy.carlauncher.feature.navigation.search

import org.wpfy.carlauncher.feature.navigation.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure ranking/filtering shared by the Android reader and unit tests. */
fun rankPoiSearchResults(
    candidates: Iterable<PoiSearchResult>,
    request: PoiSearchRequest,
): List<PoiSearchResult> {
    return candidates.mapNotNull { candidate -> rankPoiSearchCandidate(candidate, request) }
        .sortedWith(PoiSearchRankingComparator)
        .map { ranked -> ranked.result }
        .take(request.limit.coerceAtLeast(0))
}

internal data class RankedPoiSearchResult(
    val result: PoiSearchResult,
    val categoryRank: Int,
)

internal val PoiSearchRankingComparator: Comparator<RankedPoiSearchResult> =
    compareBy<RankedPoiSearchResult> { ranked -> ranked.result.matchType.ordinal }
        .thenBy { ranked -> ranked.categoryRank }
        .thenBy { ranked -> ranked.result.distanceMeters ?: Double.POSITIVE_INFINITY }
        .thenBy { ranked -> ranked.result.normalizedName }
        .thenBy { ranked -> ranked.result.regionId.orEmpty() }
        .thenBy { ranked -> ranked.result.id }

internal fun rankPoiSearchCandidate(
    candidate: PoiSearchResult,
    request: PoiSearchRequest,
): RankedPoiSearchResult? {
    val query = normalizePoiName(request.text)
    val queryTokens = query.split(' ').filter(String::isNotEmpty)
    val categoryQuery = request.category?.let(::normalizePoiName).orEmpty()
    val normalizedName = normalizePoiName(candidate.normalizedName.ifBlank { candidate.name })
    val candidateMatchType = matchType(normalizedName, query, queryTokens) ?: return null
    val candidateCategoryRank = categoryRank(candidate, categoryQuery)
    if (categoryQuery.isNotEmpty() && candidateCategoryRank == null) return null

    val distance = request.distanceOrigin?.let { origin ->
        distanceMeters(
            origin,
            GeoPoint(candidate.latitude, candidate.longitude),
        )
    } ?: candidate.distanceMeters?.takeIf { it.isFinite() && it >= 0.0 }

    return RankedPoiSearchResult(
        result = candidate.copy(
            normalizedName = normalizedName,
            distanceMeters = distance,
            matchType = candidateMatchType,
        ),
        categoryRank = candidateCategoryRank ?: 0,
    )
}

private fun matchType(
    normalizedName: String,
    query: String,
    queryTokens: List<String>,
): PoiSearchMatchType? {
    if (query.isEmpty()) return PoiSearchMatchType.Token
    if (normalizedName == query) return PoiSearchMatchType.Exact
    if (normalizedName.startsWith(query)) return PoiSearchMatchType.Prefix

    val nameTokens = normalizedName.split(' ').filter(String::isNotEmpty)
    return if (queryTokens.all { queryToken ->
        nameTokens.any { nameToken -> nameToken.startsWith(queryToken) }
    }) {
        PoiSearchMatchType.Token
    } else {
        null
    }
}

/** A category is a filter, with an exact category preferred over a subcategory match. */
private fun categoryRank(
    result: PoiSearchResult,
    categoryQuery: String,
): Int? {
    if (categoryQuery.isEmpty()) return 0
    val category = result.category?.let(::normalizePoiName).orEmpty()
    val subcategory = result.subcategory?.let(::normalizePoiName).orEmpty()
    return when {
        category == categoryQuery -> 0
        subcategory == categoryQuery -> 1
        category.startsWith(categoryQuery) || subcategory.startsWith(categoryQuery) -> 2
        category.split(' ').any { it.startsWith(categoryQuery) } ||
            subcategory.split(' ').any { it.startsWith(categoryQuery) } -> 3
        else -> null
    }
}

internal fun distanceMeters(first: GeoPoint, second: GeoPoint): Double {
    val earthRadiusMeters = 6_371_000.0
    val latitudeDelta = Math.toRadians(second.latitude - first.latitude)
    val longitudeDelta = Math.toRadians(second.longitude - first.longitude)
    val firstLatitude = Math.toRadians(first.latitude)
    val secondLatitude = Math.toRadians(second.latitude)
    val haversine = sin(latitudeDelta / 2.0) * sin(latitudeDelta / 2.0) +
        cos(firstLatitude) * cos(secondLatitude) *
        sin(longitudeDelta / 2.0) * sin(longitudeDelta / 2.0)
    return earthRadiusMeters * 2.0 * atan2(sqrt(haversine), sqrt(1.0 - haversine))
}
