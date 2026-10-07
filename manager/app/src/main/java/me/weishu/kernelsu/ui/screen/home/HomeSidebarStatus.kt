package me.weishu.kernelsu.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.SidebarUiTokens

private data class SidebarHomeColors(
    val card: Color,
    val text: Color,
    val secondaryText: Color,
    val emblem: Color,
    val emblemContent: Color,
)

@Composable
private fun sidebarHomeColors(): SidebarHomeColors = with(MaterialTheme.colorScheme) {
    SidebarHomeColors(
        card = surfaceContainer,
        text = onSurface,
        secondaryText = onSurfaceVariant,
        emblem = inverseSurface,
        emblemContent = inverseOnSurface,
    )
}

internal fun sidebarHomeMetricValue(available: Boolean, count: Int): String =
    if (available && count >= 0) count.toString() else "—"

@Composable
internal fun SidebarStatusCards(state: HomeUiState, actions: HomeActions) {
    val colors = sidebarHomeColors()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 640.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SidebarStatusHero(state, actions, colors)
                    SidebarMetricsCard(state, actions, colors)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SidebarSecurityCard(state.systemInfo, colors)
                    SidebarVersionsCard(state.systemInfo, colors)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SidebarStatusHero(state, actions, colors)
                SidebarMetricsCard(state, actions, colors)
                SidebarSecurityCard(state.systemInfo, colors)
                SidebarVersionsCard(state.systemInfo, colors)
            }
        }
    }
}

@Composable
private fun SidebarHomeCard(
    colors: SidebarHomeColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.card,
        contentColor = colors.text,
    ) {
        Box(Modifier.padding(SidebarUiTokens.PagePadding)) { content() }
    }
}

@Composable
private fun SidebarStatusHero(state: HomeUiState, actions: HomeActions, colors: SidebarHomeColors) {
    val installed = state.isKernelActive
    val supported = state.kernelVersion.isGKI()
    val title = when {
        installed && !state.isFullFeatured -> stringResource(state.rootRuntimeState.labelRes)
        installed -> stringResource(R.string.home_working)
        supported -> stringResource(R.string.home_not_installed)
        else -> stringResource(R.string.home_unsupported)
    }
    val summary = when {
        installed -> stringResource(R.string.home_working_version, state.ksuVersionLabel)
        supported -> stringResource(R.string.home_click_to_install)
        else -> stringResource(R.string.home_unsupported_reason)
    }
    val icon = when {
        installed && state.isFullFeatured -> Icons.Outlined.CheckCircle
        supported || installed -> Icons.Outlined.Warning
        else -> Icons.Outlined.Block
    }
    SidebarHomeCard(
        colors = colors,
        modifier = Modifier.clickable(enabled = !state.isLateLoadMode) { actions.onInstallClick() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.workingModeLabel?.let { mode ->
                    Text(
                        text = mode,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text,
                        maxLines = 1,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = CircleShape, color = colors.emblem, contentColor = colors.emblemContent) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = colors.secondaryText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (installed && (state.isSafeMode || state.isLateLoadMode)) {
                Text(
                    text = stringResource(if (state.isSafeMode) R.string.safe_mode else R.string.jailbreak_mode),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondaryText,
                )
            }
            if (!installed && supported && state.isSELinuxPermissive) {
                Button(
                    onClick = actions.onJailbreakClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emblem,
                        contentColor = colors.emblemContent,
                    ),
                ) {
                    Text(stringResource(R.string.home_jailbreak))
                }
            }
        }
    }
}

@Composable
private fun SidebarMetricsCard(state: HomeUiState, actions: HomeActions, colors: SidebarHomeColors) {
    SidebarHomeCard(colors) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SidebarMetricRow(
                label = stringResource(R.string.superuser),
                value = sidebarHomeMetricValue(state.isFullFeatured, state.superuserCount),
                enabled = state.isFullFeatured,
                onClick = actions.onSuperuserClick,
                colors = colors,
            )
            SidebarMetricRow(
                label = stringResource(R.string.module),
                value = sidebarHomeMetricValue(state.isFullFeatured, state.moduleCount),
                enabled = state.isFullFeatured,
                onClick = actions.onModuleClick,
                colors = colors,
            )
        }
    }
}

@Composable
private fun SidebarMetricRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    colors: SidebarHomeColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = colors.text)
    }
}

@Composable
private fun SidebarSecurityCard(systemInfo: SystemInfo, colors: SidebarHomeColors) {
    var fingerprintExpanded by rememberSaveable { mutableStateOf(false) }
    val selinux = when (systemInfo.selinuxStatus) {
        "Enforcing" -> R.string.selinux_status_enforcing
        "Permissive" -> R.string.selinux_status_permissive
        "Disabled" -> R.string.selinux_status_disabled
        else -> R.string.selinux_status_unknown
    }
    val seccomp = when (systemInfo.seccompStatus) {
        -1 -> R.string.seccomp_status_not_supported
        0 -> R.string.seccomp_status_disabled
        1 -> R.string.seccomp_status_strict
        2 -> R.string.seccomp_status_filter
        else -> R.string.seccomp_status_unknown
    }
    SidebarHomeCard(colors) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SidebarDataRow(stringResource(R.string.home_selinux_status), stringResource(selinux), colors)
            SidebarDataRow(stringResource(R.string.home_seccomp_status), stringResource(seccomp), colors)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.home_fingerprint),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                )
                Text(
                    systemInfo.fingerprint.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.secondaryText,
                    maxLines = if (fingerprintExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (systemInfo.fingerprint.length > 45) {
                    TextButton(onClick = { fingerprintExpanded = !fingerprintExpanded }) {
                        Text(
                            stringResource(
                                if (fingerprintExpanded) R.string.home_collapse_fingerprint
                                else R.string.home_expand_fingerprint
                            ),
                            color = colors.text,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarVersionsCard(systemInfo: SystemInfo, colors: SidebarHomeColors) {
    SidebarHomeCard(colors) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SidebarDataRow(stringResource(R.string.home_manager_version), systemInfo.managerVersion, colors)
            SidebarDataRow(stringResource(R.string.home_kernel), systemInfo.kernelVersion, colors)
            SidebarDataRow(stringResource(R.string.home_device_model), systemInfo.deviceModel, colors)
            if (systemInfo.kpm.isNotBlank()) {
                SidebarDataRow(stringResource(R.string.home_kpm), systemInfo.kpm, colors)
            }
            if (systemInfo.susfs.isNotBlank()) {
                SidebarDataRow(stringResource(R.string.home_susfs), systemInfo.susfs, colors)
            }
        }
    }
}

@Composable
private fun SidebarDataRow(label: String, value: String, colors: SidebarHomeColors) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.text)
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.secondaryText,
        )
    }
}
