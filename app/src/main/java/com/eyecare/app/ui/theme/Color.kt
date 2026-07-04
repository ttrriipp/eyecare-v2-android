package com.eyecare.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette (from Padilla Optical Clinic logo) ──────────────────────────
// Logo cyan: vivid sky blue used as the eye fill in the POC logo
val Primary = Color(0xFF29B6F6)
// Light cyan container — backgrounds behind primary-tinted elements
val PrimaryContainer = Color(0xFFE1F5FE)
// Text / icon on top of Primary backgrounds
val OnPrimary = Color(0xFFFFFFFF)

// Logo charcoal: the warm dark used for the eye outline and letterforms in the logo
val CharcoalDark = Color(0xFF3D3535)

// Navy for high-contrast hero cards (e.g. Next Appointment card)
val NavyBlue = Color(0xFF1A2E5A)

// ── Surface system (Direction B — warm surface depth) ─────────────────────────
// Warm off-white background — subtle warmth to lift it off pure white
val Background = Color(0xFFF8F9FA)
// All surfaces (cards, dialogs, bottom sheets, text fields) — pure white so
// they visually float above the warm off-white background
val CardSurface = Color(0xFFFFFFFF)
// Slightly warm surface variant for search bars and secondary areas
val SurfaceVariant = Color(0xFFF1F3F5)
// Subtle card border — charcoal at very low opacity for depth without heaviness
val CardBorder = Color(0x14000000) // ~8% black

// ── Text colors ────────────────────────────────────────────────────────────────
// Primary text — logo charcoal instead of generic dark grey
val OnSurface = CharcoalDark
// Secondary / helper text — desaturated mid tone
val OnSurfaceVariant = Color(0xFF6B7280)
// Outline / divider — very light, warm-leaning
val Outline = Color(0xFFE5E7EB)

// ── Status palette ─────────────────────────────────────────────────────────────
val StatusPending = Color(0xFFF6AD55)
val StatusConfirmed = Color(0xFF38A169)
val StatusCancelled = Color(0xFFE53E3E)
val StatusInfo = Primary
