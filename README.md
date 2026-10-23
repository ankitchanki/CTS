# Circlet

<img src="design/app-icon-512.png" width="96" align="right" alt="Circlet icon">

Trigger Circle to Search on Android 9–16, using **Shizuku** instead of an LSPosed module. It also adds a Pixel-style diagonal corner swipe gesture.

Package name: `io.github.ankitchanki.circlet`. Download the latest APK from [Releases](https://github.com/ankitchanki/CTS/releases).

Forked from [parallelcc/MiCTS](https://github.com/parallelcc/micts) (GPL-3.0). All credit for the original trigger work goes to that project.

## What changed from MiCTS

| MiCTS (LSPosed) | Circlet |
| --- | --- |
| Hooks system_server / launcher / Google | No hooks; privileged calls run in a Shizuku user service (shell or root) |
| `VIS`, `CSHelper`, `CSService` trigger services | `VIS` (no Shizuku needed) and `CSService` (needs Shizuku started with root) |
| Xiaomi long press handle / home button | Diagonal corner swipe gesture, works on any device |
| Device spoof for Google | Removed (it needs hooking). If Google has disabled Circle to Search on your device, you still need root + GMS flags, as in MiCTS |

## How to use

1. Install the latest [Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox) app and set it as the default assistant. Once Shizuku is connected, the settings page can do this for you.
2. Install and start [Shizuku](https://shizuku.rikka.app/), then open Circlet settings and tap **Grant**.
3. Trigger it:
   - Launch the app, use the Quick Settings tile, or point any automation app (Quick Ball, ShortX, Routines+…) at it.
   - Or turn on **Corner swipe gesture** and swipe diagonally inward from the bottom-left or bottom-right corner.
   - Or tap **Use Circlet as assistant** (or pick Circlet in *Default digital assistant app*). The system's own assistant gestures — corner swipe, long-press home, power button — then open Circlet's Circle to Search mode instead of Gemini. This works on any OEM, because it doesn't depend on launcher settings.

### Circlet as the default assistant
Circlet declares an `ACTION_ASSIST` activity, the same way ChatGPT, Claude or Hail appear in the assistant list. When the system invokes the assistant, Circlet plays the invocation animation and opens its Circle to Search mode (frozen screen → lasso → Google Lens).

Limitation: while Google is not the default assistant, Google's own Circle to Search UI can't be opened without root. The `VIS` trigger would reach Circlet itself, not Google. So in this mode Circlet always uses its built-in lasso mode, unless `CSService` with root Shizuku is available.

### Settings
- **System trigger service**
  - `VIS`: works without Shizuku when Google is the default assistant.
  - `CSService`: Android 15+. Calls `IContextualSearchManager.startContextualSearch` through Shizuku. Needs Shizuku **started with root**: the shell user (Shizuku via ADB/wireless debugging) lacks `ACCESS_CONTEXTUAL_SEARCH`. Also needs the system's contextual search service to exist (Pixel-like config). Otherwise Circlet uses `VIS`.
- **Corner swipe gesture**: needs "Display over other apps". When Shizuku is connected, this permission is granted automatically.
- **Stop Gemini on corner swipe** (on by default): Android's own "swipe from corner to invoke assistant" gesture takes the same swipe and opens the default assistant (Gemini). With Shizuku, Circlet turns that system gesture off (`settings put secure assist_touch_gesture_enabled 0`) while the corner gesture is on, and restores your original setting when you turn it off. OEM launchers with their own assistant gesture setting need it turned off manually.
- **Gesture action**
  - `Built-in lasso`: freezes the screen (screenshot via `screencap` over Shizuku). You draw a lasso or box, and the selected region is sent to Google Lens.
  - `System Circle to Search`: plays the animation, then invokes the real Circle to Search using the selected trigger service. With `VIS`, Google decides what opens: if Circle to Search isn't enabled for your device, Google may open Gemini instead. Use `Built-in lasso` in that case.

## Corner gesture design

The code keeps the math separate from the drawing:

```
gesture/CornerVectorMath.kt      pure vector math: corner zones, 45° inward direction, angle deviation, linear progress
gesture/CornerGestureTracker.kt  pure state machine: Idle → Tracking → Activated / Cancelled
gesture/GestureVisuals.kt        pure mapping progress/time → scale 1.0→0.96, corner radius, beam, shimmer
gesture/SelectionGeometry.kt     pure lasso → bounding box → bitmap pixels
gesture/HapticSteps.kt           pure rising-detent logic for haptics
gesture/GestureHaptics           vibration primitives / fallbacks
overlay/EdgeGlowRenderer         rotating edge glow
gesture/CornerGestureController  glue: MotionEvents → tracker → visuals → views, capture, hand-off
overlay/CornerTouchView          40dp × 40dp touch zones in each bottom corner
overlay/EffectsView              recessed card, corner handle → Pixel-colour crescent, white shimmer
overlay/SelectionOverlayView     frozen frame + lasso drawing (Circle to Search mode)
overlay/ScreenCapture            screencap through the Shizuku shell service
```

Feel:
- **Haptics:** 12 rising detents while you swipe, each stronger than the last (haptic primitives where supported, predefined ticks otherwise), then a crisp double-tap confirm on activation.
- **Edge glow:** a rotating, breathing rainbow glow hugs the screen edges as progress grows, swells during the shimmer, and stays softly on while Circle to Search mode is open.

Lifecycle:
1. A touch must start inside a 40dp square in a bottom corner. Movement must stay within ±30° of the 45° inward/upward diagonal. Progress is the finger's extra distance from the corner divided by 140dp, which is linear.
2. As progress grows, the corner handle lines turn into a blue/red/yellow/green crescent. The frozen frame scales from 1.0 to 0.96 into a rounded card.
3. When progress reaches 1.0 before release, the viewport locks: haptic feedback, a white shimmer across the frame, then the card settles back.
4. Circle to Search mode then shows the frozen frame as a static overlay with lasso/box selection.

The pure parts are unit tested (`./gradlew :app:testDebugUnitTest`).

### Known limitations
- Overlay windows sit below the status and navigation bars, so the real bars stay on top of the recessed frame.
- With gesture navigation, some launchers claim upward swipes from the bottom edge, which cancels the corner gesture. Swipe more sideways, or use 3-button navigation.
- The recessed-window and freeze effects need Shizuku (for `screencap`). Without it you still get the beam animation and the system trigger.

## Design assets

The app icon, UI glyphs and overlay artwork each have a separate SVG in [`design/svg`](design/README.md).

## Building and releasing

- Every push builds a release APK in GitHub Actions (artifact `Circlet_<commit>`).
- To publish a release, run **Actions › CI Build › Run workflow** and enter a tag such as `v1.0.1`. Pushing a `v*` tag works too. Either way the tag is created and a GitHub Release is published with the APK attached.
- Signing: add the repository secrets `KEY_STORE` (base64 keystore), `KEY_STORE_PASSWORD`, `ALIAS` and `KEY_PASSWORD`. Without them, CI signs with a throwaway debug key: the APK installs, but different builds can't update each other.

## License

GPL-3.0, same as upstream. See [LICENSE](LICENSE).
