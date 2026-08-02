---
name: Eyecare
description: A calm, warm, and clear Android care companion built around reassuring structure and restrained depth.
colors:
  primary: "#29B6F6"
  primary-container: "#E1F5FE"
  on-primary: "#FFFFFF"
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
typography:
  display:
    fontFamily: "Outfit, sans-serif"
    fontSize: "24sp"
    fontWeight: 600
  headline:
    fontFamily: "Outfit, sans-serif"
    fontSize: "18sp"
    fontWeight: 500
  title:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "16sp"
    fontWeight: 600
  body:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
  label:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "12sp"
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

This record intentionally treats Home, `SplitBottomNavBar`, and the appointment surfaces as the incumbent visual authority. Patterns found only in unfinished screens are implementation context, not system precedent, until they are explicitly approved and folded into this document.

**Key Characteristics:**

- Calm, warm, and clear operation-first hierarchy.
- Lens Cyan reserved for actions, selections, and identity cues.
- White cards and tonal panels over a Warm Clinic Canvas.
- Rounded geometry with restrained, mostly border-led depth.
- Deep Vision Navy reserved for high-priority visit information.

## Colors

The palette combines a bright optical cyan with warm neutrals and one deep navy anchor, keeping everyday care tasks light while giving important visit information greater gravity.

### Primary

- **Lens Cyan** (`primary`): the canonical action and selection color. Use it for primary buttons, selected navigation, active dates, icons, links, focus strokes, and concise emphasis.
- **Light Lens Wash** (`primary-container`): a low-pressure background for icon tiles, booking invitations, review summaries, and other cyan-associated supporting surfaces.
- **Clear White** (`on-primary`): content placed directly on Lens Cyan or Deep Vision Navy.

### Secondary

- **Deep Vision Navy** (`visit-navy`): a deliberate high-contrast anchor for the next-visit ticket and similarly important clinic summaries. It is not a general replacement for the warm canvas or white cards.

### Tertiary

- **Amber Pending** (`status-pending`): scheduled or waiting states.
- **Confirmation Green** (`status-confirmed`): explicitly successful or confirmed states.
- **Alert Red** (`status-cancelled`): cancellation, no-show, destructive, or error-adjacent status communication.

### Neutral

- **Warm Clinic Canvas** (`warm-canvas`): the application background; slightly warm so white surfaces remain visible without heavy shadow.
- **Pure Care Surface** (`surface`): cards, dialogs, navigation, fields, and sheets.
- **Soft Utility Surface** (`surface-variant`): subdued empty states, search areas, and secondary content groups.
- **Charcoal Ink** (`charcoal`): primary text and icons; warmer than generic black.
- **Supporting Slate** (`supporting-text`): secondary copy, metadata, inactive navigation, and low-emphasis icons.
- **Quiet Outline** (`outline`): visible field and control outlines.
- **Hairline Border** (`card-border`): the subtle one-dp boundary used to lift white cards from the warm canvas.

**The Clear Accent Rule.** Lens Cyan signals action, selection, identity, or information hierarchy; it does not become broad decoration across an entire screen.

**The Navy Anchor Rule.** Deep Vision Navy is reserved for high-priority visit information whose contrast should interrupt the otherwise quiet surface system.

Unassigned Material color roles remain implementation defaults and are provisional. Do not promote an incidental default secondary, tertiary, or error color into the Eyecare palette without updating the theme and this record together.

## Typography

**Display Font:** Outfit with the Android sans-serif fallback.

**Body Font:** DM Sans with the Android sans-serif fallback.

**Character:** Outfit gives screen titles and care summaries a friendly, open voice. DM Sans keeps operational text compact and highly readable without becoming sterile.

### Hierarchy

- **Display** (Outfit SemiBold, 24sp): compact screen-level statements such as the Home greeting and major page titles.
- **Headline** (Outfit Medium, 18sp): section headings, appointment titles, and important card content; use SemiBold only when the local hierarchy needs additional emphasis.
- **Title** (DM Sans SemiBold, 16sp): component titles and strong row-level labels.
- **Body** (DM Sans Regular, 14sp): instructions, supporting descriptions, dates, times, and normal informational content.
- **Label** (DM Sans Medium, 12sp): status labels, metadata, compact actions, and controlled uppercase eyebrow text.

Only the roles above are established custom tokens. Material roles not overridden in `Type.kt`, including current uses of `titleLarge`, `bodyLarge`, and `labelSmall`, remain provisional defaults rather than new typography rules.

**The Friendly Precision Rule.** Use Outfit to welcome and orient; use DM Sans to explain and operate. Do not add ad hoc font families or hand-picked sizes when an established role fits.

## Layout

Eyecare is currently authored for compact Android widths. Home uses a generous 24dp horizontal page margin and a 20dp vertical section rhythm; appointment task flows commonly use 16dp horizontal margins with 12–16dp internal spacing so calendars, forms, and review content remain efficient. Surfaces generally fill the available width rather than floating in narrow centered columns.

The four-destination bottom navigation is content-sized and centered. Each tab owns a 76dp column inside a white shell with 6dp inset padding; the shell respects navigation-bar insets and keeps 12dp of vertical breathing room. Screens that sit behind it reserve approximately 96–120dp at the bottom, while keyboard-sensitive appointment forms apply IME and navigation-bar insets.

Directional flows use familiar Android structure: top bars establish context, system Back remains authoritative, and booking steps move horizontally with a fade. No expanded-width, tablet, landscape, or navigation-rail composition is yet established; treat those as open design work rather than stretching the phone layout.

**The Quiet Grid Rule.** Start with 24dp margins for patient overview surfaces and 16dp for task-dense flows, then compose with the established 4/8/12/16/20/24/32dp rhythm.

## Elevation & Depth

The depth model is layered but restrained. Tonal contrast and one-dp borders do most of the work: white cards sit on the Warm Clinic Canvas, soft utility surfaces group secondary information, and cyan containers identify action-adjacent content. Shadows appear only when an element truly floats or benefits from a selectable lift.

### Shadow Vocabulary

- **Resting selection** (1dp Material elevation): appointment cards and unselected calendar dates that need a small lift from nearby content.
- **Floating navigation** (2dp Material elevation): the centered bottom navigation shell.
- **Transient emphasis** (up to 4dp Material elevation): appointment actions or overlays that temporarily sit above the content plane.

**The Border-Before-Shadow Rule.** Establish hierarchy with surface color and a subtle border first; add elevation only when the element's physical relationship would otherwise be unclear.

## Shapes

Eyecare uses soft geometry without turning every element into the same pill. Outlined text fields retain a compact Material corner; small icon tiles and selected navigation use 10–12dp corners; primary content cards use 16dp; appointment cards, empty states, and dialogs may open to 20dp. Primary actions and status capsules use fully rounded ends, while date selectors and badges use circles when the contained information is inherently compact.

The repeated silhouette is a rounded white or tonal surface with either a hairline border or low elevation. Nested shapes step down proportionally: a 16dp container pairs naturally with 10–12dp icon tiles or selected states.

**The Nested Radius Rule.** Inner shapes must be visibly tighter than their parent surface; do not place a 20–24dp inner tile inside a 16dp card.

## Components

### Buttons

- **Shape:** primary terminal actions are full-width and pill-shaped, commonly 48–52dp high; appointment confirmation uses a 52dp action with a 26dp radius.
- **Primary:** Lens Cyan with Clear White content. Keep one obvious primary action per task step and place progress inside the button without changing its dimensions.
- **Outlined:** a white or transparent surface with a Material outline and primary-colored content; use for cancellation, retry, or an alternative path that must remain visible.
- **Text:** reserve for low-emphasis details such as “View details” and “Load more,” while maintaining a 48dp minimum touch target.
- **Focus / Disabled:** retain Material focus, pressed, and disabled behavior; state must remain legible without relying on opacity alone.

### Chips

- **Style:** status pills use a semantic color at approximately 12–15% tint with a matching label; selected segmented controls use a light cyan fill, cyan content, and a restrained cyan border.
- **State:** status always includes text, never color alone. Filter and segmented controls must make selected and unselected states distinct through fill, border, and content color.

### Cards / Containers

- **Corner Style:** 16dp for general Home cards and 20dp for appointment cards or quiet empty states.
- **Background:** Pure Care Surface for ordinary information, Light Lens Wash for action-adjacent summaries, Soft Utility Surface for empty or secondary groups, and Deep Vision Navy only for the next-visit anchor.
- **Shadow Strategy:** border-led for Home cards; one-dp elevation is acceptable for appointment cards; the next-visit ticket uses contrast rather than shadow.
- **Border:** one-dp Hairline Border or `outlineVariant` where a white card meets the Warm Clinic Canvas.
- **Internal Padding:** usually 16dp, expanding to 18–20dp for patient-facing summaries and empty states.

### Inputs / Fields

- **Style:** use Material 3 `OutlinedTextField` on a white surface. Appointment notes and referral fields are the current authority; auth-field details remain provisional.
- **Focus:** Lens Cyan focus stroke, Quiet Outline at rest, and a persistent text label so meaning does not depend on placeholder copy.
- **Error / Disabled:** use semantic error text beneath or within the field, preserve the user's draft, and keep disabled content readable.

### Navigation

- **Style:** the four-tab bottom navigation is a compact white floating shell with a 16dp radius and 2dp elevation. Selected tabs receive a 12dp Lens Cyan tile with white icon and label; inactive tabs remain transparent with Supporting Slate content.
- **Motion:** selection color uses a restrained spring, while the selected tab scales only to 1.05. Route changes combine a fade with a short horizontal slide; movement communicates direction without becoming spectacle.
- **Android behavior:** preserve navigation-bar insets, system Back, and 48dp touch targets. The current compact-width shell is not an approved tablet pattern.

### Visit Ticket

The Home visit ticket is the signature high-priority component: a 16dp Deep Vision Navy card, a Lens Cyan date tile, white appointment information, and a restrained translucent status capsule. It should feel like a useful clinic ticket, not a promotional banner.

### Appointment Card

Appointment cards use a 20dp white surface with one-dp elevation, a semantic status pill, a strong appointment title, and aligned date/time rows. Keep the hierarchy scannable and status-first; avoid turning the card into a dense record summary.

### Confirmation Dialog

Appointment confirmations use a 20dp white surface with a hairline border, a 44dp tinted circular icon badge, concise title and body copy, and equal-width pill actions. Destructive confirmations use semantic error color for the confirm action and always retain a clearly worded safe alternative.

## Do's and Don'ts

### Do:

- **Do** use Home, `SplitBottomNavBar`, and Appointments as the visual authority until other surfaces are explicitly approved.
- **Do** use 24dp page margins for overview screens and 16dp for task-dense appointment flows.
- **Do** establish hierarchy with a white or tonal surface and a one-dp border before adding shadow.
- **Do** reserve Deep Vision Navy for high-priority visit summaries.
- **Do** pair every status color with a clear text label.
- **Do** preserve 48dp touch targets, Android system insets, system Back behavior, and readable increased-font layouts.

### Don't:

- **Don't** promote one-off patterns from unfinished screens into the system without approval.
- **Don't** drift into sterile hospital-dashboard styling with cold white expanses, dense data grids, or clinical blue applied everywhere.
- **Don't** imitate decorative wellness apps with gratuitous gradients, blobs, illustrations, or ornamental reassurance.
- **Don't** flood whole screens with Lens Cyan; its precision is part of its character.
- **Don't** introduce heavy shadows, arbitrary radii, or multiple competing primary actions.
- **Don't** claim dark theme, tablet, expanded-width, or landscape behavior is designed until those states are deliberately resolved.
