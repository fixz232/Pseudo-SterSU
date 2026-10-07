package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.MediaTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDevicePhotoTest {
    @Test fun imageAndCropSurviveRoundTripTogether() {
        val photo = SettingsDevicePhoto("file:///private/device.image", CustomWallpaperCrop(0.2f, 0.1f, 0.8f, 0.9f))
        assertEquals(photo, SettingsDevicePhoto.decode(photo.encode()))
    }

    @Test fun absentOrCorruptPreferencesFallBackToPlaceholder() {
        listOf(null, "", "broken", "{}", "{\"uri\":\"\"}").forEach {
            assertNull(SettingsDevicePhoto.decode(it))
        }
    }

    @Test fun olderImageOnlyPreferenceDefaultsToWholeImage() {
        val photo = SettingsDevicePhoto.decode("{\"uri\":\"file:///device.image\"}")!!
        assertEquals(FULL_CUSTOM_WALLPAPER_CROP, photo.crop)
        assertEquals(MediaTransform(), photo.transform)
    }

    @Test fun outOfRangeCropsAreSanitizedBeforeDecoding() {
        val photo = SettingsDevicePhoto.decode(
            "{\"uri\":\"file:///device.image\",\"left\":-20,\"top\":2,\"right\":-1,\"bottom\":20}",
        )!!
        assertTrue(photo.crop.left >= 0f)
        assertTrue(photo.crop.top >= 0f)
        assertTrue(photo.crop.right <= 1f)
        assertTrue(photo.crop.bottom <= 1f)
        assertTrue(photo.crop.width > 0f)
        assertTrue(photo.crop.height > 0f)
    }

    @Test fun editingACropDoesNotMutateTheSavedImage() {
        val original = SettingsDevicePhoto("file:///saved.image")
        val editing = original.copy(crop = CustomWallpaperCrop(0.2f, 0.2f, 0.7f, 0.7f))
        assertEquals(FULL_CUSTOM_WALLPAPER_CROP, original.crop)
        assertTrue(original.crop != editing.crop)
    }
}
