# Circlet design assets

Every graphic in the app has a standalone SVG source here. The Android vector drawable built from
each one is listed next to it; both are generated from the same geometry, so they match.

![Circlet icon](app-icon-512.png)

## App icon — `svg/icon/`

| File | Purpose | Android resource |
| --- | --- | --- |
| `app-icon.svg` | Full composed icon (store listing, README) | `app/src/main/ic_launcher-playstore.png` (512px render) |
| `app-icon-foreground.svg` | Adaptive icon foreground: lens ring with Pixel sweep gradient, handle, sparkle. Inside the 66dp safe zone | `drawable/ic_launcher_foreground.xml` |
| `app-icon-background.svg` | Adaptive icon background `#F8F9FA` | `values/ic_launcher_background.xml` → `launcher_background` |
| `app-icon-monochrome.svg` | Android 13+ themed icon | `drawable/ic_launcher_monochrome.xml` |

The Android foreground uses a true sweep gradient. SVG has no conic gradient, so the `.svg` files
approximate it with 90 interpolated arc segments.

## UI glyphs — `svg/ui/ic-*.svg` (24dp grid)

| File | Used for | Android resource |
| --- | --- | --- |
| `ic-tile-service.svg` | Quick Settings tile | `drawable/ic_tile_service.xml` |
| `ic-notification.svg` | Gesture service notification | `drawable/ic_notification.xml` |
| `ic-shizuku.svg` | Settings › Shizuku status | `drawable/ic_shizuku.xml` |
| `ic-trigger-service.svg` | Settings › System trigger service | `drawable/ic_trigger_service.xml` |
| `ic-assistant.svg` | Settings › Set Google as default assistant | `drawable/ic_assistant.xml` |
| `ic-corner-gesture.svg` | Settings › Corner swipe gesture | `drawable/ic_corner_gesture.xml` |

## Overlay artwork — `svg/ui/` (reference renders of what the overlay code draws)

| File | State | Drawn by |
| --- | --- | --- |
| `corner-handle.svg` | Resting corner indicator lines | `EffectsView.drawHandle` |
| `corner-beam.svg` | Multi-colour crescent at full progress | `EffectsView.drawBeam` |
| `edge-glow.svg` | Rotating rainbow glow around the screen edges during the swipe | `EdgeGlowRenderer` |
| `recessed-card.svg` | App frame scaled to 0.96 in a rounded card over the scrim | `EffectsView.drawRecessedCard` |
| `shimmer.svg` | White wave when the viewport locks | `EffectsView.drawShimmer` |

Palette: Blue `#4285F4`, Red `#EA4335`, Yellow `#FBBC04`, Green `#34A853` (`gesture/GestureVisuals.kt › PixelPalette`).
