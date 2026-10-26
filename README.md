# Circlet

<img src="design/app-icon-512.png" width="96" align="right" alt="Circlet icon">

**A Pixel-style corner swipe for Google's Circle to Search on Android 9–16, powered by Shizuku: no root, no LSPosed.**

Swipe diagonally from a bottom corner and Circlet plays a Pixel-inspired animation. A multi-colour beam grows from the corner, a rainbow glow wraps the screen, and the haptics rise with your finger. Then Google's **native Circle to Search** opens, exactly as if you had tapped the Circlet icon.

[**⬇ Download the latest signed APK**](https://github.com/ankitchanki/CTS/releases/latest) (`Circlet-Full_…apk` or `Circlet-Lite_…apk`) · Package `io.github.ankitchanki.circlet`

Forked from [parallelcc/MiCTS](https://github.com/parallelcc/micts) (GPL-3.0). Credit for the original trigger work goes to that project.

![Swipe, follow, arm, lock](docs/images/ui/flow.png)

<sub>The images on this page are renders of Circlet's overlay drawing code: the same curves, sizes and colours as the app (`docs/images/ui/render.html`). They are not device screenshots, and the app in the background is a placeholder. The last step hands over to Google's own Circle to Search UI.</sub>

---

## Two editions: Full and Lite

Every release ships two APKs, built from the same code and signed with the same key. Both use the same package name, so either installs over the other and your settings carry over.

| | **Circlet Full** | **Circlet Lite** |
| --- | :---: | :---: |
| Native Circle to Search from the icon, tile and Quick Ball | ✅ | ✅ |
| Corner swipe gesture, beam, edge glow, granular haptics, "Show me where" | ✅ | ✅ |
| Shizuku needed | Optional | ❌ Not included |
| Screen recesses to a 0.96 rounded card during the swipe | ✅ (with Shizuku) | — (beam, glow and shimmer over the live screen) |
| *Display over other apps* granted automatically | ✅ (with Shizuku) | Grant it once by hand |
| One-tap *Set Google as default assistant* | ✅ (with Shizuku) | Set it in phone settings |
| *Stop Gemini on corner swipe*, *Find launcher assistant setting* | ✅ (with Shizuku) | — |
| `CSService` trigger (Android 15+, root Shizuku) | ✅ | — |
| APK size | Larger (includes Shizuku) | Smaller |

Pick **Lite** if you don't use Shizuku. Wireless-debugging Shizuku stops on every reboot; Lite doesn't care. Pick **Full** for the recessed-card animation and the one-tap helpers.

## Features

| | |
| --- | --- |
| 🌈 **Corner swipe gesture** | Swipe inward at about 45° from just above the bottom-left or bottom-right corner. Progress follows your finger; let go early and everything springs back. |
| ✨ **Pixel-style animation** | Corner handle lines turn into a blue/red/yellow/green crescent beam. The screen recesses to 0.96 into a rounded card, and a white shimmer flashes when it locks. |
| 🔆 **Edge glow** | A rotating, breathing rainbow glow around the screen edges (in the spirit of Apple Intelligence) that grows with the swipe and swells on activation. |
| 📳 **Rising haptics** | A fine, dial-like tick about every 6dp (24 detents) that gets stronger as you swipe and softer when you pull back, then a crisp double-tap confirm. Uses haptic primitives, or amplitude-scaled pulses on motors without them. |
| 🔍 **Native Circle to Search** | Opens Google's own Circle to Search, from the same activity as the app icon. |
| 🚫 **Stops Gemini stealing the swipe** | Optionally turns off Android's own corner-assistant gesture while Circlet's gesture is on, and restores it afterwards. |
| 🛡️ **Shizuku, not root (Full)** | Overlay permission, the frozen frame for the animation and system settings go through Shizuku. No hooks, no LSPosed. Lite works without it. |
| 🔒 **Signed releases** | Every GitHub release is signed with the project's release key, so updates install over each other. |
| 🧩 **More ways to trigger** | App icon, Quick Settings tile, or any automation app (Quick Ball, ShortX, Routines+…) that can launch an app. |

## Requirements

Circle to Search must already work on your phone when you **tap the Circlet icon**. Circlet adds a nicer way to trigger it; it can't enable Circle to Search where Google hasn't.

- Google app up to date and set as the **default digital assistant**.
- Full edition only: Shizuku (optional) for the recessed-card animation and one-tap helpers. Lite needs nothing extra.

## Quick start

1. Install the [latest Circlet release](https://github.com/ankitchanki/CTS/releases/latest) and tap its icon once. Google's Circle to Search should open. If Gemini or nothing opens, see [phones without native Circle to Search](#phones-without-native-circle-to-search).
2. *(Full only, optional)* Install [Shizuku](https://shizuku.rikka.app/), start it (wireless debugging works), open Circlet settings and tap **Grant**.
3. Turn on **Corner swipe gesture**. With Shizuku, *Display over other apps* is granted automatically; otherwise Android opens the page, so allow Circlet there.
4. *(Full)* Keep **Stop Gemini on corner swipe** on. It helps on Pixel/AOSP launchers.
5. Start the swipe **just above** the navigation bar in a bottom corner and move inward at about 45°.
   Not sure where? Circlet settings shows a diagram under *Corner swipe gesture*, and **Show me where** makes the two zones glow on screen with an arrow. They also glow once when you turn the gesture on.
6. Want a one-tap trigger too? Add the Circlet Quick Settings tile, or on Xiaomi/POCO add Circlet to **Quick ball** (*Settings → Additional settings → Quick ball*). Both open Circle to Search instantly.

![Corner zones sit above the navigation bar](docs/images/ui/01-corner-zones.png)

The touch zones sit just above the navigation bar or gesture strip. That strip belongs to the system: touches there go to Recents, Home or Back, not to apps.

## Phones without native Circle to Search

If tapping the Circlet icon opens Gemini (or nothing), Google hasn't enabled Circle to Search for your model. Google decides that on its servers, and an app can't change it without root. With root:

1. Keep **Google** as the default assistant (Circlet: **Set Google as default assistant**).
2. Enable Google's Circle to Search flags for your device with GMS Flags, or use a root device-spoof module (see the MiCTS project's notes).
3. Check again by tapping the Circlet icon. Once Circle to Search opens, the corner swipe opens it too.

On Android 15+ with Shizuku running **as root**, **System trigger service → CSService** is another route on phones whose system ships the contextual search service.

## How it looks

| Swipe starts | Swipe at 60% | Armed (100%) | Locked → native Circle to Search |
| :---: | :---: | :---: | :---: |
| <img src="docs/images/ui/02-swipe-start.png" width="180"> | <img src="docs/images/ui/03-swipe-60.png" width="180"> | <img src="docs/images/ui/04-armed.png" width="180"> | <img src="docs/images/ui/05-locked-shimmer.png" width="180"> |
| Handle lines turn into the beam | Glow and card recess follow your finger | 0.96 rounded card, full glow, haptic confirm | Shimmer, then Google's Circle to Search opens |

## Settings reference

Lite shows only the gesture and app settings; the Shizuku, trigger-service and assistant rows are Full-only.

- **Shizuku**: connection status, with a button to grant or open Shizuku.
- **System trigger service**
  - `VIS`: works without Shizuku when Google is the default assistant. This is what the icon, tile and swipe use.
  - `CSService`: Android 15+. Needs Shizuku **started with root**, because the shell user lacks `ACCESS_CONTEXTUAL_SEARCH`, and a phone whose system has the contextual search service. Falls back to `VIS` otherwise.
- **Set Google as default assistant**: one tap through Shizuku.
- **Corner swipe gesture**: shows the corner zones.
- **Stop Gemini on corner swipe**: turns off Android's corner-assistant gesture while Circlet's gesture is on, and restores your setting when you turn it off. Pixel/AOSP launchers only.
- **Find launcher assistant setting**: read-only search for assistant or corner gesture settings on your phone, with a Copy button.
- **App settings**: launch and tile delays, vibration, async trigger, language.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| Swipe opens Google Lens, the icon opens Circle to Search | Fixed in v1.4.0: the swipe now triggers from the same activity as the icon. |
| Touching a corner opens Recents | Start the swipe slightly above the navigation bar; the zones sit there since v1.3.0. |
| Corner swipe opens Gemini | *Stop Gemini on corner swipe* changes the AOSP/Pixel setting, which HyperOS, One UI and some other launchers ignore. Turn off the launcher's own corner-assistant option in phone settings (on HyperOS look under *Additional settings → Gesture shortcuts* and the full-screen gesture settings), and start Circlet's swipe slightly above the navigation bar. **Find launcher assistant setting** in Circlet lists the matching system keys (read-only); please share the result in an issue so Circlet can switch your launcher's key automatically. |
| "Couldn't open Circle to Search" / "Trigger failed" now and then | The system stopped the Google app in the background, so its assistant service wasn't connected yet. Since v1.5.1 Circlet retries automatically. To stop it happening: open the Google app's *App info* → *Battery saver* → **No restrictions**, and turn on **Autostart** (on Xiaomi/POCO: *Settings → Apps → Manage apps → Google*). Do the same for Circlet so the corner gesture keeps running. |
| Icon opens Gemini, not Circle to Search | Circle to Search isn't enabled for your phone; see [phones without native Circle to Search](#phones-without-native-circle-to-search). |
| No recessed-card animation | It needs a Shizuku screenshot, so it only exists in Full with Shizuku granted. Circle to Search opens either way. |
| Update won't install over the old version | Builds before signed releases used a temporary key. Uninstall once, then install the signed release. |

## How it works

```
gesture/CornerVectorMath.kt      pure vector math: corner zones (above the nav bar), 45° direction, linear progress
gesture/CornerGestureTracker.kt  pure state machine: Idle → Tracking → Activated / Cancelled
gesture/GestureVisuals.kt        pure curves: progress/time → card scale, beam, glow, shimmer
gesture/HapticSteps.kt           pure rising-detent logic for haptics
gesture/GestureHaptics           vibration primitives with fallbacks
gesture/CornerGestureController  glue: touches → tracker → visuals → overlay windows, screenshot, hand-off
overlay/EffectsView              recessed card, corner beam, shimmer
overlay/EdgeGlowRenderer         rotating, breathing edge glow
overlay/GestureOverlayService    keeps the zones on screen; after the animation, starts the trigger activity
trigger/CircleToSearchTrigger    VIS / CSService trigger (shared by icon, tile and swipe)
shizuku/ShellService, ShizukuManager
                                 privileged calls (screencap, settings, roles, contextual search) as shell/root
```

The pure parts are unit tested: `./gradlew :app:testDebugUnitTest`.

Known limitation: overlays are drawn below the status and navigation bars, so the real bars stay on top of the recessed card during the animation.

## Design assets

The app icon, UI glyphs and overlay artwork each have a separate SVG in [`design/svg`](design/README.md). The README renders come from [`docs/images/ui/render.html`](docs/images/ui/render.html).

## Building and releasing

- Two product flavors: `full` (Shizuku: `app/src/full`) and `lite` (no-op `ShizukuManager`: `app/src/lite`). Shared code lives in `app/src/main`; `BuildConfig.SHIZUKU` hides the Shizuku-only settings in Lite.
- Local builds: `./gradlew :app:assembleFullRelease :app:assembleLiteRelease`.
- Every push builds both APKs and runs the unit tests in GitHub Actions (artifact `Circlet_<commit>`).
- To publish a release, run **Actions › CI Build › Run workflow** on `main` with a tag such as `v1.6.1`. The workflow creates the tag and publishes a GitHub Release with both APKs.
- Releases are signed with the keystore stored in the repository secrets `KEY_STORE`, `KEY_STORE_PASSWORD`, `ALIAS` and `KEY_PASSWORD`. A local build without them is signed with the debug key.

## License

GPL-3.0, same as upstream. See [LICENSE](LICENSE).
