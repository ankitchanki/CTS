package io.github.ankitchanki.circlet.config
import android.annotation.SuppressLint
import io.github.ankitchanki.circlet.R
import java.util.Locale

enum class Language(val id: Int, val toLocale: () -> Locale) {
    FollowSystem(R.string.follow_system, { Locale.getDefault() }),
    Arabic(R.string.arabic, { Locale("ar") }),
    ChineseSimplified(R.string.chinese_simplified, { Locale.SIMPLIFIED_CHINESE }),
    ChineseTraditional(R.string.chinese_traditional, { Locale.TRADITIONAL_CHINESE }),
    English(R.string.english, { Locale.ENGLISH }),
    Greek(R.string.greek, { Locale("el") }),
    Japanese(R.string.japanese, { Locale.JAPANESE }),
    Odia(R.string.odia, { Locale("or") }),
    Persian(R.string.persian, { Locale("fa") }),
    Russian(R.string.russian, { Locale("ru") }),
    Spanish(R.string.spanish, { Locale("es") }),
    Turkish(R.string.turkish, { Locale("tr") }),
    Vietnamese(R.string.vietnamese, { Locale("vi") }),
}

/**
 * [needsShizukuRoot]: CSService checks ACCESS_CONTEXTUAL_SEARCH, which the shell user (Shizuku via
 * ADB) doesn't hold, so it only works when Shizuku runs as root.
 */
enum class TriggerService(val isSupported: Boolean, val needsShizukuRoot: Boolean) {
    VIS(true, false),
    @SuppressLint("PrivateApi")
    CSService(
        try {
            Class.forName("android.app.contextualsearch.ContextualSearchManager")
            true
        } catch (_: Exception) {
            false
        },
        true
    );

    companion object {
        fun getSupportedServices(): List<TriggerService> {
            return TriggerService.entries.filter { it.isSupported }
        }
    }
}

/** What happens once the corner swipe gesture reaches full progress. */
enum class GestureAction(val id: Int) {
    // Frozen screenshot + lasso selection handled by this app (needs Shizuku for screencap).
    BuiltInLasso(R.string.gesture_action_lasso),
    // Hand off to the system / Google Circle to Search via the selected trigger service.
    SystemCircleToSearch(R.string.gesture_action_system),
}

object AppConfig {
    const val CONFIG_NAME = "app_config"
    const val KEY_LANGUAGE = "language"
    const val KEY_DEFAULT_DELAY = "default_delay"
    const val KEY_TILE_DELAY = "tile_delay"
    const val KEY_VIBRATE = "vibrate"
    const val KEY_ASYNC_TRIGGER = "async_trigger"
    const val KEY_TRIGGER_SERVICE = "trigger_service"
    const val KEY_CORNER_GESTURE = "corner_gesture"
    const val KEY_GESTURE_ACTION = "gesture_action"
    const val KEY_BLOCK_ASSIST_GESTURE = "block_assist_gesture"
    /** Original value of the system assist gesture setting, kept while Circlet has it turned off. */
    const val KEY_SAVED_ASSIST_GESTURE = "saved_assist_gesture"

    val DEFAULT_CONFIG = mapOf<String, Any>(
        KEY_LANGUAGE to Language.FollowSystem.ordinal,
        KEY_DEFAULT_DELAY to 0L,
        KEY_TILE_DELAY to 400L,
        KEY_VIBRATE to false,
        KEY_ASYNC_TRIGGER to false,
        KEY_TRIGGER_SERVICE to TriggerService.VIS.ordinal,
        KEY_CORNER_GESTURE to false,
        KEY_GESTURE_ACTION to GestureAction.BuiltInLasso.ordinal,
        KEY_BLOCK_ASSIST_GESTURE to true,
    )
}
