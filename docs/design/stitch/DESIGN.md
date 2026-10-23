---
name: NextBill Clean Fintech
colors:
  surface: '#f8f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f8f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#424654'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#737785'
  outline-variant: '#c2c6d6'
  surface-tint: '#0058cb'
  primary: '#004cb2'
  on-primary: '#ffffff'
  primary-container: '#1b64da'
  on-primary-container: '#e6ebff'
  inverse-primary: '#b0c6ff'
  secondary: '#545f6d'
  on-secondary: '#ffffff'
  secondary-container: '#d8e3f4'
  on-secondary-container: '#5a6573'
  tertiary: '#a7002f'
  on-tertiary: '#ffffff'
  tertiary-container: '#d30b3f'
  on-tertiary-container: '#ffe6e6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d9e2ff'
  primary-fixed-dim: '#b0c6ff'
  on-primary-fixed: '#001945'
  on-primary-fixed-variant: '#00429b'
  secondary-fixed: '#d8e3f4'
  secondary-fixed-dim: '#bcc7d7'
  on-secondary-fixed: '#121c28'
  on-secondary-fixed-variant: '#3d4855'
  tertiary-fixed: '#ffdada'
  tertiary-fixed-dim: '#ffb3b6'
  on-tertiary-fixed: '#40000c'
  on-tertiary-fixed-variant: '#920028'
  background: '#f8f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
  surface-card: '#FFFFFF'
  surface-canvas: '#F2F4F6'
  surface-canvas-subtle: '#F7F8FA'
  text-primary: '#191F28'
  text-secondary: '#6B7684'
  text-tertiary: '#8B95A1'
  border-subtle: '#E5E8EB'
  urgent-warning: '#E11D48'
  urgent-surface: '#FFF1F2'
  urgent-text: '#BE123C'
  badge-neutral-bg: '#EAECEF'
  badge-neutral-text: '#4E5968'
typography:
  display-currency:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  display-currency-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.01em
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0em
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: 0em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0em
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style
The design system establishes a high-clarity, privacy-first personal finance experience for tracking recurring commitments and subscriptions. Drawing inspiration from modern South Korean fintech interfaces (such as Toss), the visual character communicates quiet confidence, extreme legibility, and effortless structure. 

The emotional tone is calm and unburdened: financial tracking should feel lightweight and reassuring rather than overwhelming. We avoid visual clutter, dramatic gradients, and ornamental photography or illustrations. Instead, clarity is achieved through crisp typographic contrast, pure tonal layering, tactile Material 3 micro-surfaces, and deliberate whitespace.

### Design Movement: Modern Functional Korean Fintech
- **Surface Hierarchy:** Clean pure white base cards resting on subtle off-white and cool-gray backdrops (`#F2F4F6` and `#F7F8FA`).
- **Tactile Material 3 Roots:** Adheres to modern Android principles with 16dp rounded card containers, a standard floating action button (FAB), Material Symbols, and subtle interactive states without synthetic skeuomorphism.
- **Data-First Focus:** Numbers and currency figures are presented in bold, high-contrast black typography, while metadata, helper labels, and operational copy stay anchored in restrained neutral tones.

## Colors
The color architecture relies on a strictly disciplined palette. White cards on soft gray canvases create immediate visual structure without borders or heavy shadows.

- **Primary (`#1B64DA`):** A deep, dependable financial blue reserved for primary calls to action, active navigation affordances, floating action buttons, and critical interactive confirmations.
- **Secondary (`#6B7684`):** A calm, neutral slate gray providing legible secondary hierarchies, timestamps, frequency cycles, and section labels.
- **Tertiary / Urgent (`#E11D48`):** An alert crimson reserved strictly for high-urgency notifications—specifically payments due today ("오늘") or tomorrow ("내일"), error validations, and destructive actions. It must never be used ornamentally.
- **Neutral Canvas (`#F2F4F6` / `#F7F8FA`):** The backdrop color scheme across screens, establishing depth against pure white `#FFFFFF` component cards.

### Color Distribution Rules
- Keep primary brand accents concentrated: no more than one dominant action element per screen.
- For D-day chips, routine timelines ("D-5", "매월 15일") use neutral subtle pills (`#EAECEF` surface with `#4E5968` text). Impending payments ("오늘", "내일") shift directly to the urgent token pairing (`#FFF1F2` surface with `#BE123C` text).
- Avoid multi-stop color gradients or complex fill washes.

## Typography
Typography is tuned to deliver clean, neutral Korean and alphanumeric balance, pairing seamlessly with system sans fonts (such as Pretendard in local production and Plus Jakarta Sans in global system declarations).

- **Currency Hierarchy:** Large currency numbers (`display-currency`) use bold weight, tighter letter spacing (`-0.02em`), and dense primary text color (`#191F28`) to present figures with immediate readability.
- **Korean Context Alignment:** Body text maintains standard line-heights (1.4–1.5x) to accommodate Hangul script density gracefully.
- **Labels & Microcopy:** Secondary labels, cycle metadata, and D-day indicators leverage `label-md` and `label-sm` with slightly boosted medium/semi-bold weights (`500` / `600`) to guarantee contrast against subtle background fills.

## Layout & Spacing
The layout follows a continuous Android single-column fluid card layout bound by standard Material 3 screen gutters.

- **Canvas Margins:** Fixed outer screen margin of `1.25rem` (20dp) on mobile devices ensures comfortable touch offsets and avoids bezel crowding.
- **Card Padding:** Content containers leverage `space-xl` (24dp) for key hero cards (e.g. monthly summary) and `space-lg` (16dp) for structured list rows and form sections.
- **Component Stack Rhythm:** Vertical separation between sections follows `space-xl` (24dp), while related grouped items inside cards adhere to `space-md` (12dp) or `space-sm` (8dp).
- **Responsive Adaptations:**
  - **Mobile (<600dp):** Fluid full-width cards with edge-to-edge touch targets. Top app bar fixed at 64dp height.
  - **Tablet/Large Surface (≥600dp):** Centered fixed-width viewport (maximum 560dp width) with symmetrical horizontal spacing to maintain fingertip reach and scan efficiency.

## Elevation & Depth
This system eliminates heavy, artificial drop shadows in favor of tonal surfaces and subtle boundary definitions.

- **Level 0 (App Canvas):** Base backdrop filled with `#F2F4F6` or `#F7F8FA`.
- **Level 1 (Card Surfaces):** Pure white `#FFFFFF` cards resting on the Level 0 canvas. Depth is produced by the tonal juxtaposition rather than a drop shadow. Where structural separation is needed against white backdrops, use a 1px border with `border-subtle` (`#E5E8EB`).
- **Level 2 (Modal Sheets & Floating Controls):** Bottom sheets and the floating action button (FAB) utilize soft ambient occlusion: `0 8px 24px rgba(25, 31, 40, 0.08)`.
- **Level 3 (Overlays & Dialogs):** Centered alerts and pickers utilize `0 16px 36px rgba(25, 31, 40, 0.14)`.

## Shapes
In line with modern Korean mobile utilities and Material 3 specifications, the shape language uses soft, rounded geometries that feel ergonomic and friendly.

- **Cards & Primary Panels:** Set strictly at 16dp (`rounded-lg` under `roundedness: 2`), creating a smooth, friendly silhouette for financial blocks.
- **Chips & Status Badges:** Fully pill-shaped (`rounded-full` / 9999px) to communicate self-contained, tappable or categorical metadata.
- **Circular Badges & Avatars:** Subscriptions are represented by 40dp (list) or 56dp (detail) 1:1 circular badge icons (`rounded-full`) showing the first letter of the service name.
- **Input Fields & Segmented Buttons:** Radii of 12dp to 16dp to match the overall card geometry.

## Components

### Buttons
- **Primary FAB (Floating Action Button):** Extended or circular FAB anchored to the bottom right (16dp offset). Background is `#1B64DA`, icon and text are white, radius is 16dp, with Material 3 ambient elevation.
- **Contained Primary Button:** Full-width or inline, height 52dp, background `#1B64DA`, text `#FFFFFF`, font weight 600, radius 12dp.
- **Outlined Secondary Button:** Border 1px `#E5E8EB`, background `#FFFFFF`, text `#4E5968`, height 48dp, radius 12dp.
- **Destructive Text Button:** Text `#E11D48`, transparent background, medium weight, used for subscription deletion.

### Subscription List Item
- **Structure:** 72dp minimum row height, pure white surface or separated row inside a unified card.
- **Left:** 40dp circular avatar badge tinted with subscription theme color, displaying a single bold letter.
- **Center:** Service title (`title-md` `#191F28`) stacked above the billing cycle text (`body-sm` `#6B7684`, e.g., "매월 15일").
- **Right:** Bold currency text (`title-md` `#191F28`, e.g., "₩17,000") stacked above or inline with a D-day chip.

### D-Day Chips
- **Standard (D-3, D-5, etc.):** Background `#EAECEF`, text `#4E5968`, padding 4dp 8dp, font `label-sm`.
- **Urgent ("오늘", "내일"):** Background `#FFF1F2`, text `#BE123C`, padding 4dp 8dp, font `label-sm` with bold weight.

### Form Inputs & Segmented Controls
- **Amount Field:** Minimalist large input presenting numbers in `display-currency`, accompanied by an inline currency selector pill (`KRW ▾`).
- **Segmented Selectors:** 4-option frequency tabs (일 / 주 / 개월 / 년) set in a 44dp gray track (`#F2F4F6`), active segment white `#FFFFFF` with soft shadow and bold blue or black text.
- **Input Error State:** 1px border in `#E11D48`, accompanied by 12dp bottom helper copy in `#E11D48`.

### Summary Card (Home Hero)
- Enclosed in a 16dp rounded card, white `#FFFFFF`, padding 24dp.
- Top label: "이번 달 결제 예정" (`body-sm` `#6B7684`).
- Primary metric: "₩86,400" (`display-currency` `#191F28`).
- Secondary breakdown: "월 고정비 ₩72,300" with optional foreign currency indicator ("+ $19.98") in `body-sm` `#8B95A1`.

### Modal Bottom Sheet
- Material 3 sliding sheet with 24dp top corner radius, background `#FFFFFF`, anchored with a 4dp x 32dp gray drag handle at top center. Used for permission guides and contextual action menus.