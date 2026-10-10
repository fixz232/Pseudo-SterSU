package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.LocalSidebarGlassBackdrop
import me.weishu.kernelsu.ui.screen.home.SidebarHomeCardSurface
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.SidebarCardGradient
import me.weishu.kernelsu.ui.util.SidebarCardMaterial
import me.weishu.kernelsu.ui.util.SidebarHomeCardId
import me.weishu.kernelsu.ui.util.SidebarHomeCardStyle
import me.weishu.kernelsu.ui.util.SidebarHomeCards
import me.weishu.kernelsu.ui.util.SidebarHomeLayout
import me.weishu.kernelsu.ui.util.SidebarWidgetConfig
import me.weishu.kernelsu.ui.util.formatSidebarRgb
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.parseSidebarRgb
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.sidebarHomeCardIds
import kotlin.math.roundToInt

@Composable
internal fun SidebarHomeCardOptions(config: SidebarWidgetConfig, onUpdate: (SidebarWidgetConfig) -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val latestConfig by rememberUpdatedState(config)
    val latestUpdate by rememberUpdatedState(onUpdate)
    val systemDark = isInDarkTheme()
    var editingDark by rememberSaveable { mutableStateOf(systemDark) }
    var layoutName by rememberSaveable(config.homeLayout) { mutableStateOf(config.homeLayout.value) }
    val layout = SidebarHomeLayout.fromValue(layoutName)
    val ids = sidebarHomeCardIds(layout)
    var selectedName by rememberSaveable { mutableStateOf(ids.first().value) }
    val selected = ids.firstOrNull { it.value == selectedName } ?: ids.first()
    val style = config.homeCards.style(selected, editingDark)
    var tint by remember(selected, editingDark, style.tint) { mutableFloatStateOf(style.tint) }
    var colorRole by rememberSaveable { mutableStateOf<String?>(null) }
    var cropOpen by rememberSaveable { mutableStateOf(false) }
    var confirmAction by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImageCard by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImageDark by rememberSaveable { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }

    fun updateCards(cards: SidebarHomeCards) {
        latestUpdate(latestConfig.copy(homeCards = cards))
    }
    fun updateStyle(next: SidebarHomeCardStyle) {
        updateCards(latestConfig.homeCards.withStyle(selected, editingDark, next))
    }
    fun releaseUnused(uri: String?, cards: SidebarHomeCards) {
        if (uri == null || cards.overrides.values.any { it.imageUri == uri } || latestConfig.imageUriString == uri) return
        scope.launch(NonCancellable + Dispatchers.IO) { releaseCustomImageReference(context, uri) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = SidebarHomeCardId.entries.firstOrNull { it.value == pendingImageCard }
        val dark = pendingImageDark
        pendingImageCard = null
        if (uri != null && target != null) scope.launch {
            importing = true
            imageError = false
            var imported: String? = null
            var committed = false
            try {
                withContext(Dispatchers.IO) {
                    imported = persistCustomImageReference(context, uri, "sidebar_home_${target.value}_$dark", 12L * 1024 * 1024)
                    val bitmap = imported?.let { loadCustomImageBitmap(context, it, maxSide = 512) }
                    check(bitmap != null)
                    bitmap.recycle()
                }
                val previous = latestConfig.homeCards.style(target, dark)
                val cards = latestConfig.homeCards.withStyle(target, dark, previous.copy(
                    imageUri = imported, crop = FULL_CUSTOM_WALLPAPER_CROP, inheritImage = false,
                ))
                updateCards(cards)
                committed = true
                releaseUnused(previous.imageUri, cards)
                if (selected == target && editingDark == dark) cropOpen = true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                imageError = true
            } finally {
                if (!committed) withContext(NonCancellable + Dispatchers.IO) {
                    imported?.let { releaseCustomImageReference(context, it) }
                }
                importing = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.sidebar_home_cards_scope), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.sidebar_home_cards_edit_layout), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SidebarHomeLayout.entries.forEach { item ->
                FilterChip(selected = layout == item, onClick = {
                    layoutName = item.value
                    selectedName = sidebarHomeCardIds(item).first().value
                    imageError = false
                }, enabled = !importing, label = { Text(stringResource(item.cardLayoutLabel())) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        Text(stringResource(R.string.sidebar_home_cards_edit_card), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ids.forEach { id ->
                FilterChip(selected = selected == id, onClick = { selectedName = id.value; imageError = false }, enabled = !importing,
                    label = { Text(stringResource(id.cardLabel())) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        Text(stringResource(R.string.sidebar_palette_mode), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false, true).forEach { dark ->
                FilterChip(selected = editingDark == dark, onClick = { editingDark = dark; imageError = false }, enabled = !importing,
                    label = { Text(stringResource(if (dark) R.string.sidebar_palette_dark else R.string.sidebar_palette_light)) },
                    modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        HorizontalDivider()
        // Preview the selected theme, never a captured frame from the settings page.
        CompositionLocalProvider(LocalSidebarGlassBackdrop provides null) {
            MaterialTheme(colorScheme = if (editingDark) darkColorScheme() else lightColorScheme()) {
                SidebarHomeCardSurface(id = selected, styleOverride = style.copy(tint = tint), modifier = Modifier.fillMaxWidth()) { ink, _ ->
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(selected.cardLabel()), color = ink, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.sidebar_home_cards_preview), color = ink, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.sidebar_home_cards_image), style = MaterialTheme.typography.titleSmall)
        if (importing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.sidebar_home_cards_importing))
        }
        if (imageError) Text(stringResource(R.string.sidebar_home_cards_image_error), color = MaterialTheme.colorScheme.error)
        Text(stringResource(when {
            style.imageUri != null -> R.string.sidebar_home_cards_image_selected
            style.inheritImage -> R.string.sidebar_home_cards_image_inherited
            else -> R.string.sidebar_home_cards_image_none
        }), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                pendingImageCard = selected.value
                pendingImageDark = editingDark
                runCatching { picker.launch(arrayOf("image/*")) }.onFailure { pendingImageCard = null; imageError = true }
            }, enabled = !importing, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sidebar_home_cards_pick)) }
            if (style.imageUri != null) OutlinedButton(onClick = { cropOpen = true }, enabled = !importing,
                modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings_wallpaper_crop)) }
            if (style.imageUri != null || style.inheritImage) TextButton(onClick = {
                val cards = latestConfig.homeCards.withStyle(selected, editingDark, style.copy(imageUri = null, inheritImage = false))
                updateCards(cards)
                releaseUnused(style.imageUri, cards)
            }, enabled = !importing, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sidebar_home_cards_clear_image)) }
            if (!style.inheritImage || style.imageUri != null) TextButton(onClick = {
                val cards = latestConfig.homeCards.withStyle(selected, editingDark, style.copy(imageUri = null, inheritImage = true))
                updateCards(cards)
                releaseUnused(style.imageUri, cards)
            }, enabled = !importing, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sidebar_home_cards_inherit)) }
        }
        HorizontalDivider()
        Text(stringResource(R.string.sidebar_home_cards_material), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SidebarCardMaterial.entries.forEach { material ->
                FilterChip(selected = style.material == material, onClick = { updateStyle(style.copy(material = material)) },
                    label = { Text(stringResource(material.cardMaterialLabel())) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        if (style.material != SidebarCardMaterial.Default) {
            Text(stringResource(R.string.sidebar_home_cards_gradient), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SidebarCardGradient.entries.forEach { gradient ->
                    FilterChip(selected = style.gradient == gradient, onClick = { updateStyle(style.copy(gradient = gradient)) },
                        label = { Text(stringResource(gradient.cardGradientLabel())) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            val tintLabel = stringResource(R.string.sidebar_home_cards_tint, (tint * 100).roundToInt())
            val tintState = remember { SliderState(tint, trackRange = 0.15f..1f) }
            tintState.value = tint
            Text(tintLabel, style = MaterialTheme.typography.bodyMedium)
            Slider(state = tintState, onValueChange = { tint = it }, onValueChangeFinished = { updateStyle(style.copy(tint = tint)) },
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = tintLabel })
        }
        if (style.material != SidebarCardMaterial.Default || style.imageUri != null || !style.inheritImage) {
            Text(stringResource(R.string.sidebar_palette_colors_title), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardColorRole.entries.filter { role ->
                    when {
                        style.material == SidebarCardMaterial.Default -> role == CardColorRole.Content
                        style.gradient == SidebarCardGradient.Solid -> role != CardColorRole.Middle && role != CardColorRole.End
                        else -> true
                    }
                }.forEach { role ->
                    OutlinedButton(onClick = { colorRole = role.name }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(18.dp).background(Color(role.color(style).toInt()), MaterialTheme.shapes.extraSmall))
                            Text(stringResource(role.label))
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.sidebar_home_cards_readability), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { confirmAction = "apply" }, enabled = !importing, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.sidebar_home_cards_apply_all))
            }
            TextButton(onClick = { confirmAction = "reset" }, enabled = !importing, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.sidebar_home_cards_reset))
            }
        }
    }
    SettingsWallpaperCropDialog(show = cropOpen && style.imageUri != null, uriString = style.imageUri, crop = style.crop,
        onCropChange = { updateStyle(style.copy(crop = it)) }, onDismissRequest = { cropOpen = false },
        editorAspectRatio = 1.8f, defaultCrop = FULL_CUSTOM_WALLPAPER_CROP, allowTransforms = false)
    CardColorRole.entries.firstOrNull { it.name == colorRole }?.let { role ->
        SidebarCardColorDialog(role = role, style = style, onApply = { color -> updateStyle(role.update(style, color)); colorRole = null },
            onDismiss = { colorRole = null })
    }
    if (confirmAction != null) AlertDialog(onDismissRequest = { confirmAction = null },
        title = { Text(stringResource(if (confirmAction == "apply") R.string.sidebar_home_cards_apply_all else R.string.sidebar_home_cards_reset)) },
        text = { Text(stringResource(if (confirmAction == "apply") R.string.sidebar_home_cards_apply_confirm else R.string.sidebar_home_cards_reset_confirm)) },
        confirmButton = { TextButton(onClick = {
            val cards = if (confirmAction == "apply") latestConfig.homeCards.applyFinish(layout, editingDark, style)
                else latestConfig.homeCards.reset(selected, editingDark)
            updateCards(cards)
            releaseUnused(style.imageUri, cards)
            confirmAction = null
        }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = { confirmAction = null }) { Text(stringResource(android.R.string.cancel)) } })
}

private enum class CardColorRole(@StringRes val label: Int) {
    Start(R.string.sidebar_home_cards_color_start), Middle(R.string.sidebar_home_cards_color_middle), End(R.string.sidebar_home_cards_color_end),
    Content(R.string.sidebar_palette_content), Highlight(R.string.sidebar_palette_highlight);

    fun color(style: SidebarHomeCardStyle): Long = when (this) {
        Start -> style.start; Middle -> style.middle; End -> style.end; Content -> style.content; Highlight -> style.highlight
    }
    fun update(style: SidebarHomeCardStyle, color: Long): SidebarHomeCardStyle = when (this) {
        Start -> style.copy(start = color); Middle -> style.copy(middle = color); End -> style.copy(end = color)
        Content -> style.copy(content = color); Highlight -> style.copy(highlight = color)
    }
}

@Composable
private fun SidebarCardColorDialog(role: CardColorRole, style: SidebarHomeCardStyle, onApply: (Long) -> Unit, onDismiss: () -> Unit) {
    var color by rememberSaveable(role, style) { mutableStateOf(role.color(style)) }
    var hex by rememberSaveable(role, style) { mutableStateOf(formatSidebarRgb(color)) }
    val parsed = parseSidebarRgb(hex)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(role.label)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = hex, onValueChange = { value -> hex = value.take(7); parseSidebarRgb(hex)?.let { color = it } },
                singleLine = true, label = { Text(stringResource(R.string.sidebar_home_cards_hex)) },
                supportingText = { Text(stringResource(if (parsed == null) R.string.sidebar_palette_invalid else R.string.sidebar_palette_hex)) },
                isError = parsed == null, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                leadingIcon = { Box(Modifier.size(24.dp).background(Color(color.toInt()))) }, modifier = Modifier.fillMaxWidth())
            listOf(16 to R.string.sidebar_palette_red, 8 to R.string.sidebar_palette_green, 0 to R.string.sidebar_palette_blue).forEach { (shift, label) ->
                val name = stringResource(label)
                val value = ((color shr shift) and 0xFF).toInt()
                val channelState = remember { SliderState(value.toFloat(), trackRange = 0f..255f) }
                channelState.value = value.toFloat()
                Text("$name  $value", style = MaterialTheme.typography.labelLarge)
                Slider(state = channelState, onValueChange = { next ->
                    color = (color and (0xFFL shl shift).inv()) or (next.roundToInt().toLong() shl shift)
                    hex = formatSidebarRgb(color)
                }, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = name })
            }
        }
    }, confirmButton = { Button(onClick = { parsed?.let(onApply) }, enabled = parsed != null) { Text(stringResource(R.string.sidebar_home_cards_apply_color)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } })
}

@StringRes private fun SidebarHomeLayout.cardLayoutLabel(): Int = when (this) {
    SidebarHomeLayout.Material -> R.string.sidebar_home_layout_material
    SidebarHomeLayout.StatusCards -> R.string.sidebar_home_layout_cards
}
@StringRes private fun SidebarHomeCardId.cardLabel(): Int = when (this) {
    SidebarHomeCardId.MaterialStatus -> R.string.sidebar_home_card_material_status
    SidebarHomeCardId.Superuser -> R.string.sidebar_home_card_superuser
    SidebarHomeCardId.Module -> R.string.sidebar_home_card_module
    SidebarHomeCardId.MaterialSecurity -> R.string.sidebar_home_card_material_security
    SidebarHomeCardId.MaterialInfo -> R.string.sidebar_home_card_material_info
    SidebarHomeCardId.StatusHero -> R.string.sidebar_home_card_status_hero
    SidebarHomeCardId.StatusMetrics -> R.string.sidebar_home_card_status_metrics
    SidebarHomeCardId.StatusSecurity -> R.string.sidebar_home_card_status_security
    SidebarHomeCardId.StatusVersions -> R.string.sidebar_home_card_status_versions
    SidebarHomeCardId.Support -> R.string.sidebar_home_card_support
    SidebarHomeCardId.Learn -> R.string.sidebar_home_card_learn
}
@StringRes private fun SidebarCardMaterial.cardMaterialLabel(): Int = when (this) {
    SidebarCardMaterial.Default -> R.string.sidebar_home_cards_default
    SidebarCardMaterial.LiquidGlass -> R.string.sidebar_material_liquid
    SidebarCardMaterial.NeumorphicGlass -> R.string.sidebar_material_neumorphic
}
@StringRes private fun SidebarCardGradient.cardGradientLabel(): Int = when (this) {
    SidebarCardGradient.Solid -> R.string.sidebar_home_cards_solid
    SidebarCardGradient.Vertical -> R.string.sidebar_home_cards_vertical
    SidebarCardGradient.Horizontal -> R.string.sidebar_home_cards_horizontal
    SidebarCardGradient.Diagonal -> R.string.sidebar_home_cards_diagonal
    SidebarCardGradient.ReverseDiagonal -> R.string.sidebar_home_cards_reverse_diagonal
    SidebarCardGradient.Radial -> R.string.sidebar_home_cards_radial
    SidebarCardGradient.Sweep -> R.string.sidebar_home_cards_sweep
}
