package com.pdfreader.app.presentation.mvi

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/** Pure hit-testing policy for pending and embedded vector ink. */
object InkHitTester {
    fun select(position: Offset, candidates: List<SelectedInk>): SelectedInk? {
        return candidates
            .mapNotNull { candidate ->
                val distance = candidate.paths.minOfOrNull { path -> distanceToPath(position, path) }
                    ?: return@mapNotNull null
                val tolerance = candidate.normalizedStrokeWidth / 2f + MINIMUM_TOUCH_TOLERANCE
                if (distance <= tolerance) candidate to distance else null
            }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    private fun distanceToPath(position: Offset, path: List<Offset>): Float {
        if (path.isEmpty()) return Float.MAX_VALUE
        if (path.size == 1) return hypot(position.x - path[0].x, position.y - path[0].y)
        return path.zipWithNext().minOf { (start, end) ->
            distanceToSegment(position, start, end)
        }
    }

    private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) return hypot(point.x - start.x, point.y - start.y)
        val projection = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared)
            .coerceIn(0f, 1f)
        return hypot(point.x - (start.x + projection * dx), point.y - (start.y + projection * dy))
    }

    private const val MINIMUM_TOUCH_TOLERANCE = 0.012f
}
