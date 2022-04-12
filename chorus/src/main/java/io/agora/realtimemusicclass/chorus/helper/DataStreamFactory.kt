package io.agora.realtimemusicclass.chorus.helper

import com.google.gson.Gson

data class MusicSyncMessage(
    var type: Int,
    var msg: String?
)

data class CheckReq(
    var uid: String,
    var startTs: Long
)

data class CheckResp(
    var remoteUid: String,
    var remoteTS: Long,
    var broadTs: Long,
    var position: Int
)

data class MusicState(
    var position: Long,
    var bgmId: String,
    var broadTs: Long,
    var Duration: Long
)

enum class SyncCmdType(val value: Int) {
    StartCheckTs(0),
    CheckTsResp(1),
    PlayStatus(2),
    PauseStatus(3),
}

class DataStreamFactory {
    private val gson = Gson()

    fun buildStartCheckTs(info: CheckReq): String {
        val msg = gson.toJson(info)
        val data = MusicSyncMessage(SyncCmdType.StartCheckTs.value, msg)
        return gson.toJson(data)
    }

    fun buildCheckTsResp(info: CheckResp): String {
        val msg = gson.toJson(info)
        val data = MusicSyncMessage(SyncCmdType.CheckTsResp.value, msg)
        return gson.toJson(data)
    }

    fun buildMusicPlayState(info: MusicState): String {
        val msg = gson.toJson(info)
        val data = MusicSyncMessage(SyncCmdType.PlayStatus.value, msg)
        return gson.toJson(data)
    }

    fun buildMusicPauseState(info: MusicState): String {
        val msg = gson.toJson(info)
        val data = MusicSyncMessage(SyncCmdType.PauseStatus.value, msg)
        return gson.toJson(data)
    }
}