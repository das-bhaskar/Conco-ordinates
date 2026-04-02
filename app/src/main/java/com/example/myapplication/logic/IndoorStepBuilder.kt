package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorNode
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.abs

/**
 * Converts a raw A* node list into human-readable turn-by-turn steps.
 *
 * [scaleMetresPerUnit] converts normalised 0-1 coordinates to metres.
 * Different buildings have different physical dimensions — inject the correct
 * scale at construction time rather than hardcoding "1 unit ≈ 100m".
 *
 * A default companion factory [default] is provided for call-sites that
 * don't need building-specific scaling.
 *
 * Algorithm:
 * 1. Compute heading (angle) of each edge in the path.
 * 2. At each node, compute the heading change (delta).
 * 3. If |delta| < STRAIGHT_THRESHOLD → merge with previous segment (same direction).
 * 4. If |delta| >= STRAIGHT_THRESHOLD → start a new step with a turn instruction.
 * 5. Special nodes (ELEVATOR, ESCALATOR, STAIRCASE, ENTRANCE) always force a new step.
 */
class IndoorStepBuilder(
    private val scaleMetresPerUnit: Float = DEFAULT_SCALE
) {

    companion object {
        /** Approximate scale for buildings without specific dimensions. */
        const val DEFAULT_SCALE = 100f

        /** Heading change below this threshold is considered "straight" (degrees). */
        const val STRAIGHT_THRESHOLD = 25.0

        /** Singleton with default scale — avoids instantiation boilerplate in callers
         *  that don't need building-specific dimensions. */
        val default: IndoorStepBuilder = IndoorStepBuilder()

        /** Convenience: call [default.build] without creating an instance. */
        fun build(
            path:             List<IndoorNode>,
            destinationLabel: String = "your destination"
        ): List<NavStep> = default.build(path, destinationLabel)
    }

    data class NavStep(
        val instruction: String,
        val nodes:       List<IndoorNode>,
        val distanceM:   Float,
        val isLast:      Boolean = false
    )

    /**
     * Build turn-by-turn steps from a flat node list (A* output).
     * Uses [scaleMetresPerUnit] injected at construction time.
     */
    fun build(
        path:             List<IndoorNode>,
        destinationLabel: String = "your destination"
    ): List<NavStep> {
        if (path.size < 2) return emptyList()

        val steps    = mutableListOf<NavStep>()
        var segStart = 0
        var segNodes = mutableListOf(path[0])

        for (i in 1 until path.size) {
            val prev    = path[i - 1]
            val curr    = path[i]
            val isLast  = i == path.size - 1
            val forceBreak = curr.type in listOf("ELEVATOR", "ESCALATOR", "STAIRCASE")
            val turnDeg = if (i < path.size - 1) headingChange(path[i - 1], path[i], path[i + 1]) else 0.0
            val isTurn  = abs(turnDeg) >= STRAIGHT_THRESHOLD

            segNodes.add(curr)

            if (forceBreak || isTurn || isLast) {
                flushSegment(steps, segNodes, path[segStart], prev, curr, path.last(), isLast && !forceBreak)
                segStart = i
                segNodes = mutableListOf(curr)
                if (forceBreak) {
                    emitTransferStep(steps, curr, isLast)
                    segStart = i
                    segNodes = mutableListOf(curr)
                }
            }
        }

        markArrival(steps, destinationLabel)
        return steps
    }

    /** Closes the current walk segment and appends a [NavStep]. */
    private fun flushSegment(
        steps:        MutableList<NavStep>,
        segNodes:     List<IndoorNode>,
        segStartNode: IndoorNode,
        prev:         IndoorNode,
        curr:         IndoorNode,
        lastNode:     IndoorNode,
        isLastStep:   Boolean
    ) {
        val dist = segmentDistance(segNodes, scaleMetresPerUnit)
        val instruction = if (steps.isEmpty()) {
            buildFirstInstruction(segNodes.first(), lastNode)
        } else {
            val prevHeading = headingDeg(segStartNode, prev)
            val newHeading  = headingDeg(prev, curr)
            turnInstruction(normaliseDelta(newHeading - prevHeading), curr)
        }
        steps.add(NavStep(instruction, segNodes.toList(), dist, isLastStep))
    }

    /** Emits a "Take the elevator/escalator/stairs" transfer step. */
    private fun emitTransferStep(steps: MutableList<NavStep>, curr: IndoorNode, isLast: Boolean) {
        val xferInstruction = when (curr.type) {
            "ELEVATOR"  -> "Take the elevator to the next floor"
            "ESCALATOR" -> "Take the escalator to the next floor"
            "STAIRCASE" -> "Take the stairs to the next floor"
            else        -> "Transfer"
        }
        steps.add(NavStep(xferInstruction, listOf(curr), 0f, isLast))
    }

    /** Rewrites the last step's instruction to the arrival message. */
    private fun markArrival(steps: MutableList<NavStep>, destinationLabel: String) {
        if (steps.isNotEmpty()) {
            val last = steps.removeLast()
            steps.add(last.copy(instruction = "You have arrived at $destinationLabel", isLast = true))
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Heading in degrees from node a to node b (0° = right, 90° = down in canvas coords). */
    private fun headingDeg(a: IndoorNode, b: IndoorNode): Double =
        Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble()))

    /** Signed heading change at node b, given incoming a→b and outgoing b→c. */
    private fun headingChange(a: IndoorNode, b: IndoorNode, c: IndoorNode): Double {
        val h1 = headingDeg(a, b)
        val h2 = headingDeg(b, c)
        return normaliseDelta(h2 - h1)
    }

    /** Normalise angle delta to [-180, 180]. */
    private fun normaliseDelta(delta: Double): Double =
        ((delta + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

    /** Human-readable turn instruction. */
    private fun turnInstruction(delta: Double, atNode: IndoorNode): String {
        // Special node types override turn language
        if (atNode.type == "ELEVATOR")  return "Take the elevator"
        if (atNode.type == "ESCALATOR") return "Take the escalator"
        if (atNode.type == "STAIRCASE") return "Take the stairs"
        return when {
            delta < -120 -> "Turn around"
            delta < -45  -> "Turn left"
            delta < -STRAIGHT_THRESHOLD -> "Bear left"
            delta >  120 -> "Turn around"
            delta >  45  -> "Turn right"
            delta >  STRAIGHT_THRESHOLD -> "Bear right"
            else         -> "Continue straight"
        }
    }

    /** First step instruction based on overall heading. */
    private fun buildFirstInstruction(start: IndoorNode, end: IndoorNode): String {
        val heading = headingDeg(start, end)
        val cardinal = when {
            heading in -22.5..22.5    -> "east"
            heading in 22.5..67.5     -> "southeast"
            heading in 67.5..112.5    -> "south"
            heading in 112.5..157.5   -> "southwest"
            heading > 157.5 || heading < -157.5 -> "west"
            heading in -157.5..-112.5 -> "northwest"
            heading in -112.5..-67.5  -> "north"
            else                      -> "northeast"
        }
        return "Head $cardinal"
    }

    /** Euclidean distance along the segment in metres. */
    private fun segmentDistance(nodes: List<IndoorNode>, scale: Float): Float {
        var d = 0f
        for (i in 1 until nodes.size) {
            val dx = nodes[i].x - nodes[i-1].x
            val dy = nodes[i].y - nodes[i-1].y
            d += sqrt(dx * dx + dy * dy) * scale
        }
        return d
    }
}
