package io.github.ankitchanki.circlet.assist

import android.service.voice.VoiceInteractionService

/**
 * Makes Circlet a full voice interaction service, not just an ACTION_ASSIST activity. Some OEM
 * launchers (e.g. HyperOS) invoke the assistant through VoiceInteractionManagerService directly,
 * which only reaches apps that provide this service. Sessions are created by [CircletSessionService].
 */
class CircletVoiceInteractionService : VoiceInteractionService()
