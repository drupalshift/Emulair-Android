/*
 * LocalGameLibraryProvider.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bigbratan.emulair.common.managers.storage.local

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
// import androidx.leanback.preference.LeanbackPreferenceFragment
import com.bigbratan.emulair.common.utils.kotlin.extractEntryToFile
import com.bigbratan.emulair.common.utils.kotlin.isZipped
import com.bigbratan.emulair.common.R
import com.bigbratan.emulair.common.metadata.retrograde.db.entity.DataFile
import com.bigbratan.emulair.common.metadata.retrograde.db.entity.Game
import com.bigbratan.emulair.common.managers.preferences.SharedPreferencesHelper
import com.bigbratan.emulair.common.managers.storage.BaseStorageFile
import com.bigbratan.emulair.common.managers.storage.DirectoriesManager
import com.bigbratan.emulair.common.managers.storage.RomFiles
import com.bigbratan.emulair.common.managers.storage.StorageFile
import com.bigbratan.emulair.common.managers.storage.StorageProvider
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalStorageProvider(
    private val context: Context,
    private val directoriesManager: DirectoriesManager
) : StorageProvider {

    override val id: String = "local"

    override val name: String = context.getString(R.string.local_storage)

    override val uriSchemes = listOf("file")

    // override val prefsFragmentClass: Class<LeanbackPreferenceFragment>? = null

    override val enabledByDefault = true

    override fun listBaseStorageFiles(): Flow<List<BaseStorageFile>> =
        walkDirectory(getExternalFolder() ?: directoriesManager.getInternalRomsDirectory())

    override fun getStorageFile(baseStorageFile: BaseStorageFile): StorageFile? {
        return DocumentFileParser.parseDocumentFile(context, baseStorageFile)
    }

    private fun getExternalFolder(): File? {
        val prefString = context.getString(R.string.pref_key_legacy_external_folder)
        val preferenceManager = SharedPreferencesHelper.getLegacySharedPreferences(context)
        return preferenceManager.getString(prefString, null)?.let { File(it) }
    }

    private fun walkDirectory(rootDirectory: File): Flow<List<BaseStorageFile>> = flow {
        val directories = mutableListOf(rootDirectory)

        while (directories.isNotEmpty()) {
            val directory = directories.removeAt(0)
            val groups = directory.listFiles()
                ?.filterNot { it.name.startsWith(".") }
                ?.groupBy { it.isDirectory } ?: mapOf()

            val newDirectories = groups[true] ?: listOf()
            val newFiles = groups[false] ?: listOf()

            directories.addAll(newDirectories)
            emit((newFiles.map { BaseStorageFile(it.name, it.length(), it.toUri(), it.path) }))
        }
    }

    // There is no need to handle anything. Data file have to be in the same directory for detection we expect them
    // to still be there.
    private fun getDataFile(dataFile: DataFile): File {
        val dataFilePath = Uri.parse(dataFile.fileUri).path
        return File(dataFilePath)
    }

    private fun getGameRom(game: Game): File {
        val gamePath = Uri.parse(game.fileUri).path
        val originalFile = File(gamePath)
        if (!originalFile.isZipped() || originalFile.name == game.fileName) {
            return originalFile
        }

        val cacheFile = GameCacheUtils.getCacheFileForGame(LOCAL_STORAGE_CACHE_SUBFOLDER, context, game)
        if (cacheFile.exists()) {
            return cacheFile
        }

        if (originalFile.isZipped()) {
            val stream = ZipInputStream(originalFile.inputStream())
            stream.extractEntryToFile(game.fileName, cacheFile)
        }

        return cacheFile
    }

    override fun getGameRomFiles(
        game: Game,
        dataFiles: List<DataFile>,
        allowVirtualFiles: Boolean
    ): RomFiles {
        return RomFiles.Standard(listOf(getGameRom(game)) + dataFiles.map { getDataFile(it) })
    }

    override fun getInputStream(uri: Uri): InputStream {
        return File(uri.path).inputStream()
    }

    companion object {
        const val LOCAL_STORAGE_CACHE_SUBFOLDER = "local-storage-games"
    }
}
