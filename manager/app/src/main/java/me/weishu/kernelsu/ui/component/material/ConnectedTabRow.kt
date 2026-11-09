package me.weishu.kernelsu.ui.component.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor

/** A single frame and shared dividers keep the tabs connected without doubled borders. */
@Composable
internal fun ConnectedTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val selected = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    val outline = colors.outlineVariant.copy(alpha = 0.85f)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).height(IntrinsicSize.Min)
            .clip(shape).background(immersiveSurfaceColor(colors.surfaceContainerHigh))
            .border(1.dp, outline, shape).selectableGroup(),
    ) {
        tabs.forEachIndexed { index, label ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(outline))
            val active = index == selected
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (active) colors.primaryContainer else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onTabSelected(index) })
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, style = MaterialTheme.typography.labelLarge,
                    color = if (active) colors.onPrimaryContainer else colors.onSurface,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
