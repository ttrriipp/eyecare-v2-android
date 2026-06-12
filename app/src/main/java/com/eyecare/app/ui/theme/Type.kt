package com.eyecare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.eyecare.app.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val Outfit = FontFamily(
    Font(GoogleFont("Outfit"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Outfit"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("Outfit"), provider, weight = FontWeight.SemiBold),
)

private val DmSans = FontFamily(
    Font(GoogleFont("DM Sans"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("DM Sans"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("DM Sans"), provider, weight = FontWeight.SemiBold),
)

val EyecareTypography = Typography(
    displayLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    bodySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
)
