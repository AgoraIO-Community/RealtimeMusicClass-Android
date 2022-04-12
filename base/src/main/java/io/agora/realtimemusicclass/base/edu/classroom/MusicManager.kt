package io.agora.realtimemusicclass.base.edu.classroom

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.*
import java.lang.StringBuilder
import java.util.*

data class MusicInfo(
    var name: String,
    var id: String,
    var fileName: String,
    var lyricName: String,
    var fileDesPath: String,
    var lyricDesPath: String,
    var musicUrl: String? = null,
    var lrcUrl: String? = null,
)

interface MusicManagerListener {
    fun onMusicInitialized()
}

object MusicManager {
    private const val tag = "MusicManager"
    private const val configFileName = "musics.json"
    private const val bufferSize = 1024 * 128

    @Volatile
    private var hasInitialized: Boolean = false

    private val musicConfigs = mutableListOf<MusicConfigItem>()
    private var musicInfoArray = mutableListOf<MusicInfo>()

    private val listeners = mutableListOf<MusicManagerListener>()

    fun registerMusicManagerListener(listener: MusicManagerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeMusicManagerListener(listener: MusicManagerListener) {
        listeners.remove(listener)
    }

    fun initMusicManager(context: Context) {
        if (!hasInitialized && readConfig(context)) {
            copyMusics(context)
            hasInitialized = true
        }
    }

    private fun readConfig(context: Context): Boolean {
        var inputStream: InputStream? = null
        var scanner: Scanner? = null
        return try {
            inputStream = context.assets.open(configFileName)
            scanner = Scanner(inputStream)
            val builder = StringBuilder()
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine().trim())
            }

            val configs = Gson().fromJson(builder.toString(),
                Array<MusicConfigItem>::class.java)
            configs.forEach {
                musicConfigs.add(it)
            }
            true
        } catch (e: IOException) {
            false
        } finally {
            inputStream?.close()
            scanner?.close()
        }
    }

    @Throws(IOException::class)
    fun copyMusics(context: Context) {
        val localDir = context.filesDir.absolutePath
        val buffer = ByteArray(bufferSize)
        musicConfigs.forEach { item ->
            if (item.name.isBlank() || item.identifier.isBlank() ||
                    item.music.isBlank() || item.lyric.isBlank()) {
                Log.w(tag, "Invalid music config format, $item")
                return@forEach
            }

            val info = MusicInfo(
                item.name, item.identifier, item.music, item.lyric,
                localDir + File.separator + item.music,
                localDir + File.separator + item.lyric)
            musicInfoArray.add(info)

            copyFile(context, info.fileName, info.fileDesPath, buffer)
            copyFile(context, info.lyricName, info.lyricDesPath, buffer)
        }

        listeners.forEach {
            it.onMusicInitialized()
        }
    }

    private fun copyFile(context: Context, fileName: String, desPath: String, buffer: ByteArray? = null) {
        var inputStream : InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val out = File(desPath)
            if (out.exists()) {
                return
            }

            inputStream = context.assets.open(fileName)
            outputStream = FileOutputStream(out)

            val buf = buffer ?: ByteArray(bufferSize)
            while (true) {
                val len = inputStream.read(buf)
                if (len < 0) break
                outputStream.write(buf, 0, len)
            }
        } catch (e:IOException) {
            e.printStackTrace()
        } finally {
            outputStream?.flush()
            outputStream?.close()
            inputStream?.close()
        }
    }

    fun getMusicInfo(Index: Int): MusicInfo {
        return musicInfoArray[Index]
    }

    fun getMusicInfo(id: String?): MusicInfo? {
        for (info in musicInfoArray) {
            if (info.id == id) {
                return info
            }
        }
        return null
    }

    fun getMusicInfoList(): List<MusicInfo>{
        return musicInfoArray
    }

    fun isInitialized():Boolean {
        return hasInitialized
    }
}

data class MusicConfigItem(
    val name: String,
    val identifier: String,
    val music: String,
    val lyric: String)