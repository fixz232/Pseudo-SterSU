package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.core.tasks.KernelIdentity
import me.weishu.kernelsu.ui.util.KernelBootSlot
import me.weishu.kernelsu.ui.viewmodel.SusfsKernelSlotsState

@Composable
internal fun SusfsKernelSlotPicker(
    state: SusfsKernelSlotsState,
    queryEnabled: Boolean,
    fillEnabled: Boolean,
    onRefresh: () -> Unit,
    onFill: (KernelIdentity) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.susfs_slots_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onRefresh, enabled = queryEnabled && !state.loading) {
                Icon(Icons.Rounded.Refresh, stringResource(R.string.susfs_slots_refresh))
            }
        }
        Text(stringResource(R.string.susfs_slots_summary), style = MaterialTheme.typography.bodySmall)
        if (state.loading) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.susfs_slots_loading), style = MaterialTheme.typography.bodyMedium)
            }
        } else if (state.result.error.isNotEmpty()) {
            KernelSlotError(state.result.error)
        } else if (state.loaded) {
            val singleSlot = state.result.slots.singleOrNull()?.slot == KernelBootSlot.Single
            Text(
                when {
                    singleSlot -> stringResource(R.string.susfs_slots_single_device)
                    state.result.currentSlot != null -> stringResource(
                        R.string.susfs_slots_current,
                        state.result.currentSlot.name,
                    )
                    else -> stringResource(R.string.susfs_slots_current_unknown)
                },
                style = MaterialTheme.typography.bodySmall,
            )
            state.result.slots.forEach { slot ->
                HorizontalDivider()
                val label = if (slot.slot == KernelBootSlot.Single) {
                    stringResource(R.string.susfs_slots_single)
                } else {
                    stringResource(R.string.susfs_slots_slot, slot.slot.name)
                }
                Text(label, fontWeight = FontWeight.SemiBold)
                if (slot.partition.isNotEmpty()) {
                    Text(slot.partition, style = MaterialTheme.typography.bodySmall)
                }
                val identity = slot.identity
                if (identity != null) {
                    Text(stringResource(R.string.susfs_slots_release, identity.release), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.susfs_slots_time, identity.buildTime ?: stringResource(R.string.susfs_slots_time_unknown)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(stringResource(R.string.susfs_slots_version, identity.version), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { onFill(identity) },
                        enabled = fillEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.susfs_slots_fill, label)) }
                } else {
                    KernelSlotError(slot.error)
                }
            }
            if (!fillEnabled && state.result.slots.any { it.identity != null }) {
                Text(stringResource(R.string.susfs_slots_fill_unavailable), style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(stringResource(R.string.susfs_slots_not_loaded), style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
    }
}

@Composable
private fun KernelSlotError(error: String) {
    val message = when (error.substringBefore(':')) {
        "root_unavailable" -> R.string.susfs_slots_error_root
        "slot_missing" -> R.string.susfs_slots_error_missing
        "slot_alias_conflict" -> R.string.susfs_slots_error_alias
        "invalid_boot_header" -> R.string.susfs_slots_error_header
        "kernel_size_limit" -> R.string.susfs_slots_error_size
        "unsupported_kernel_compression" -> R.string.susfs_slots_error_compression
        "ambiguous_kernel_banner" -> R.string.susfs_slots_error_ambiguous
        "kernel_banner_unavailable" -> R.string.susfs_slots_error_banner
        "read_timeout" -> R.string.susfs_slots_error_timeout
        "cache_space_low" -> R.string.susfs_slots_error_space
        "truncated_kernel", "invalid_lz4_block" -> R.string.susfs_slots_error_truncated
        else -> R.string.susfs_slots_error_read
    }
    Text(stringResource(message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    if (message == R.string.susfs_slots_error_read && error.isNotBlank()) {
        Text(error, style = MaterialTheme.typography.bodySmall)
    }
}
