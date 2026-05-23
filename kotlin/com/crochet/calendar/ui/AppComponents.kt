package com.crochet.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
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

// ─────────────────────────────────────────────────────────────────────────────
// Custom Shape for the Bottom Bar
// ─────────────────────────────────────────────────────────────────────────────

val DipsyBottomBarShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val startY = 0f
    moveTo(0f, startY)
    lineTo(20f * (w / 400f), startY - 24f)
    lineTo(25f * (w / 400f), startY - 26f)
    lineTo(w - 25f * (w / 400f), startY - 26f)
    lineTo(w - 20f * (w / 400f), startY - 24f)
    lineTo(w, startY)
    lineTo(w, h)
    lineTo(0f, h)
    close()
}

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
            .height(26.dp)
            .drawWithContent {
                val path = Path().apply {
                    val w = size.width
                    val startY = 12f
                    moveTo(0f, startY)
                    lineTo(20f * (w/400f), startY + 24f)
                    lineTo(25f * (w/400f), startY + 26f)
                    lineTo(w - 25f * (w/400f), startY + 26f)
                    lineTo(w - 20f * (w/400f), startY + 24f)
                    lineTo(w, startY)
                }
                
                drawPath(
                    path = path,
                    color = AppColors.Outline.copy(alpha = 0.4f),
                    style = Stroke(
                        width = 2.dp.toPx(),
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

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

/**@Preview(showBackground = true, backgroundColor = 0xFFFFF8EF) // Matches AppColors.Background
@Composable
fun GrannySquarePreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Outer Layer", color = AppColors.OnSurface)
        Image(
            painter = painterResource(id = R.drawable.ic_granny_square),
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )

        Text("Middle Layer", color = AppColors.OnSurface)
        Image(
            painter = painterResource(id = R.drawable.ic_granny_square),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            colorFilter = ColorFilter.tint(Color(0xFF8D6E63))
        )

        Text("Inner Layer", color = AppColors.OnSurface)
        Image(
            painter = painterResource(id = R.drawable.ic_granny_square_rounds),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            colorFilter = ColorFilter.tint(Color(0xFF8D6E63))
        )
    }
}
**/