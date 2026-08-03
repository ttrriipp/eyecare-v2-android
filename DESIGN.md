---
name: Eyecare
description: A calm, warm, and clear Android care companion built around reassuring structure and restrained depth.
colors:
  primary: "#29B6F6"
  primary-container: "#E1F5FE"
  on-primary: "#3D3535"
  accent-text: "#076D9D"
  charcoal: "#3D3535"
  visit-navy: "#1A2E5A"
  warm-canvas: "#F8F9FA"
  surface: "#FFFFFF"
  surface-variant: "#F1F3F5"
  card-border: "rgba(0, 0, 0, 0.08)"
  supporting-text: "#6B7280"
  outline: "#E5E7EB"
  status-pending: "#F6AD55"
  status-confirmed: "#38A169"
  status-cancelled: "#E53E3E"
  error-container: "#FFE0DE"
  tertiary-container: "#D8F3E3"
typography:
  display:
    fontFamily: "Outfit, sans-serif"
    fontSize: "24sp"
    fontWeight: 600
  headline-large:
    fontFamily: "Outfit, sans-serif"
    fontSize: "22sp"
    fontWeight: 600
  headline:
    fontFamily: "Outfit, sans-serif"
    fontSize: "18sp"
    fontWeight: 500
  headline-small:
    fontFamily: "Outfit, sans-serif"
    fontSize: "16sp"
    fontWeight: 600
  title-large:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "18sp"
    fontWeight: 600
  title:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "16sp"
    fontWeight: 600
  title-small:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "14sp"
    fontWeight: 600
  body-large:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
  body:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
  label-large:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "14sp"
    fontWeight: 500
  label:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "12sp"
    fontWeight: 500
  label-small:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "11sp"
    fontWeight: 500
rounded:
  field: "4dp"
  icon-tile: "10dp"
  small: "12dp"
  medium: "16dp"
  dialog: "20dp"
  large: "24dp"
  pill: "999dp"
spacing:
  xxs: "2dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "20dp"
  xxl: "24dp"
  xxxl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.pill}"
    padding: "14dp 24dp"
    height: "52dp"
  button-outlined:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.primary}"
    rounded: "{rounded.pill}"
    padding: "12dp 20dp"
    height: "48dp"
  field-outlined:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.charcoal}"
    rounded: "{rounded.field}"
    padding: "16dp"
  card-standard:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.charcoal}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  card-appointment:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.charcoal}"
    rounded: "{rounded.dialog}"
    padding: "14dp 18dp"
  card-frame:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.charcoal}"
    rounded: "{rounded.medium}"
    padding: "12dp"
  visit-ticket:
    backgroundColor: "{colors.visit-navy}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  navigation-shell:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.supporting-text}"
    rounded: "{rounded.medium}"
    padding: "6dp"
  navigation-selected:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.small}"
    padding: "8dp 6dp"
    width: "76dp"
---

# Design System: Eyecare

## Overview

**Creative North Star: "The Clear Care Companion"**

Eyecare should feel like a composed companion beside the patient: calm enough for health-related information, warm enough to remain human, and clear enough that the next action never feels uncertain. Its strongest screens use friendly typography, quiet warm surfaces, and precise cyan cues to make operational tasks feel reassuring rather than clinical or bureaucratic.

The system is layered but restrained. White cards and soft tonal panels organize information over a warm off-white canvas; fine borders establish most separation, while low elevation is reserved for genuinely floating or selectable elements. Components are softly structured and reassuring, with rounded geometry, readable labels, and explicit state communication.

This record treats Home, `SplitBottomNavBar`, the appointment surfaces, and the Frames grid as the incumbent visual authority: each independently converges on the same border-led cards, 12–20dp corner family, and restrained status color language, which is the strongest evidence a system is actually being followed rather than reinvented per screen. Patterns found only in unfinished or single-use screens remain implementation context, not system precedent, until they are explicitly approved and folded into this document.

**Key Characteristics:**

- Calm, warm, and clear operation-first hierarchy.
- Lens Cyan reserved for actions, selections, and identity cues.
- White cards and tonal panels over a Warm Clinic Canvas.
- Rounded geometry with restrained, mostly border-led depth.
- Deep Vision Navy reserved for high-priority visit information.

## Colors

The palette combines a bright optical cyan with warm neutrals and one deep navy anchor, keeping everyday care tasks light while giving important visit information greater gravity.

### Primary

- **Lens Cyan** (`primary`, `#29B6F6`): the canonical action and selection *fill* color, identical in light and dark. Use it for primary button/FAB/selected-tab fills, active date circles, and other solid cyan surfaces — never as text or icon color on a light surface (see Deep Lens Cyan below).
- **Light Lens Wash** (`primary-container`, `#E1F5FE` light / `#123846` dark): a low-pressure background for icon tiles, booking invitations, review summaries, and other cyan-associated supporting surfaces. The dark variant is a composed deep cyan-tinted container, not an inverted wash.
- **Charcoal-on-Cyan** (`on-primary`, `#3D3535`, both themes): content placed directly on a Lens Cyan *fill*. White measured 2.3:1 against Lens Cyan — below the 3:1 floor even for large text — so `on-primary` uses the existing Charcoal Ink instead, which clears 5.18:1.
- **Deep Lens Cyan** (`accent-text`, `#076D9D` light / `#29B6F6` dark): Lens Cyan itself fails WCAG AA as text or icon color on *any* light surface — white, its own container wash, and the warm canvas all measure 2.0–2.3:1, the identical failure to white-on-cyan, just the two colors swapped. Use this deeper, same-hue cyan for every cyan link, price, icon-tile glyph, status-pill "info" state, and segmented-tab active label sitting on a light surface. In dark mode it resolves back to Lens Cyan unchanged, since Lens Cyan already clears 7:1+ against the dark surfaces.

### Secondary

- **Deep Vision Navy** (`visit-navy`, `#1A2E5A` light / `#2A4374` dark): a deliberate high-contrast anchor for the next-visit ticket and similarly important clinic summaries. The dark variant is lightened so it still visibly lifts off a dark canvas the way the light variant lifts off the warm canvas; content on top stays white in both themes. Not a general replacement for the warm canvas or white cards.

### Tertiary

- **Amber Pending** (`status-pending`, `#F6AD55`, both themes): scheduled or waiting states. Unchanged in dark mode — it already clears WCAG AA against a near-black surface.
- **Confirmation Green** (`status-confirmed`, `#38A169` light / `#7DD9A8` dark): explicitly successful or confirmed states. This hue also backs Material's `tertiary` role (`tertiary-container` `#D8F3E3` light / `#1B4332` dark), so stock Material components (and the My Eyewear "Accepted / Ready / Paid" states, which read `colorScheme.tertiary`) now resolve to this brand green instead of Material's unbranded default.
- **Alert Red** (`status-cancelled`, `#E53E3E` light / `#FF8A80` dark): cancellation, no-show, destructive, or error-adjacent status communication. This hue also backs Material's `error` role (`error-container` `#FFE0DE` light / `#5C1A16` dark) for the same reason — form validation, destructive buttons, and inline error text across the app now render in brand Alert Red rather than Material's default red.

**Known contrast gap:** white content on the light-theme `status-cancelled`/`status-confirmed` fills (and those hues as inline text on white) measures 3.25–4.13:1 — clears the 3:1 large-text/UI-component floor but not strict 4.5:1 body text. Both hexes are pre-existing, documented brand tokens; nudging them is a separate decision from adding dark theme and needs its own sign-off before either hex changes.

### Neutral

- **Warm Clinic Canvas** (`warm-canvas`, `#F8F9FA` light / `#15161A` dark): the application background. Dark is a composed near-black with the faintest cool cast, not an inverted warm tone — heavy warmth read as muddy at this lightness.
- **Pure Care Surface** (`surface`, `#FFFFFF` light / `#1E2024` dark): cards, dialogs, navigation, fields, and sheets.
- **Soft Utility Surface** (`surface-variant`, `#F1F3F5` light / `#26282D` dark): subdued empty states, search areas, and secondary content groups.
- **Charcoal Ink** (`charcoal`, `#3D3535`): primary text and icons in light mode; warmer than generic black.
- **Warm Fog** (`on-surface` dark, `#ECE7E4`): primary text and icons in dark mode — an off-white, not stark `#FFFFFF`, so long reading passages stay comfortable.
- **Supporting Slate** (`supporting-text`, `#6B7280` light / `#A7ADB4` dark): secondary copy, metadata, inactive navigation, and low-emphasis icons.
- **Quiet Outline** (`outline`, `#E5E7EB` light / `#6E7178` dark): visible field and control outlines.
- **Hairline Border** (`card-border`, `rgba(0,0,0,0.08)` light / `rgba(255,255,255,0.12)` dark): the subtle one-dp boundary used to lift cards from the canvas. Direction inverts deliberately — a black-based hairline is invisible on a dark surface, so dark mode uses a white-based one instead.

**The Clear Accent Rule.** Lens Cyan signals action, selection, identity, or information hierarchy; it does not become broad decoration across an entire screen. **Cyan-as-fill vs. cyan-as-text are different tokens** (`primary` vs. `accent-text`) — never reach for `primary` to color text or an icon glyph sitting on a light surface; that pairing fails contrast outright.

**The Navy Anchor Rule.** Deep Vision Navy is reserved for high-priority visit information whose contrast should interrupt the otherwise quiet surface system.

**The Composed Dark Rule.** Dark theme is a first-class scheme with its own surface, container, and hairline-border decisions — never a mechanical inversion of the light values. `EyecareTheme` switches on `isSystemInDarkTheme()`; the app does not yet opt into Android 12+ Dynamic Color, since it would replace the pinned canonical Lens Cyan with a wallpaper-derived hue.

Material's `secondary`/`secondaryContainer` roles remain unassigned implementation defaults — nothing in the app currently reads them. Do not promote an incidental default color into the Eyecare palette without updating the theme and this record together.

## Typography

**Display Font:** Outfit with the Android sans-serif fallback.

**Body Font:** DM Sans with the Android sans-serif fallback.

**Character:** Outfit gives screen titles and care summaries a friendly, open voice. DM Sans keeps operational text compact and highly readable without becoming sterile.

### Hierarchy

- **Display** (Outfit SemiBold, 24sp): compact screen-level statements such as the Home greeting, the Appointments page title, and the Welcome screen's hero line.
- **Headline Large** (Outfit SemiBold, 22sp): identity-level headings, e.g. the read-only Patient Profile screen's full-name hero line.
- **Headline** (Outfit Medium, 18sp): section headings, appointment titles, and important card content; use SemiBold only when the local hierarchy needs additional emphasis.
- **Headline Small** (Outfit SemiBold, 16sp): in-flow step or empty-state intros, e.g. "Select a visit," "Scheduled visit required."
- **Title Large** (DM Sans SemiBold, 18sp): a detail screen's own hero heading — the appointment type, an estimate's or order's title, a bottom sheet's header — paired with a small label/reference above it; callers may push to Bold for extra emphasis.
- **Title** (DM Sans SemiBold, 16sp): component titles and strong row-level labels.
- **Title Small** (DM Sans SemiBold, 14sp): compact sub-section headers inside a card ("Items," "Notes," "Payment summary") and the Frame Card's frame name.
- **Body Large** (DM Sans Regular, 16sp): prominent detail-row values, review-step content, and empty-state messages that need more presence than ordinary body copy.
- **Body** (DM Sans Regular, 14sp): instructions, supporting descriptions, dates, times, and normal informational content.
- **Label Large** (DM Sans Medium, 14sp): form field labels (e.g. "Gender *").
- **Label** (DM Sans Medium, 12sp): status labels, metadata, compact actions, and controlled uppercase eyebrow text (e.g. the visit ticket's "YOUR NEXT VISIT" kicker and the registration form's section labels).
- **Label Small** (DM Sans Medium, 11sp): the smallest captions and badges, including the bottom navigation's per-tab label and compact status chips, both of which further override the base size via `.copy(fontSize = …)` while keeping the DM Sans/Medium voice.

These twelve roles are the complete set of Material typography slots the app actually reaches for; `displayMedium` and `displaySmall` remain unstyled Material defaults because no screen currently uses them — style them the moment a screen does, rather than letting them silently fall back to Roboto.

**The Friendly Precision Rule.** Use Outfit to welcome and orient; use DM Sans to explain and operate. Do not add ad hoc font families, hand-picked sizes, or an unstyled Material role when an established custom role fits.

## Layout

Eyecare is currently authored for compact Android widths. Home uses a generous 24dp horizontal page margin and a 20dp vertical section rhythm; appointment task flows commonly use 16dp horizontal margins with 12–16dp internal spacing so calendars, forms, and review content remain efficient. Auth flows use a 24dp horizontal margin, matching overview density rather than task-dense density, since each auth step is a single focused decision. Surfaces generally fill the available width rather than floating in narrow centered columns.

The four-destination bottom navigation is content-sized and centered. Each tab owns a 76dp column inside a white shell with 6dp inset padding; the shell respects navigation-bar insets and keeps 12dp of vertical breathing room. Screens that sit behind it reserve approximately 96–120dp at the bottom, while keyboard-sensitive appointment and auth forms apply IME and navigation-bar insets, and scroll rather than clip when content plus keyboard exceeds the viewport.

Directional flows use familiar Android structure: top bars establish context, system Back remains authoritative, and booking and auth steps move horizontally with a fade. No expanded-width, tablet, landscape, or navigation-rail composition is yet established; treat those as open design work rather than stretching the phone layout.

**The Quiet Grid Rule.** Start with 24dp margins for patient overview surfaces and 16dp for task-dense flows, then compose with the established 4/8/12/16/20/24/32dp rhythm.

## Elevation & Depth

The depth model is layered but restrained. Tonal contrast and one-dp borders do most of the work: white cards sit on the Warm Clinic Canvas, soft utility surfaces group secondary information, and cyan containers identify action-adjacent content. Shadows appear only when an element truly floats or benefits from a selectable lift.

### Shadow Vocabulary

- **Resting selection** (1–2dp Material elevation): appointment cards, unselected calendar dates, and Frame grid cards that need a small lift from nearby content.
- **Floating navigation** (2dp Material elevation): the centered bottom navigation shell.
- **Transient emphasis** (up to 4dp Material elevation): appointment actions or overlays that temporarily sit above the content plane (e.g. the "Request appointment" FAB's pressed state).

**The Border-Before-Shadow Rule.** Establish hierarchy with surface color and a subtle border first; add elevation only when the element's physical relationship would otherwise be unclear.

## Shapes

Eyecare uses soft geometry without turning every element into the same pill. Outlined text fields retain a compact Material corner; small icon tiles and selected navigation use 10–12dp corners; primary content cards and the Frame grid use 16dp; appointment cards, empty states, and dialogs may open to 20dp. Primary actions, status capsules, and segmented controls use fully rounded ends, while date selectors and badges use circles when the contained information is inherently compact.

The repeated silhouette is a rounded white or tonal surface with either a hairline border or low elevation. Nested shapes step down proportionally: a 16dp container pairs naturally with 10–12dp icon tiles or selected states.

**The Nested Radius Rule.** Inner shapes must be visibly tighter than their parent surface; do not place a 20–24dp inner tile inside a 16dp card.

## Components

### Buttons

- **Shape:** primary terminal actions are full-width and pill-shaped, commonly 48–52dp high; appointment confirmation uses a 52dp action with a 26dp radius.
- **Primary:** Lens Cyan with Clear White content. Keep one obvious primary action per task step and place progress inside the button without changing its dimensions.
- **Outlined:** a white or transparent surface with a Material outline and primary-colored content; use for cancellation, retry, or an alternative path that must remain visible.
- **Text:** reserve for low-emphasis details such as "View details" and "Load more," while maintaining a 48dp minimum touch target.
- **Focus / Disabled:** retain Material focus, pressed, and disabled behavior; state must remain legible without relying on opacity alone.

### Chips

- **Style:** status pills use a semantic color at approximately 12–15% tint with a matching label, in two weights — a borderless tinted pill (appointment cards) and a bordered `SuggestionChip` variant of the same color (appointment detail); selected segmented controls use a ~14% cyan fill, cyan content, and a ~35%-alpha cyan border.
- **State:** status always includes text, never color alone. Filter and segmented controls must make selected and unselected states distinct through fill, border, and content color.

### Cards / Containers

- **Corner Style:** 16dp for general Home cards and the Frame grid, 20dp for appointment cards or quiet empty states.
- **Background:** Pure Care Surface for ordinary information, Light Lens Wash for action-adjacent summaries, Soft Utility Surface for empty or secondary groups, and Deep Vision Navy only for the next-visit anchor.
- **Shadow Strategy:** border-led for Home cards; one-to-two-dp elevation is acceptable for appointment and Frame cards; the next-visit ticket uses contrast rather than shadow.
- **Border:** one-dp Hairline Border or `outlineVariant` where a white card meets the Warm Clinic Canvas.
- **Internal Padding:** usually 16dp, expanding to 18–20dp for patient-facing summaries and empty states, and tightening to 12dp for the denser Frame grid.

### Inputs / Fields

- **Style:** use Material 3 `OutlinedTextField` on a white surface, with a compact 4dp corner (the Material `extraSmall` default). Appointment notes, referral fields, and the auth flows all follow this same shape and color treatment; auth-specific behaviors (read-only date pickers, password visibility toggles, OTP fields) are established, but the field's own visual finish has not yet been deliberately customized away from the Material default.
- **Focus:** Lens Cyan focus stroke, Quiet Outline at rest, and a persistent text label so meaning does not depend on placeholder copy.
- **Error / Disabled:** use semantic error text beneath or within the field, preserve the user's draft, and keep disabled content readable.

### Navigation

- **Style:** the four-tab bottom navigation is a compact white floating shell with a 16dp radius and 2dp elevation. Selected tabs receive a 12dp Lens Cyan tile with Charcoal-on-Cyan icon and label (`on-primary`, not white — white measured 2.3:1 against Lens Cyan); inactive tabs remain transparent with Supporting Slate content.
- **Motion:** selection color uses a restrained spring, while the selected tab scales only to 1.05. Route changes combine a fade with a short horizontal slide; movement communicates direction without becoming spectacle.
- **Android behavior:** preserve navigation-bar insets, system Back, and 48dp touch targets. The current compact-width shell is not an approved tablet pattern.

### Visit Ticket

The Home visit ticket is the signature high-priority component: a 16dp Deep Vision Navy card, a Lens Cyan date tile, white appointment information, and a restrained translucent status capsule. It should feel like a useful clinic ticket, not a promotional banner.

### Appointment Card

Appointment cards use a 20dp white surface with one-dp elevation, a semantic status pill, a strong appointment title, and aligned date/time rows. Keep the hierarchy scannable and status-first; avoid turning the card into a dense record summary.

### Frame Card

The eyewear grid card is a 16dp white surface with a one-dp `outlineVariant` border and 2dp elevation — a quieter sibling of the appointment card, sized for a two-up grid. A square product image sits above 12dp of padding holding an uppercase brand label (Label role), the frame name (Title, two-line clamp), and the price in Deep Lens Cyan (`accent-text`; the fill-strength Lens Cyan fails contrast as text on this white card). AR-ready frames get a small pill badge (Lens Cyan at 15% tint, rounded 20dp) pinned to the image's top-right corner, pairing a Deep Lens Cyan icon with the text "AR" rather than color alone.

### Segmented Tabs & Date Selector

Appointment history uses a two-item Material `SegmentedButtonRow` for Upcoming/History, tinted with the same ~14%-cyan-fill / Deep-Lens-Cyan-content / ~35%-alpha-border recipe as other selected chip states — the active label uses `accent-text`, not `primary`, since it's content on a near-white tint. The weekly date strip uses 38dp circular date targets: the selected day fills solid Lens Cyan with Charcoal-on-Cyan text (`on-primary`), unselected days sit on a one-dp-elevated white circle, and a 4dp dot (`on-primary` when selected and the day has an appointment, otherwise Deep Lens Cyan) sits beneath the day number as a quiet at-a-glance signal — never the only cue, since the day is also distinguishable by its own date.

### Confirmation Dialog

Appointment confirmations use a 20dp white surface with a hairline border, a 44dp tinted circular icon badge, concise title and body copy, and equal-width pill actions. Destructive confirmations use semantic error color for the confirm action and always retain a clearly worded safe alternative.

### Method-Choice Card

When a screen offers two equally valid ways to complete one task (e.g. the account-linking screen's "enter an invitation code" vs. "ask the clinic to review"), consolidate them into one 16dp bordered white card headed by a Title-role label, with the same segmented-tab recipe as Segmented Tabs choosing between the methods and a crossfade swapping the method-specific copy and action beneath it. This keeps the decision legible as one task rather than a stack of equally-weighted buttons; reserve it for genuinely parallel paths, not a primary action plus a secondary escape hatch.

### Identity Header

Profile leads with a 56dp circular Light Lens Wash avatar holding cyan initials (Headline role), the patient's name (Display role, single line with ellipsis), and a small status dot paired with a short label naming the account's clinic-link state (Confirmation Green when linked, Amber Pending under clinic review, Lens Cyan when not yet linked and a next step exists, Supporting Slate for an unknown status). Never show a bare page title ("Profile") when real account identity is available; the generic title is reserved for states where identity genuinely cannot be shown yet, such as a load error.

## Do's and Don'ts

### Do:

- **Do** use Home, `SplitBottomNavBar`, Appointments, and the Frames grid as the visual authority until other surfaces are explicitly approved.
- **Do** use 24dp page margins for overview and auth screens, and 16dp for task-dense appointment flows.
- **Do** establish hierarchy with a white or tonal surface and a one-dp border before adding shadow.
- **Do** reserve Deep Vision Navy for high-priority visit summaries.
- **Do** pair every status color with a clear text label.
- **Do** preserve 48dp touch targets, Android system insets, system Back behavior, and readable increased-font layouts — including making long or keyboard-compressed content scrollable rather than letting it clip.
- **Do** route every headline, title, body, and label through the twelve established typography roles, even in a shared/base component, so no screen silently reverts to unstyled Material defaults.

### Don't:

- **Don't** promote one-off patterns from unfinished screens into the system without approval.
- **Don't** drift into sterile hospital-dashboard styling with cold white expanses, dense data grids, or clinical blue applied everywhere.
- **Don't** imitate decorative wellness apps with gratuitous gradients, blobs, illustrations, or ornamental reassurance.
- **Don't** flood whole screens with Lens Cyan; its precision is part of its character.
- **Don't** introduce heavy shadows, arbitrary radii, or multiple competing primary actions.
- **Don't** make an entire clickable text run (e.g. a consent label plus an inline link) a single tap target for the link action; isolate the link's own text so the surrounding label can still drive its adjacent control.
- **Don't** claim tablet, expanded-width, or landscape behavior is designed until those states are deliberately resolved. Dark theme is now a first-class scheme; treat any screen that still hard-codes a light-only color as an unfixed defect, not an exception.
- **Don't** use `primary`/Lens Cyan as a `Text` color or `Icon` tint on a light surface — it fails WCAG AA outright. Use `accent-text` (`EyecareColors.current.accentText`) for cyan text/icons instead; `primary` is reserved for fills.
