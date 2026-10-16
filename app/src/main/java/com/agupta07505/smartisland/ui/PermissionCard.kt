/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.ui.components.FilledActionButton
import com.agupta07505.smartisland.ui.components.StatusKind
import com.agupta07505.smartisland.ui.components.StatusLabel
import com.agupta07505.smartisland.ui.components.TintedActionButton

/**
 * One permission: name, status (icon + text, never color alone), explanation, and a single action.
 * Meant to live inside a [com.agupta07505.smartisland.ui.components.GroupedSection].
 */
@Composable
fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.RowHorizontal, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatusLabel(
                text = if (granted) stringResource(R.string.perm_status_granted) else stringResource(R.string.perm_status_action_required),
                kind = if (granted) StatusKind.Success else StatusKind.Warning
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (granted) {
            TintedActionButton(text = stringResource(R.string.btn_open), onClick = onClick)
        } else {
            FilledActionButton(text = buttonText, onClick = onClick)
        }
    }
}
