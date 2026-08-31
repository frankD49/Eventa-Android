package com.kosd.eventa.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Eventa brand logo — seven person figures arranged in two vertical layers:
 * three in the back row (smaller, higher, narrower spread) and four in the
 * front row (larger, lower, wider spread). The front row's extended base
 * evokes a crowd gathering at an event.
 *
 * Each figure uses a distinct teal/cyan shade so they are individually
 * identifiable while staying within Eventa's brand palette.
 *
 * @param size  edge length of the square canvas, in dp
 */
@Composable
fun EventaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val iconSize = size
    Canvas(modifier = modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height

        // ── Dimensions ────────────────────────────────────────────────────
        // Back row: 3 figures, smaller.
        // Front row: 4 figures, larger, with a wider horizontal spread.
        val backHeadR    = w * 0.075f
        val frontHeadR   = w * 0.095f
        val backBodyW    = w * 0.18f
        val frontBodyW   = w * 0.21f
        val backBodyH    = h * 0.24f
        val frontBodyH   = h * 0.28f

        // Back row Y positions (higher)
        val backHeadCy   = h * 0.28f
        val backBodyTop  = h * 0.37f

        // Front row Y positions (lower)
        val frontHeadCy  = h * 0.48f
        val frontBodyTop = h * 0.59f

        // Back row: 3 figures, narrower spread
        val backXs = listOf(w * 0.30f, w * 0.50f, w * 0.70f)

        // Front row: 4 figures, wider/extended spread
        val frontXs = listOf(w * 0.12f, w * 0.37f, w * 0.63f, w * 0.88f)

        // ── Color palette (teal/cyan family, each figure distinct) ────────
        // Back row: medium shades (further back = slightly lighter)
        val backColors = listOf(
            Color(0xFF14B8A6),  // teal-500
            Color(0xFF0D9488),  // teal-600
            Color(0xFF0891B2)   // cyan-600
        )
        // Front row: darker shades (closer = more prominent)
        val frontColors = listOf(
            Color(0xFF0F766E),  // teal-700
            Color(0xFF115E59),  // teal-800
            Color(0xFF0E7490),  // cyan-700
            Color(0xFF134E4A)   // teal-900
        )

        // ── Draw back row first (so front row overlaps) ───────────────────
        for (i in backXs.indices) {
            drawPerson(
                headCx = backXs[i], headCy = backHeadCy, headR = backHeadR,
                bodyCx = backXs[i], bodyTop = backBodyTop,
                bodyW = backBodyW, bodyH = backBodyH,
                color = backColors[i]
            )
        }

        // ── Draw front row ────────────────────────────────────────────────
        for (i in frontXs.indices) {
            drawPerson(
                headCx = frontXs[i], headCy = frontHeadCy, headR = frontHeadR,
                bodyCx = frontXs[i], bodyTop = frontBodyTop,
                bodyW = frontBodyW, bodyH = frontBodyH,
                color = frontColors[i]
            )
        }
    }
}

/**
 * Draws a single person figure (head circle + shoulder/body path) on the
 * current DrawScope. The body is a rounded-shoulder shape that sits below
 * the head.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPerson(
    headCx: Float,
    headCy: Float,
    headR: Float,
    bodyCx: Float,
    bodyTop: Float,
    bodyW: Float,
    bodyH: Float,
    color: Color
) {
    // Head
    drawCircle(
        color = color,
        radius = headR,
        center = Offset(headCx, headCy)
    )

    // Body — rounded shoulders + straight sides + flat bottom
    val halfW = bodyW / 2f
    val shoulderR = bodyW * 0.45f
    val left = bodyCx - halfW
    val right = bodyCx + halfW
    val top = bodyTop
    val bottom = bodyTop + bodyH

    val bodyPath = Path().apply {
        // Start at top-left of the shoulder arc
        moveTo(left, top + shoulderR)
        // Left shoulder arc (quarter circle curving up to the top center)
        arcTo(
            rect = Rect(
                left, top, left + 2 * shoulderR, top + 2 * shoulderR
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // Top edge to right shoulder start
        lineTo(right - shoulderR, top)
        // Right shoulder arc
        arcTo(
            rect = Rect(
                right - 2 * shoulderR, top, right, top + 2 * shoulderR
            ),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // Right side down to bottom
        lineTo(right, bottom)
        // Bottom edge
        lineTo(left, bottom)
        close()
    }

    drawPath(path = bodyPath, color = color)
}
