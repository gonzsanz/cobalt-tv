package io.github.gonzsanz.cobalttv.data.epg

/** One XMLTV programme for a channel. Times are epoch milliseconds. */
data class EpgProgramme(
    val channelId: String,
    val title: String,
    val startMs: Long,
    val stopMs: Long,
)

data class NowNext(
    val now: EpgProgramme? = null,
    val next: EpgProgramme? = null,
)

enum class EpgStatus { Unavailable, Loading, Ready, Error }

/**
 * In-memory XMLTV state. Only a short window around the current time is kept,
 * so a large guide does not have to live in memory for days.
 */
data class EpgState(
    val status: EpgStatus = EpgStatus.Unavailable,
    val programmes: Map<String, List<EpgProgramme>> = emptyMap(),
    val message: String? = null,
) {
    fun nowNext(channelId: String?, atMs: Long): NowNext? {
        if (channelId == null) return null
        val schedule = programmes[channelId] ?: return null
        return schedule.nowNextAt(atMs)
    }
}

fun List<EpgProgramme>.nowNextAt(atMs: Long): NowNext {
    val current = firstOrNull { it.startMs <= atMs && it.stopMs > atMs }
    val upcoming = firstOrNull { it.startMs > atMs }
    return NowNext(now = current, next = upcoming)
}
