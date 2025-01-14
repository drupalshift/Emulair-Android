package com.bigbratan.emulair.fragments.pauseMenuActions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.bigbratan.emulair.R
import com.bigbratan.emulair.activities.pauseMenu.PauseMenuContract
import com.bigbratan.emulair.common.utils.graphics.GraphicsUtils
import com.bigbratan.emulair.common.metadata.retrograde.CoreID
import com.bigbratan.emulair.common.metadata.retrograde.SystemCoreConfig
import com.bigbratan.emulair.common.metadata.retrograde.db.entity.Game
import com.bigbratan.emulair.common.managers.saves.SaveInfo
import com.bigbratan.emulair.common.managers.saves.StatesPreviewManager
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

object PauseMenuPreferences {

    fun setupAudioOption(screen: PreferenceScreen, audioEnabled: Boolean, context: Context) {
        val preference = screen.findPreference<SwitchPreferenceCompat>(context.getString(R.string.pref_game_mute))
        preference?.isChecked = !audioEnabled
    }

    fun setupFastForwardOption(
        screen: PreferenceScreen,
        fastForwardEnabled: Boolean,
        fastForwardSupported: Boolean,
        context: Context
    ) {
        val preference = screen.findPreference<SwitchPreferenceCompat>(context.getString(R.string.pref_game_fast_forward))
        preference?.isChecked = fastForwardEnabled
        preference?.isVisible = fastForwardSupported
    }

    fun setupSaveLoadOptions(
        screen: PreferenceScreen,
        systemCoreConfig: SystemCoreConfig,
        context: Context
    ) {
        val savesOption = screen.findPreference<Preference>(context.getString(R.string.pref_game_section_save))
        savesOption?.isVisible = systemCoreConfig.statesSupported

        val loadOption = screen.findPreference<Preference>(context.getString(R.string.pref_game_section_load))
        loadOption?.isVisible = systemCoreConfig.statesSupported
    }

    fun setupCoreOptions(
        screen: PreferenceScreen,
        systemCoreConfig: SystemCoreConfig,
        context: Context
    ) {
        screen.findPreference<Preference>(context.getString(R.string.pref_game_section_core_options))?.isVisible = sequenceOf(
            systemCoreConfig.exposedSettings.isNotEmpty(),
            systemCoreConfig.exposedAdvancedSettings.isNotEmpty(),
            systemCoreConfig.controllerConfigs.values.any { it.size > 1 }
        ).any { it }
    }

    fun setupChangeDiskOption(
        activity: Activity?,
        screen: PreferenceScreen,
        currentDisk: Int,
        numDisks: Int,
        context: Context
    ) {
        val changeDiskPreference = screen.findPreference<ListPreference>(context.getString(R.string.pref_game_section_change_disk))
        changeDiskPreference?.isVisible = numDisks > 1

        changeDiskPreference?.entries = (0 until numDisks)
            .map {
                screen.context.resources.getString(
                    R.string.pause_menu_change_disk_option,
                    (it + 1).toString()
                )
            }
            .toTypedArray()

        changeDiskPreference?.entryValues = (0 until numDisks)
            .map { it.toString() }
            .toTypedArray()

        changeDiskPreference?.setValueIndex(currentDisk)
        changeDiskPreference?.setOnPreferenceChangeListener { _, newValue ->
            val resultIntent = Intent().apply {
                putExtra(PauseMenuContract.RESULT_CHANGE_DISK, (newValue as String).toInt())
            }
            setResultAndFinish(activity, resultIntent)
            true
        }
    }

    fun addSavePreference(
        screen: PreferenceScreen,
        index: Int,
        saveStateInfo: SaveInfo,
        bitmap: Bitmap?,
    ) {
        screen.addPreference(
            Preference(screen.context).apply {
                val keyConcat = "pref_game_save_$index"
                this.key = context.getString(context.resources.getIdentifier(keyConcat, "string", context.packageName))
                this.summary = getDateString(saveStateInfo)
                this.title = context.getString(R.string.pause_menu_state, (index + 1).toString())
                this.icon = BitmapDrawable(screen.context.resources, bitmap)
                this.layoutResource = (R.layout.layout_preference_pausemenu_simple_image)
            }
        )
    }

    fun addLoadPreference(
        screen: PreferenceScreen,
        index: Int,
        saveStateInfo: SaveInfo,
        bitmap: Bitmap?,
    ) {
        screen.addPreference(
            Preference(screen.context, null).apply {
                val keyConcat = "pref_game_load_$index"
                this.key = context.getString(context.resources.getIdentifier(keyConcat, "string", context.packageName))
                this.summary = getDateString(saveStateInfo)
                this.isEnabled = saveStateInfo.exists
                this.title = context.getString(R.string.pause_menu_state, (index + 1).toString())
                this.icon = BitmapDrawable(screen.context.resources, bitmap)
                this.layoutResource = (R.layout.layout_preference_pausemenu_simple_image)
            }
        )
    }

    fun onPreferenceTreeClicked(activity: Activity?, preference: Preference?, context: Context?): Boolean {
        return when (preference?.key) {
            context?.getString(R.string.pref_game_save_0) -> handleSaveAction(activity, 0)
            context?.getString(R.string.pref_game_save_1) -> handleSaveAction(activity, 1)
            context?.getString(R.string.pref_game_save_2) -> handleSaveAction(activity, 2)
            context?.getString(R.string.pref_game_save_3) -> handleSaveAction(activity, 3)
            context?.getString(R.string.pref_game_save_4) -> handleSaveAction(activity, 4)
            // -- //
            context?.getString(R.string.pref_game_load_0) -> handleLoadAction(activity, 0)
            context?.getString(R.string.pref_game_load_1) -> handleLoadAction(activity, 1)
            context?.getString(R.string.pref_game_load_2) -> handleLoadAction(activity, 2)
            context?.getString(R.string.pref_game_load_3) -> handleLoadAction(activity, 3)
            context?.getString(R.string.pref_game_load_4) -> handleLoadAction(activity, 4)
            // -- //
            context?.getString(R.string.pref_game_mute) -> {
                val currentValue = (preference as SwitchPreferenceCompat).isChecked
                val resultIntent = Intent().apply {
                    putExtra(PauseMenuContract.RESULT_ENABLE_AUDIO, !currentValue)
                }
                setResultAndFinish(activity, resultIntent)
                true
            }
            context?.getString(R.string.pref_game_fast_forward) -> {
                val currentValue = (preference as SwitchPreferenceCompat).isChecked
                val resultIntent = Intent().apply {
                    putExtra(PauseMenuContract.RESULT_ENABLE_FAST_FORWARD, currentValue)
                }
                setResultAndFinish(activity, resultIntent)
                true
            }
            context?.getString(R.string.pref_game_edit_touch_controls) -> {
                val resultIntent = Intent().apply {
                    putExtra(PauseMenuContract.RESULT_EDIT_TOUCH_CONTROLS, true)
                }
                setResultAndFinish(activity, resultIntent)
                true
            }
            else -> false
        }
    }

    private fun handleSaveAction(activity: Activity?, index: Int): Boolean {
        val resultIntent = Intent().apply {
            putExtra(PauseMenuContract.RESULT_SAVE, index)
        }
        setResultAndFinish(activity, resultIntent)
        return true
    }

    private fun handleLoadAction(activity: Activity?, index: Int): Boolean {
        val resultIntent = Intent().apply {
            putExtra(PauseMenuContract.RESULT_LOAD, index)
        }
        setResultAndFinish(activity, resultIntent)
        return true
    }

    private fun setResultAndFinish(activity: Activity?, resultIntent: Intent) {
        activity?.setResult(Activity.RESULT_OK, resultIntent)
        activity?.finish()
    }

    private fun getDateString(saveInfo: SaveInfo): String {
        val formatter = SimpleDateFormat.getDateTimeInstance()
        return if (saveInfo.exists) {
            formatter.format(saveInfo.date)
        } else {
            ""
        }
    }

    suspend fun getSaveStateBitmap(
        context: Context,
        statesPreviewManager: StatesPreviewManager,
        saveStateInfo: SaveInfo,
        game: Game,
        coreID: CoreID,
        index: Int,
    ): Bitmap? {
        if (!saveStateInfo.exists) return null
        val imageSize = GraphicsUtils.convertDpToPixel(96f, context).roundToInt()
        return statesPreviewManager.getPreviewForSlot(game, coreID, index, imageSize)
    }
}
