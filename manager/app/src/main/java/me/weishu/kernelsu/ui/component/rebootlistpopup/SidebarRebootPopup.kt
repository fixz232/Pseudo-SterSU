package me.weishu.kernelsu.ui.component.rebootlistpopup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.bottombar.SidebarPaneShape

@Composable
internal fun SidebarRebootPopup() {
    KsuIsValid {
        var expanded by remember { mutableStateOf(false) }
        val onReboot = rememberRebootAction()
        val options = getRebootListOption()
        val screen = LocalConfiguration.current
        val menuWidth = (screen.screenWidthDp - 24).coerceIn(160, 288).dp
        val columns = if (menuWidth >= 240.dp && LocalDensity.current.fontScale <= 1.3f) 3 else 2
        Box {
            Box(
                modifier = Modifier.size(52.dp).clip(SidebarPaneShape)
                    .clickable(role = Role.Button) { expanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PowerSettingsNew, stringResource(R.string.reboot), Modifier.size(26.dp))
            }
            DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Column(
                        modifier = Modifier.width(menuWidth)
                            .heightIn(max = (screen.screenHeightDp - 40).coerceAtLeast(120).dp)
                            .verticalScroll(rememberScrollState()).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.reboot),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        options.chunked(columns).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { option ->
                                    val label = stringResource(option.labelRes)
                                    val shortLabel = when (option.reason) {
                                        "recovery" -> "Recovery"
                                        "bootloader" -> "Bootloader"
                                        "download" -> "Download"
                                        "edl" -> "EDL"
                                        else -> label
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable(role = Role.Button) {
                                                expanded = false
                                                onReboot(option.reason)
                                            }
                                            .semantics(mergeDescendants = true) { contentDescription = label }
                                            .heightIn(min = 80.dp).padding(horizontal = 2.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(36.dp))
                                            Text(
                                                when (option.reason) {
                                                    "soft_reboot" -> "S"
                                                    "userspace" -> "U"
                                                    "recovery" -> "R"
                                                    "bootloader" -> "B"
                                                    "download" -> "D"
                                                    "edl" -> "E"
                                                    else -> ""
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                            )
                                        }
                                        Text(shortLabel, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
