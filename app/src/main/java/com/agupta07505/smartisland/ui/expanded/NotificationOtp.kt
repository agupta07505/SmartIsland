/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.OtpChip
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.ui.IslandText as Text

/**
 * Copy code, offered in place of the Open link chip and the suggested replies on a one-time code message. It fires the
 * app's own copy button when the notification has one (shown with the app's own wording), otherwise it copies the code
 * the island found in the message
 */
@Composable
internal fun CopyCodeChip(notification: IslandNotification, chip: OtpChip, onCopied: () -> Unit) {
    val context = LocalContext.current
    val label = chip.action()?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.otp_copy)
    Row(
        modifier = Modifier
            .height(NOTIFICATION_CHIP_HEIGHT)
            .clip(RoundedCornerShape(15.dp))
            .background(LocalIslandPalette.current.smartChipFill)
            .bounceClick {
                val action = chip.action()
                val done = if (chip is OtpChip.Detected) {
                    copyCode(context, chip.code)
                } else {
                    triggerAction(
                        context, notification.packageName, action?.pendingIntent, action?.title.orEmpty(),
                        notification.contentIntent
                    )
                }
                if (done) onCopied() else Toast.makeText(context, R.string.otp_copy_failed, Toast.LENGTH_SHORT).show()
            }
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = null,
            tint = LocalIslandPalette.current.smartChipText,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = LocalIslandPalette.current.smartChipText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** The button the app or Android put on the notification, null when the island copies the code itself. */
private fun OtpChip.action(): IslandNotificationAction? = when (this) {
    is OtpChip.AppAction -> action
    is OtpChip.SystemAction -> action
    is OtpChip.Detected -> null
}

/** Puts the code on the clipboard, marked sensitive so the keyboard and the copy preview do not show it. */
private fun copyCode(context: Context, code: String): Boolean =
    runCatchingLogged("OtpCopy", "Code could not be copied") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("One-time code", code)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
        clipboard.setPrimaryClip(clip)
        // Android 13 and later confirm a copy themselves
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.otp_copied, Toast.LENGTH_SHORT).show()
        }
        true
    } ?: false
