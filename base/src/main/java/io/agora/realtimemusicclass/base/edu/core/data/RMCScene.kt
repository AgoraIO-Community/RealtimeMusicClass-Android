package io.agora.realtimemusicclass.base.edu.core.data

enum class RMCSceneType(val value: Int) {
    Chorus(0), Instruments(1), Piano(2)
}

class RMCSceneParams {
    companion object {
        fun getSceneParams(type: RMCSceneType): String {
            return when (type) {
                RMCSceneType.Chorus -> "{" +
                        "\"rtc\":" +
                        "  [" +
                        "    \"{\\\"rtc.audio_resend\\\":false}\"," +
                        "    \"{\\\"rtc.audio_fec\\\":[3,2]}\"," +
                        "    \"{\\\"rtc.audio.opensl.mode\\\":0}\"," +
                        "    \"{\\\"rtc.audio.aec_length\\\":50}\"" +
                        "  ]," +
                        "\"rtm\":[]" +
                        "}"
                RMCSceneType.Instruments,
                RMCSceneType.Piano -> ""
            }
        }
    }
}