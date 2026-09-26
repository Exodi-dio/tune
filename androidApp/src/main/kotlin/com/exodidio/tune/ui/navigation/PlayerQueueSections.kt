package com.exodidio.tune.ui.navigation

data class QueueSections(
    val nowPlayingId: String?,
    val userIds: List<String>,
    val contextIds: List<String>,
    val autoplayIds: List<String>
)

fun splitQueue(
    activeTrackIds: List<String>,
    currentIndex: Int,
    userIds: Set<String>,
    autoplayIds: Set<String>
): QueueSections {
    if (activeTrackIds.isEmpty() || currentIndex !in activeTrackIds.indices) {
        return QueueSections(null, emptyList(), emptyList(), emptyList())
    }
    val nowPlayingId = activeTrackIds[currentIndex]
    val upcoming = activeTrackIds.subList(currentIndex + 1, activeTrackIds.size)
    val user = upcoming.filter { it in userIds && it !in autoplayIds }
    val autoplay = upcoming.filter { it in autoplayIds }
    val context = upcoming.filter { it !in userIds && it !in autoplayIds }
    return QueueSections(nowPlayingId, user, context, autoplay)
}

fun shouldShowAutoplayHeader(autoplayIds: List<String>, autoplayEnabled: Boolean): Boolean =
    autoplayIds.isNotEmpty() || autoplayEnabled
