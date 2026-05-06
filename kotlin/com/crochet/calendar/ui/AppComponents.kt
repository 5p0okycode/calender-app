package com.crochet.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crochet.calendar.AppColors

// ─────────────────────────────────────────────────────────────────────────────
// StitchedCard
// Ports the HTML .stitch-wrap SVG dashed border to Compose Canvas.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StitchedCard(
    modifier:      Modifier  = Modifier,
    stitchColor:   Color     = AppColors.StitchGreen,
    cornerRadius:  Dp        = 16.dp,
    inset:         Dp        = 5.dp,
    dashLength:    Float     = 10f,
    gapLength:     Float     = 6f,
    strokeWidth:   Dp        = 2.dp,
    surfaceColor:  Color     = Color.White.copy(alpha = 0.72f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                val insetPx  = inset.toPx()
                val rPx      = cornerRadius.toPx()
                val swPx     = strokeWidth.toPx()
                val dashPx   = dashLength * density
                val gapPx    = gapLength  * density

                drawRoundRect(
                    color        = stitchColor,
                    topLeft      = Offset(insetPx, insetPx),
                    size         = Size(
                        size.width  - insetPx * 2,
                        size.height - insetPx * 2
                    ),
                    cornerRadius = CornerRadius(rPx, rPx),
                    style        = Stroke(
                        width      = swPx,
                        cap        = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dashPx, gapPx), 0f
                        )
                    )
                )
            }
    ) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape    = RoundedCornerShape(cornerRadius),
            color    = surfaceColor
        ) {}
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modifier extension — stitch border without a wrapper Box.
// Use this on existing layouts.
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.stitchBorder(
    color:          Color = AppColors.StitchGreen,
    cornerRadiusDp: Float = 16f,
    insetDp:        Float = 5f,
    dashLength:     Float = 10f,
    gapLength:      Float = 6f,
    strokeWidthDp:  Float = 2f
): Modifier = this.drawWithContent {
    drawContent()
    val insetPx = insetDp        * density
    val rPx     = cornerRadiusDp * density
    val swPx    = strokeWidthDp  * density
    val dashPx  = dashLength     * density
    val gapPx   = gapLength      * density

    drawRoundRect(
        color        = color,
        topLeft      = Offset(insetPx, insetPx),
        size         = Size(size.width - insetPx * 2, size.height - insetPx * 2),
        cornerRadius = CornerRadius(rPx, rPx),
        style        = Stroke(
            width      = swPx,
            cap        = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DashedDivider — ports the hand-drawn SVG dividers from the HTML header/nav
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp) // Height to accommodate the hand-drawn dip
            .drawWithContent {
                val path = Path().apply {
                    val w = size.width
                    // SVG path translated: M0,8 L5,16 L10,18 L390,18 L395,16 L400,8
                    // We normalize the 400 coordinates to actual width
                    moveTo(0f, 0f)
                    lineTo(5f * (w/400f), 12f)
                    lineTo(10f * (w/400f), 14f)
                    lineTo(w - 10f * (w/400f), 14f)
                    lineTo(w - 5f * (w/400f), 12f)
                    lineTo(w, 0f)
                }
                
                drawPath(
                    path = path,
                    color = AppColors.Outline.copy(alpha = 0.4f),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f)
                    )
                )
            }
    )
}



// ─────────────────────────────────────────────────────────────────────────────
// WoodenButton shadow modifier
// Ports .wooden-button { box-shadow: inset 0 -4px 0 rgba(0,0,0,0.2), ... }
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.woodenElevation(): Modifier = this  // placeholder — actual shadow
// is applied via Modifier.shadow() at call sites since Compose handles
// elevation differently from CSS box-shadow

// ─────────────────────────────────────────────────────────────────────────────
// GrainTexture overlay — ports .grain-texture { opacity: 0.03 }
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GrainOverlay(modifier: Modifier = Modifier) {
    // A subtle noise overlay drawn via Canvas turbulence simulation.
    // In practice, ship a small grain PNG as a drawable and overlay it:
    //   Image(painter = painterResource(R.drawable.grain), alpha = 0.03f, ...)
    // This composable is a placeholder — add the drawable to res/drawable.
    Box(modifier = modifier.fillMaxSize())
}
