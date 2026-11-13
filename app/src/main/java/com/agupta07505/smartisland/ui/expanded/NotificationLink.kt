/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.CardLinks
import com.agupta07505.smartisland.model.LinkKind
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.LinkHandler
import com.agupta07505.smartisland.util.LinkOpener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.agupta07505.smartisland.ui.IslandText as Text

private const val LINK_ICON_PX = 64

/**
 * Open link, Send email, Call and the like, with the icon of the app that will open it when Android names one.
 * Offered instead of the suggested replies when a message holds exactly 1 link
 * ([com.agupta07505.smartisland.model.CardLinks.forCard])
 */
@Composable
internal fun OpenLinkChip(link: String, handler: LinkHandler?, onOpened: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, handler?.packageName) {
        value = handler?.let {
            withContext(Dispatchers.Default) {
                LinkOpener.icon(context, it)?.toBitmap(LINK_ICON_PX, LINK_ICON_PX)?.asImageBitmap()
            }
        }
    }
    val label = when (CardLinks.kind(link)) {
        LinkKind.WEB -> R.string.link_open
        LinkKind.EMAIL -> R.string.link_email
        LinkKind.PHONE -> R.string.link_call
        LinkKind.SMS -> R.string.link_sms
        LinkKind.APP -> R.string.link_open_app
    }
    Row(
        modifier = Modifier
            .height(NOTIFICATION_CHIP_HEIGHT)
            .clip(RoundedCornerShape(15.dp))
            .background(LocalIslandPalette.current.smartChipFill)
            .bounceClick {
                if (LinkOpener.open(context, link, handler)) {
                    onOpened()
                } else {
                    Toast.makeText(context, R.string.link_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
            .padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape)) }
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(label),
            color = LocalIslandPalette.current.smartChipText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
