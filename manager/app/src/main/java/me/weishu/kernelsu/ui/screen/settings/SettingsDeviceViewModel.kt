package me.weishu.kernelsu.ui.screen.settings

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.MediaTransform
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import org.json.JSONObject

private const val PHOTO_KEY = "settings_device_photo"
private const val PENDING_PHOTO_KEY = "settings_device_photo_pending"
private const val PICKER_RESULT_KEY = "settings_device_picker_result"
private const val MAX_PHOTO_BYTES = 20L * 1024 * 1024

internal data class SettingsDevicePhoto(
    val uri: String,
    val crop: CustomWallpaperCrop = FULL_CUSTOM_WALLPAPER_CROP,
    val transform: MediaTransform = MediaTransform(),
) {
    fun encode(): String = JSONObject().put("uri", uri)
        .put("left", crop.left.toDouble()).put("top", crop.top.toDouble())
        .put("right", crop.right.toDouble()).put("bottom", crop.bottom.toDouble())
        .put("transform", transform.toJson()).toString()

    companion object {
        fun decode(value: String?): SettingsDevicePhoto? = runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            val json = JSONObject(value)
            val uri = json.optString("uri").takeIf(String::isNotBlank) ?: return@runCatching null
            SettingsDevicePhoto(
                uri,
                sanitizeCustomWallpaperCrop(CustomWallpaperCrop(
                    json.optDouble("left", 0.0).toFloat(), json.optDouble("top", 0.0).toFloat(),
                    json.optDouble("right", 1.0).toFloat(), json.optDouble("bottom", 1.0).toFloat(),
                )),
                MediaTransform.fromJson(json.optJSONObject("transform")),
            )
        }.getOrNull()
    }
}

internal data class SettingsDeviceState(
    val info: SettingsDeviceInfo = SettingsDeviceInfo(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val photo: SettingsDevicePhoto? = null,
    val bitmap: Bitmap? = null,
    val editing: SettingsDevicePhoto? = null,
    val error: Int? = null,
)

internal class SettingsDeviceViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val preferences = context.getSharedPreferences("settings_device_card", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(SettingsDeviceState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val editing = SettingsDevicePhoto.decode(savedState[PENDING_PHOTO_KEY])
            val loaded = withContext(Dispatchers.IO) {
                val photo = SettingsDevicePhoto.decode(preferences.getString(PHOTO_KEY, null))
                // Pending imports survive recreation; abandoned imports are reclaimed next time.
                val orphan = preferences.getString(PENDING_PHOTO_KEY, null)
                if (orphan != null && orphan != photo?.uri && orphan != editing?.uri) {
                    releaseCustomImageReference(context, orphan)
                    preferences.edit().remove(PENDING_PHOTO_KEY).apply()
                }
                SettingsDeviceState(
                    info = readSettingsDeviceInfo(context), loading = false,
                    photo = photo, bitmap = photo?.let(::decodePhoto), editing = editing,
                )
            }
            _state.value = loaded
            savedState.remove<String>(PICKER_RESULT_KEY)?.let { pickPhoto(Uri.parse(it)) }
        }
    }

    fun pickPhoto(uri: Uri) {
        // An activity result may arrive before loading completes after process recreation.
        if (_state.value.loading) {
            savedState[PICKER_RESULT_KEY] = uri.toString()
            return
        }
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            var imported: String? = null
            var retained = false
            try {
                val photo = withContext(Dispatchers.IO) {
                    imported = persistCustomImageReference(context, uri, PHOTO_KEY, MAX_PHOTO_BYTES)
                    val source = imported ?: error("Cannot copy photo")
                    val probe = loadCustomImageBitmap(context, source, 512, FULL_CUSTOM_WALLPAPER_CROP)
                        ?: error("Unsupported image")
                    probe.recycle()
                    check(preferences.edit().putString(PENDING_PHOTO_KEY, source).commit())
                    SettingsDevicePhoto(source)
                }
                savedState[PENDING_PHOTO_KEY] = photo.encode()
                retained = true
                _state.update { it.copy(editing = photo) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = R.string.settings_device_photo_failed) }
            } finally {
                if (!retained) withContext(NonCancellable + Dispatchers.IO) {
                    releaseCustomImageReference(context, imported)
                    preferences.edit().remove(PENDING_PHOTO_KEY).commit()
                }
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun editPhoto() {
        if (_state.value.busy) return
        val photo = _state.value.photo ?: return
        savedState[PENDING_PHOTO_KEY] = photo.encode()
        _state.update { it.copy(editing = photo) }
    }

    fun cancelEdit() {
        if (_state.value.busy) return
        val editing = _state.value.editing ?: return
        val currentUri = _state.value.photo?.uri
        savedState.remove<String>(PENDING_PHOTO_KEY)
        _state.update { it.copy(editing = null, busy = true) }
        viewModelScope.launch {
            try {
                withContext(NonCancellable + Dispatchers.IO) {
                    if (editing.uri != currentUri) releaseCustomImageReference(context, editing.uri)
                    preferences.edit().remove(PENDING_PHOTO_KEY).commit()
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun savePhoto(crop: CustomWallpaperCrop, transform: MediaTransform) {
        val editing = _state.value.editing ?: return
        if (_state.value.busy) return
        val photo = editing.copy(crop = sanitizeCustomWallpaperCrop(crop), transform = transform.normalized())
        val previous = _state.value.photo
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                // Once committed, finish the in-memory handoff even if this page is leaving.
                withContext(NonCancellable) {
                    val bitmap = withContext(Dispatchers.IO) {
                        val decoded = decodePhoto(photo) ?: error("Cannot decode photo")
                        check(preferences.edit().putString(PHOTO_KEY, photo.encode())
                            .remove(PENDING_PHOTO_KEY).commit())
                        if (previous?.uri != photo.uri) releaseCustomImageReference(context, previous?.uri)
                        decoded
                    }
                    savedState.remove<String>(PENDING_PHOTO_KEY)
                    _state.update { it.copy(photo = photo, bitmap = bitmap, editing = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = R.string.settings_device_photo_failed) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun clearPhoto() {
        if (_state.value.busy || _state.value.loading) return
        val previous = _state.value.photo ?: return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    withContext(Dispatchers.IO) {
                        check(preferences.edit().remove(PHOTO_KEY).remove(PENDING_PHOTO_KEY).commit())
                        releaseCustomImageReference(context, previous.uri)
                    }
                    savedState.remove<String>(PENDING_PHOTO_KEY)
                    _state.update { it.copy(photo = null, bitmap = null, editing = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = R.string.settings_device_photo_failed) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun decodePhoto(photo: SettingsDevicePhoto): Bitmap? {
        val bitmap = loadCustomImageBitmap(context, photo.uri, 1024, photo.crop) ?: return null
        val transform = photo.transform.normalized()
        if (transform == MediaTransform()) return bitmap
        val matrix = Matrix().apply {
            if (transform.flipHorizontal) postScale(-1f, 1f)
            postRotate(transform.quarterTurns * 90f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }
}
