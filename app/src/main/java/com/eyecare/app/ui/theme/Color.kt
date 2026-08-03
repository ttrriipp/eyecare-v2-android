package com.eyecare.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette (from Padilla Optical Clinic logo) ──────────────────────────
// Logo cyan: vivid sky blue used as the eye fill in the POC logo. Identical in
// light and dark — this exact hex is the canonical brand blue (see PRODUCT.md).
val Primary = Color(0xFF29B6F6)
// Light cyan container — backgrounds behind primary-tinted elements (light theme)
val PrimaryContainer = Color(0xFFE1F5FE)
// Dark cyan-tinted container — same role, composed for a dark canvas rather than inverted
val PrimaryContainerDark = Color(0xFF123846)
val OnPrimaryContainerDark = Color(0xFFAEE3F5)

// Primary itself fails WCAG AA as text/icon color on any light surface (white, its own
// container wash, warm-canvas all measure ~2.0-2.3:1) — the same cyan that reads fine as
// a fill or as text-on-dark is too light to read as text-on-light. This deeper, same-hue
// cyan is reserved for that role; Primary itself is never darkened, so fills/selection/
// buttons keep the exact canonical brand hex. In dark mode this resolves back to Primary,
// which already clears AA against the dark surfaces (7+:1).
val AccentTextLight = Color(0xFF076D9D)

// Logo charcoal: the warm dark used for the eye outline and letterforms in the logo.
// Also used as content on top of Primary fills (white failed WCAG AA at 2.3:1; charcoal
// clears 5.18:1 while staying an existing brand color rather than introducing a new one).
val CharcoalDark = Color(0xFF3D3535)
val OnPrimary = CharcoalDark

// Navy for high-contrast hero cards (e.g. Next Appointment card)
val NavyBlue = Color(0xFF1A2E5A)
// Dark theme's navy needs to be lighter than a plain-inverted canvas so it still visibly
// lifts off the dark surface the way the light-theme navy lifts off the warm canvas.
val NavyBlueDark = Color(0xFF2A4374)

// ── Surface system — light (Direction B — warm surface depth) ─────────────────
// Warm off-white background — subtle warmth to lift it off pure white
val Background = Color(0xFFF8F9FA)
// All surfaces (cards, dialogs, bottom sheets, text fields) — pure white so
// they visually float above the warm off-white background
val CardSurface = Color(0xFFFFFFFF)
// Slightly warm surface variant for search bars and secondary areas
val SurfaceVariant = Color(0xFFF1F3F5)
// Subtle card border — charcoal at very low opacity for depth without heaviness
val CardBorder = Color(0x14000000) // ~8% black

// ── Surface system — dark ──────────────────────────────────────────────────────
// A composed dark canvas, not an inverted light one: a near-black neutral with the
// faintest cool cast so it reads as a deliberate "night" surface, not a washed-out grey.
val BackgroundDark = Color(0xFF15161A)
val CardSurfaceDark = Color(0xFF1E2024)
val SurfaceVariantDark = Color(0xFF26282D)
// Card borders invert direction on dark: a black-based hairline disappears on a dark
// surface, so dark mode uses a soft white-based hairline instead.
val CardBorderDark = Color(0x1FFFFFFF) // ~12% white

// ── Text colors — light ─────────────────────────────────────────────────────────
// Primary text — logo charcoal instead of generic dark grey
val OnSurface = CharcoalDark
// Secondary / helper text — desaturated mid tone
val OnSurfaceVariant = Color(0xFF6B7280)
// Outline / divider — very light, warm-leaning
val Outline = Color(0xFFE5E7EB)

// ── Text colors — dark ──────────────────────────────────────────────────────────
val OnSurfaceDark = Color(0xFFECE7E4) // warm off-white, not stark white
val OnSurfaceVariantDark = Color(0xFFA7ADB4)
val OutlineDark = Color(0xFF6E7178)

// ── Status palette ─────────────────────────────────────────────────────────────
// Established brand tokens (see DESIGN.md); unchanged across themes. Verified: the
// base hues themselves are unaffected by dark mode, only their containers/pairings are.
val StatusPending = Color(0xFFF6AD55)
val StatusConfirmed = Color(0xFF38A169)
val StatusCancelled = Color(0xFFE53E3E)
val StatusInfo = Primary

// Dark-mode variants for the two status hues that also back Material's error/tertiary
// roles, lightened so they hold WCAG AA as text directly on a near-black surface —
// the base StatusPending/StatusConfirmed/StatusCancelled hex values are left untouched.
val ErrorDark = Color(0xFFFF8A80)
val OnErrorDark = Color(0xFF410002)
val ErrorContainerLight = Color(0xFFFFE0DE)
val OnErrorContainerLight = Color(0xFF7A1712)
val ErrorContainerDark = Color(0xFF5C1A16)
val OnErrorContainerDark = Color(0xFFFFDAD4)

val TertiaryDark = Color(0xFF7DD9A8)
val OnTertiaryDark = Color(0xFF003920)
val TertiaryContainerLight = Color(0xFFD8F3E3)
// Same deep green intentionally: light theme's on-container text and dark theme's
// container fill both land on this value.
val TertiaryContainerDeepGreen = Color(0xFF1B4332)
val OnTertiaryContainerDark = Color(0xFFB7F0D3)
