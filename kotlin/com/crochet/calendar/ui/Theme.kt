package com.crochet.calendar.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.crochet.calendar.R

// ── Google Fonts provider ─────────────────────────────────────────────────────
// Add to build.gradle: implementation("androidx.compose.ui:ui-text-google-fonts")
// Add font_certs.xml to res/values (standard GMS cert file)

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

val PlusJakartaSans = FontFamily(
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Normal),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Medium),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.SemiBold),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Bold),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.ExtraBold),
)

val BeVietnamPro = FontFamily(
    Font(GoogleFont("Be Vietnam Pro"), provider, FontWeight.Light),
    Font(GoogleFont("Be Vietnam Pro"), provider, FontWeight.Normal),
    Font(GoogleFont("Be Vietnam Pro"), provider, FontWeight.Medium),
    Font(GoogleFont("Be Vietnam Pro"), provider, FontWeight.SemiBold),
)

// ── Colour tokens (exact hex values from the HTML Tailwind config) ─────────────

object AppColors {
    val Background          = Color(0xFFFFF8EF)
    val OnBackground        = Color(0xFF3A3216)
    val Surface             = Color(0xFFFFF8EF)
    val SurfaceContainerLow = Color(0xFFFEF3D6)
    val SurfaceContainer    = Color(0xFFF9EDCC)
    val SurfaceContainerHigh= Color(0xFFF4E8C3)
    val OnSurface           = Color(0xFF3A3216)
    val OnSurfaceVariant    = Color(0xFF685F3E)
    val SurfaceVariant      = Color(0xFFF0E2B9)

    val Primary             = Color(0xFF405334)
    val OnPrimary           = Color(0xFFECFFDD)
    val PrimaryContainer    = Color(0xFFD4E9C4)
    val OnPrimaryContainer  = Color(0xFF45573B)

    val Secondary           = Color(0xFF835252)
    val OnSecondary         = Color(0xFFFFF7F6)
    val SecondaryContainer  = Color(0xFFFFDAD9)

    val Tertiary            = Color(0xFF7E572E)
    val OnTertiary          = Color(0xFFFFF7F4)
    val TertiaryContainer   = Color(0xFFE1AF7E)

    val Outline             = Color(0xFF857A58)
    val OutlineVariant      = Color(0xFFBEB18B)

    // Stitch colours from HTML data-stitch-color attributes
    val StitchGreen         = Color(0xFF33442A)
    val StitchBrown         = Color(0xFF502F09)
    val StitchTan           = Color(0xFFBEB18B)
    val StitchCream         = Color(0xFFFCE7D2)

    val deleteColor = Color(0xFF97150D)
    val copyColor = Color(0xFF5B92AA)
    val MonthColors = listOf(
        Color(0xFFAEC6CF) to Color(0xFFE0F2F1), // January:   Ice Blue & Frost
        Color(0xFF835252) to Color(0xFFF48FB1), // February:  Dusty Rose & Pink
        Color(0xFFA5D6A7) to Color(0xFFFFF59D), // March:     Sage & Pale Yellow
        Color(0xFFCE93D8) to Color(0xFFF8BBD0), // April:     Lavender & Blossom
        Color(0xFF81C784) to Color(0xFFFFEE58), // May:       Meadow & Buttercup
        Color(0xFF4FC3F7) to Color(0xFFFFD54F), // June:      Sky & Sun
        Color(0xFF0288D1) to Color(0xFFFCE7D2), // July:      Ocean & Sand
        Color(0xFFFFB74D) to Color(0xFFE57373), // August:    Orange & Soft Red
        Color(0xFF689F38) to Color(0xFFD32F2F), // September: Olive & Apple
        Color(0xFFEF6C00) to Color(0xFF3E2723), // October:   Pumpkin & Chocolate
        Color(0xFFBF360C) to Color(0xFFFFAB00), // November:  Rust & Amber
        Color(0xFF2E7D32) to Color(0xFFC62828)  // December:  Holly & Berry
    )
}
