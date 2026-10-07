package me.weishu.kernelsu.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.sidebarColors
import me.weishu.kernelsu.ui.component.bottombar.sidebarMinimumContrast
import me.weishu.kernelsu.ui.util.SidebarMaterial
import me.weishu.kernelsu.ui.util.SidebarPalette
import me.weishu.kernelsu.ui.util.SidebarWidgetConfig
import me.weishu.kernelsu.ui.util.formatSidebarRgb
import me.weishu.kernelsu.ui.util.parseSidebarRgb
import kotlin.math.roundToInt

private enum class PaletteRole(@StringRes val label: Int) {
    Background(R.string.sidebar_palette_background),
    BackgroundEnd(R.string.sidebar_palette_background_end),
    Selection(R.string.sidebar_palette_selection),
    Content(R.string.sidebar_palette_content),
    Highlight(R.string.sidebar_palette_highlight),
    Shade(R.string.sidebar_palette_shade);

    fun color(palette: SidebarPalette): Long = when (this) {
        Background -> palette.background
        BackgroundEnd -> palette.backgroundEnd
        Selection -> palette.selection
        Content -> palette.content
        Highlight -> palette.highlight
        Shade -> palette.shade
    }

    fun update(palette: SidebarPalette, color: Long): SidebarPalette = when (this) {
        Background -> palette.copy(background = color)
        BackgroundEnd -> palette.copy(backgroundEnd = color)
        Selection -> palette.copy(selection = color)
        Content -> palette.copy(content = color)
        Highlight -> palette.copy(highlight = color)
        Shade -> palette.copy(shade = color)
    }
}

private val colorPresets = listOf(
    0xFF161616L, 0xFF303030L, 0xFF606060L, 0xFFBDBDBDL, 0xFFEAEAEAL,
    0xFFFFFFFFL, 0xFF4E6D8AL, 0xFF7B5E8AL, 0xFF547661L, 0xFF996A54L,
)

@Composable
internal fun SidebarMaterialOptions(
    config: SidebarWidgetConfig,
    editingDark: Boolean,
    onEditingDarkChange: (Boolean) -> Unit,
    onUpdate: (SidebarWidgetConfig) -> Unit,
) {
    val material = config.material
    val saved = config.palettes.palette(material, editingDark)
    val roles = PaletteRole.entries.filter {
        when (it) {
            PaletteRole.Highlight -> material != SidebarMaterial.Flat
            PaletteRole.Shade -> material == SidebarMaterial.NeumorphicGlass
            else -> true
        }
    }
    var roleName by rememberSaveable { mutableStateOf(PaletteRole.Background.name) }
    val role = roles.firstOrNull { it.name == roleName } ?: PaletteRole.Background
    val savedColor = role.color(saved)
    var hex by rememberSaveable(material, editingDark, role, savedColor) { mutableStateOf(formatSidebarRgb(savedColor)) }
    var pendingColor by remember(material, editingDark, role, savedColor) { mutableStateOf(savedColor) }
    var showRgb by rememberSaveable { mutableStateOf(false) }
    val previewPalette = role.update(saved, pendingColor)
    val contrast = sidebarMinimumContrast(sidebarColors(previewPalette), material != SidebarMaterial.Flat)

    fun applyColor(color: Long) {
        val next = role.update(saved, color)
        if (next != saved) onUpdate(config.copy(palettes = config.palettes.withPalette(material, editingDark, next)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SidebarMaterial.entries.forEach { option ->
                FilterChip(
                    selected = material == option,
                    onClick = { onUpdate(config.copy(material = option, glassEnabled = option != SidebarMaterial.Flat)) },
                    label = { Text(stringResource(option.labelRes())) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(
            stringResource(when (material) {
                SidebarMaterial.LiquidGlass -> R.string.sidebar_material_liquid_summary
                SidebarMaterial.NeumorphicGlass -> R.string.sidebar_material_neumorphic_summary
                SidebarMaterial.Flat -> R.string.sidebar_material_flat_summary
            }),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider()
        Text(stringResource(R.string.sidebar_palette_mode), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false, true).forEach { dark ->
                FilterChip(
                    selected = editingDark == dark,
                    onClick = { onEditingDarkChange(dark) },
                    label = { Text(stringResource(if (dark) R.string.sidebar_palette_dark else R.string.sidebar_palette_light)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(stringResource(R.string.sidebar_palette_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text(stringResource(R.string.sidebar_palette_colors_title), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            roles.forEach { item ->
                FilterChip(
                    selected = item == role,
                    onClick = { roleName = item.name },
                    leadingIcon = {
                        Box(Modifier.size(16.dp).background(Color(item.color(saved).toInt()), RoundedCornerShape(4.dp)))
                    },
                    label = { Text(stringResource(item.label)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        OutlinedTextField(
            value = hex,
            onValueChange = { next ->
                hex = next.take(7)
                parseSidebarRgb(hex)?.let { color ->
                    pendingColor = color
                    applyColor(color)
                }
            },
            label = { Text(stringResource(role.label)) },
            supportingText = { Text(stringResource(if (parseSidebarRgb(hex) == null) R.string.sidebar_palette_invalid else R.string.sidebar_palette_hex)) },
            isError = parseSidebarRgb(hex) == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                autoCorrectEnabled = false,
            ),
            leadingIcon = { Box(Modifier.size(24.dp).background(Color(pendingColor.toInt()), RoundedCornerShape(6.dp))) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.sidebar_palette_presets), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            colorPresets.forEach { color ->
                val colorLabel = formatSidebarRgb(color)
                IconButton(
                    onClick = { pendingColor = color; hex = colorLabel; applyColor(color) },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = colorLabel },
                ) {
                    Box(
                        Modifier.size(30.dp)
                            .background(Color(color.toInt()), CircleShape)
                            .border(2.dp, if (color == pendingColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                }
            }
        }
        TextButton(onClick = { showRgb = !showRgb }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(if (showRgb) R.string.sidebar_palette_rgb_hide else R.string.sidebar_palette_rgb_show))
        }
        if (showRgb) {
            listOf(16 to R.string.sidebar_palette_red, 8 to R.string.sidebar_palette_green, 0 to R.string.sidebar_palette_blue).forEach { (shift, label) ->
                val name = stringResource(label)
                val value = ((pendingColor shr shift) and 0xFF).toInt()
                val channelState = remember { SliderState(value.toFloat(), trackRange = 0f..255f) }
                channelState.value = value.toFloat()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(name, style = MaterialTheme.typography.labelLarge)
                    Slider(
                        state = channelState,
                        onValueChange = { next ->
                            pendingColor = (pendingColor and (0xFFL shl shift).inv()) or (next.roundToInt().toLong() shl shift)
                            hex = formatSidebarRgb(pendingColor)
                        },
                        onValueChangeFinished = { applyColor(pendingColor) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = name },
                    )
                    Text(value.toString().padStart(3, ' '), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (material != SidebarMaterial.Flat) {
            HorizontalDivider()
            Text(stringResource(R.string.sidebar_palette_effects_title), style = MaterialTheme.typography.titleSmall)
            EffectSlider(R.string.sidebar_palette_tint, saved.tintAlpha, 0.75f..1f) { value ->
                onUpdate(config.copy(palettes = config.palettes.withPalette(material, editingDark, saved.copy(tintAlpha = value))))
            }
            EffectSlider(R.string.sidebar_palette_highlight_strength, saved.highlightStrength, 0f..1.5f) { value ->
                onUpdate(config.copy(palettes = config.palettes.withPalette(material, editingDark, saved.copy(highlightStrength = value))))
            }
            if (material == SidebarMaterial.NeumorphicGlass) {
                EffectSlider(R.string.sidebar_palette_relief_strength, saved.reliefStrength, 0f..1.5f) { value ->
                    onUpdate(config.copy(palettes = config.palettes.withPalette(material, editingDark, saved.copy(reliefStrength = value))))
                }
            }
        }
        if (contrast < 4.5f) {
            Text(stringResource(R.string.sidebar_palette_contrast), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                val black = previewPalette.copy(content = 0xFF161616)
                val white = previewPalette.copy(content = 0xFFF5F5F5)
                val best = if (sidebarMinimumContrast(sidebarColors(black), material != SidebarMaterial.Flat) >=
                    sidebarMinimumContrast(sidebarColors(white), material != SidebarMaterial.Flat)) black else white
                onUpdate(config.copy(palettes = config.palettes.withPalette(material, editingDark, best)))
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.sidebar_palette_improve_text)) }
        }
        OutlinedButton(
            onClick = { onUpdate(config.copy(palettes = config.palettes.reset(material, editingDark))) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.sidebar_palette_reset)) }
    }
}

@Composable
private fun EffectSlider(
    @StringRes label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onCommit: (Float) -> Unit,
) {
    var pending by remember(value) { mutableStateOf(value) }
    val sliderState = remember(range) { SliderState(pending, trackRange = range) }
    sliderState.value = pending
    val name = stringResource(label)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text((pending * 100).roundToInt().toString() + "%", style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            state = sliderState,
            onValueChange = { pending = (it * 100).roundToInt() / 100f },
            onValueChangeFinished = { onCommit(pending) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = name },
        )
    }
}

@StringRes
private fun SidebarMaterial.labelRes(): Int = when (this) {
    SidebarMaterial.LiquidGlass -> R.string.sidebar_material_liquid
    SidebarMaterial.NeumorphicGlass -> R.string.sidebar_material_neumorphic
    SidebarMaterial.Flat -> R.string.sidebar_material_flat
}
