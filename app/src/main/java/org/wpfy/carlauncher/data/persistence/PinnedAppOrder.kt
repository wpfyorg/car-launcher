package org.wpfy.carlauncher.data.persistence

import org.json.JSONArray

internal fun resolvePinnedOrder(
    encodedOrder: String?,
    legacyKeys: Set<String>,
): List<String> {
    if (legacyKeys.isEmpty()) return emptyList()
    if (encodedOrder.isNullOrBlank()) return legacyKeys.sorted()

    val decodedOrder = runCatching { JSONArray(encodedOrder) }.getOrNull()
        ?: return legacyKeys.sorted()
    val orderedMembers = buildList {
        for (index in 0 until decodedOrder.length()) {
            val key = (decodedOrder.opt(index) as? String)?.trim()
            if (!key.isNullOrEmpty() && key in legacyKeys && key !in this) {
                add(key)
            }
        }
    }

    return orderedMembers + (legacyKeys - orderedMembers.toSet()).sorted()
}

internal fun encodePinnedOrder(keys: List<String>): String {
    return JSONArray(keys).toString()
}
