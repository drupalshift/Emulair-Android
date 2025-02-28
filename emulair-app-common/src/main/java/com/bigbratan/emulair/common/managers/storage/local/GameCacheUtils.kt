package com.bigbratan.emulair.common.managers.storage.local

import android.content.Context
import com.bigbratan.emulair.common.metadata.retrograde.db.entity.DataFile
import com.bigbratan.emulair.common.metadata.retrograde.db.entity.Game
import java.io.File

object GameCacheUtils {

    fun getDataFileForGame(
        folderName: String,
        context: Context,
        game: Game,
        dataFile: DataFile
    ): File {
        val gamesCacheDir = getCacheDirForGame(folderName, game, context)
        return File(gamesCacheDir, dataFile.fileName)
    }

    fun getCacheFileForGame(folderName: String, context: Context, game: Game): File {
        val gamesCacheDir = getCacheDirForGame(folderName, game, context)
        return File(gamesCacheDir, game.fileName)
    }

    private fun getCacheDirForGame(folderName: String, game: Game, context: Context): File {
        val gamesCachePath = buildPath(folderName, game.systemId)
        val gamesCacheDir = File(context.cacheDir, gamesCachePath)
        gamesCacheDir.mkdirs()
        return gamesCacheDir
    }

    private fun buildPath(vararg chunks: String): String {
        return chunks.joinToString(separator = File.separator)
    }
}
