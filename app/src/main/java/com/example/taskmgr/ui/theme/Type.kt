package com.example.taskmgr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.taskmgr.R

val UbuntuSansNerdFontFamily = FontFamily(
    Font(R.font.ubuntu_sans_nerd_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.ubuntu_sans_nerd_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.ubuntu_sans_nerd_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.ubuntu_sans_nerd_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.ubuntu_sans_nerd_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.ubuntu_sans_nerd_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.ubuntu_sans_nerd_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.ubuntu_sans_nerd_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

val UbuntuSansMonoNerdFontFamily = FontFamily(
    Font(R.font.ubuntu_sans_mono_nerd_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.ubuntu_sans_mono_nerd_bold, FontWeight.Bold, FontStyle.Normal)
)

private val DefaultTypography = Typography()

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = UbuntuSansNerdFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = UbuntuSansNerdFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = UbuntuSansNerdFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = UbuntuSansNerdFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = UbuntuSansNerdFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = UbuntuSansNerdFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = UbuntuSansNerdFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = UbuntuSansNerdFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = UbuntuSansNerdFontFamily),
    bodyLarge = TextStyle(
        fontFamily = UbuntuSansNerdFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = UbuntuSansNerdFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = UbuntuSansNerdFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = UbuntuSansNerdFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = UbuntuSansNerdFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = UbuntuSansNerdFontFamily)
)
