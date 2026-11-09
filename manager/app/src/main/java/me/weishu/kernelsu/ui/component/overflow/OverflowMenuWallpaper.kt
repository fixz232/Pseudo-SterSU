package me.weishu.kernelsu.ui.component.overflow

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperCropDialog
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.adaptiveDialogContentMaxHeight
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import kotlin.math.roundToInt

internal data class OverflowMenuWallpaperState(
    val uriString: String?,
    val bitmap: ImageBitmap?,
    val crop: CustomWallpaperCrop,
    val busy: Boolean,
    val onPick: () -> Unit,
    val onClear: () -> Unit,
    val onCropChange: (CustomWallpaperCrop) -> Unit,
    val opacity: Float,
    val onOpacityChange: (Float) -> Unit,
)

@Composable
internal fun rememberOverflowMenuWallpaper(target: OverflowMenuTarget): OverflowMenuWallpaperState {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("overflow_menu_appearance", Context.MODE_PRIVATE) }
    var uriString by remember(preferences, target) { mutableStateOf(preferences.getString(target.wallpaperKey, null)) }
    var crop by remember(preferences, target) { mutableStateOf(decodeOverflowMenuCrop(preferences.getString(target.cropKey, null))) }
    var opacity by remember(preferences, target) { mutableFloatStateOf(sanitizeOverflowMenuOpacity(preferences.getFloat(target.opacityKey, 1f))) }
    var busy by remember(target) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    DisposableEffect(preferences, target) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == null || key == target.wallpaperKey || key == target.cropKey || key == target.opacityKey) {
                uriString = prefs.getString(target.wallpaperKey, null)
                crop = decodeOverflowMenuCrop(prefs.getString(target.cropKey, null))
                opacity = sanitizeOverflowMenuOpacity(prefs.getFloat(target.opacityKey, 1f))
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        uriString = preferences.getString(target.wallpaperKey, null)
        crop = decodeOverflowMenuCrop(preferences.getString(target.cropKey, null))
        opacity = sanitizeOverflowMenuOpacity(preferences.getFloat(target.opacityKey, 1f))
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun update(errorRes: Int = R.string.overflow_menu_wallpaper_error, operation: () -> Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { runCatching(operation).getOrDefault(false) }
                if (!saved) Toast.makeText(context, errorRes, Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            update {
                // Own a local copy: gallery permissions and deleted source photos cannot break it.
                val newReference = persistCustomImageReference(context, uri, target.wallpaperKey, maxBytes = 32L * 1024 * 1024)
                    ?: return@update false
                val validation = loadCustomImageBitmap(context, newReference, maxSide = 64)
                if (validation == null) {
                    releaseCustomImageReference(context, newReference)
                    return@update false
                }
                validation.recycle()
                val previous = preferences.getString(target.wallpaperKey, null)
                val previousCrop = preferences.getString(target.cropKey, null)
                if (preferences.edit().putString(target.wallpaperKey, newReference)
                        .putString(target.cropKey, encodeOverflowMenuCrop(FULL_CUSTOM_WALLPAPER_CROP)).commit()) {
                    if (previous != newReference) releaseCustomImageReference(context, previous)
                    true
                } else {
                    // A failed commit still updates SharedPreferences in memory.
                    if (preferences.edit().putString(target.wallpaperKey, previous)
                            .putString(target.cropKey, previousCrop).commit()) {
                        releaseCustomImageReference(context, newReference)
                    }
                    false
                }
            }
        }
    }
    return OverflowMenuWallpaperState(
        uriString = uriString,
        bitmap = rememberCustomImageBitmap(uriString, maxSide = 800, crop = crop),
        crop = crop,
        busy = busy,
        onPick = {
            if (!busy) {
                runCatching { picker.launch(arrayOf("image/*")) }.onFailure {
                    Toast.makeText(context, R.string.overflow_menu_wallpaper_error, Toast.LENGTH_LONG).show()
                }
            }
        },
        onClear = {
            update {
                val previous = preferences.getString(target.wallpaperKey, null)
                val previousCrop = preferences.getString(target.cropKey, null)
                preferences.edit().remove(target.wallpaperKey).remove(target.cropKey).commit().also { saved ->
                    if (saved) releaseCustomImageReference(context, previous)
                    else preferences.edit().putString(target.wallpaperKey, previous)
                        .putString(target.cropKey, previousCrop).commit()
                }
            }
        },
        onCropChange = { value ->
            update {
                if (preferences.getString(target.wallpaperKey, null) == null) return@update false
                val previousCrop = preferences.getString(target.cropKey, null)
                preferences.edit().putString(target.cropKey, encodeOverflowMenuCrop(value)).commit().also { saved ->
                    if (!saved) preferences.edit().putString(target.cropKey, previousCrop).commit()
                }
            }
        },
        opacity = opacity,
        onOpacityChange = { value ->
            update(errorRes = R.string.overflow_menu_appearance_error) {
                val previous = preferences.getFloat(target.opacityKey, 1f)
                preferences.edit().putFloat(target.opacityKey, sanitizeOverflowMenuOpacity(value)).commit().also { saved ->
                    if (!saved) preferences.edit().putFloat(target.opacityKey, previous).commit()
                }
            }
        },
    )
}

@Composable
internal fun OverflowMenuWallpaperDialog(state: OverflowMenuWallpaperState, onDismissRequest: () -> Unit) {
    var editingCrop by remember(state.uriString) { mutableStateOf(false) }
    var opacity by remember(state.opacity) { mutableFloatStateOf(state.opacity) }
    val opacitySlider = remember { SliderState(opacity, trackRange = 0f..1f) }
    opacitySlider.value = opacity
    val opacityLabel = stringResource(R.string.overflow_menu_background_opacity)
    val opacityValue = stringResource(R.string.overflow_menu_background_opacity_value, (opacity * 100).roundToInt())
    if (editingCrop && state.uriString != null) {
        SettingsWallpaperCropDialog(
            show = true,
            uriString = state.uriString,
            crop = state.crop,
            onCropChange = state.onCropChange,
            onDismissRequest = { editingCrop = false },
            editorAspectRatio = 1f,
            defaultCrop = FULL_CUSTOM_WALLPAPER_CROP,
            allowTransforms = false,
        )
        return
    }
    AlertDialog(
        onDismissRequest = { if (!state.busy) onDismissRequest() },
        title = { Text(stringResource(R.string.overflow_menu_appearance)) },
        text = {
            Column(
                Modifier.heightIn(max = adaptiveDialogContentMaxHeight()).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.overflow_menu_wallpaper_summary),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(opacityLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text(opacityValue, style = MaterialTheme.typography.labelLarge)
                    }
                    Slider(
                        state = opacitySlider,
                        onValueChange = { opacity = sanitizeOverflowMenuOpacity(it) },
                        onValueChangeFinished = { state.onOpacityChange(opacity) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                            contentDescription = opacityLabel
                            stateDescription = opacityValue
                        },
                    )
                    Text(
                        stringResource(R.string.overflow_menu_background_opacity_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { opacity = 1f; state.onOpacityChange(1f) }, enabled = !state.busy && opacity != 1f) {
                        Text(stringResource(R.string.overflow_menu_background_opacity_reset))
                    }
                }
                state.bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.overflow_menu_wallpaper),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(12.dp)),
                    )
                }
                if (state.busy) CircularProgressIndicator()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = state.onPick, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.overflow_menu_wallpaper_pick))
                    }
                    if (state.uriString != null) {
                        TextButton(
                            onClick = { editingCrop = true },
                            enabled = !state.busy && state.bitmap != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.overflow_menu_wallpaper_crop))
                        }
                    }
                }
                if (state.uriString != null) {
                    TextButton(onClick = state.onClear, enabled = !state.busy) {
                        Text(stringResource(R.string.overflow_menu_wallpaper_clear))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest, enabled = !state.busy) { Text(stringResource(android.R.string.ok)) } },
    )
}
