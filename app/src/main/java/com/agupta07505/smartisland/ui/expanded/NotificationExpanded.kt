/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import com.agupta07505.smartisland.R
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.ui.IslandPalette
import android.app.ActivityOptions
import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.formatNotificationTimeOrNow
import com.agupta07505.smartisland.util.GatedAction
import com.agupta07505.smartisland.util.KeyguardGate
import com.agupta07505.smartisland.ui.LOCK_CROSSFADE_MS
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

@Composable
fun NotificationExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit,
    showActions: Boolean = true,
    settings: SmartIslandSettings = SmartIslandSettings.Default,
    onReplyStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    // Opens straight into the reply field when "Reply" was tapped on the lockscreen and the user then unlocked.
    var isReplying by remember(notification?.key) { mutableStateOf(KeyguardGate.consumeReplyRequest(notification?.key)) }
    var replyText by remember(notification?.key) { mutableStateOf("") }

    LaunchedEffect(isReplying) {
        onReplyStateChanged(isReplying)
        if (isReplying) {
            kotlinx.coroutines.delay(60)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    DisposableEffect(notification?.key) {
        onDispose {
            onReplyStateChanged(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: app icon, sender (with the count for a folded group) and when it arrived.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerIcon = notification?.icon ?: notification?.largeIcon
            if (headerIcon != null) {
                Image(
                    bitmap = headerIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(settings.notificationDotColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        notification?.appName?.firstOrNull()?.uppercase() ?: "S",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            val groupCount = notification?.groupCount ?: 1
            val headerText = when {
                notification == null -> "Notification"
                groupCount > 1 -> "${notification.appName} • $groupCount Messages"
                // A masked alert's header is the app, never the sender (Rule_Book §8.1).
                notification.isMasked -> notification.appName
                else -> notification.title.takeIf { it.isNotBlank() } ?: notification.appName
            }
            AnimatedContent(
                targetState = headerText to (notification?.isMasked == true),
                contentKey = { it.second },
                transitionSpec = { fadeIn(tween(LOCK_CROSSFADE_MS)) togetherWith fadeOut(tween(LOCK_CROSSFADE_MS)) },
                label = "notificationHeaderMask",
                modifier = Modifier.weight(1f)
            ) { (shown, _) ->
                Text(
                    text = shown,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = notification?.let { formatNotificationTimeOrNow(it.timeMillis) } ?: "",
                color = IslandPalette.secondaryLabel,
                fontSize = 12.sp
            )
        }

        // Body: the message in full, three lines at most. Tapping it opens the app (the page handles that).
        val bodyText = when {
            // Locked and private: only the fallback ("1 New Message"), crossfading to the text on unlock.
            notification?.isMasked == true -> notification.title
            else -> buildString {
                if (notification != null && notification.groupCount > 1 && notification.title.isNotBlank()) {
                    append(notification.title).append(": ")
                }
                append(notification?.text?.takeIf { it.isNotBlank() } ?: "New activity")
            }
        }
        AnimatedContent(
            targetState = bodyText to (notification?.isMasked == true),
            contentKey = { it.second },
            transitionSpec = { fadeIn(tween(LOCK_CROSSFADE_MS)) togetherWith fadeOut(tween(LOCK_CROSSFADE_MS)) },
            label = "notificationBodyMask"
        ) { (shown, _) ->
            Text(
                text = shown,
                color = Color.White.copy(alpha = 0.86f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
        }

        // Bottom Section: Inline Quick Reply OR Action Buttons
        if (isReplying && notification != null) {
            val quickReplyAction = notification.actionIntents.firstOrNull { it.isQuickReply }
                ?: notification.actionIntents.firstOrNull { it.title.lowercase().contains("reply") }

            val sendReplyAction: () -> Unit = {
                if (replyText.isNotBlank()) {
                    if (quickReplyAction?.pendingIntent != null) {
                        val intent = Intent()
                        val bundle = Bundle()
                        val key = quickReplyAction.remoteInputKey ?: "key_text_reply"
                        bundle.putCharSequence(key, replyText)
                        val remoteInput = RemoteInput.Builder(key).build()
                        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)

                        runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                val options = ActivityOptions.makeBasic()
                                    .setPendingIntentBackgroundActivityStartMode(
                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                    )
                                    .toBundle()
                                quickReplyAction.pendingIntent.send(context, 0, intent, null, null, null, options)
                            } else {
                                quickReplyAction.pendingIntent.send(context, 0, intent)
                            }
                        }
                    } else {
                        Toast.makeText(context, "Reply sent: $replyText", Toast.LENGTH_SHORT).show()
                    }
                    val repo = SmartIslandRepositories.notificationRepository(context)
                    notification.memberKeys.ifEmpty { listOf(notification.key) }.forEach { key ->
                        repo.removeNotification(key)
                        repo.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.CancelNotification(key))
                    }
                    isReplying = false
                    onReplyStateChanged(false)
                    keyboardController?.hide()
                    onCollapse()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(IslandPalette.fill)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { sendReplyAction() }
                        ),
                        decorationBox = { innerTextField ->
                            if (replyText.isEmpty()) {
                                Text(
                                    text = "Reply to ${notification.title.ifBlank { notification.appName }}...",
                                    color = IslandPalette.secondaryLabel,
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                // Send Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else IslandPalette.fill
                        )
                        .bounceClick {
                            sendReplyAction()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.a11y_send_reply),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Cancel Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IslandPalette.fill)
                        .bounceClick {
                            isReplying = false
                            onReplyStateChanged(false)
                            keyboardController?.hide()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.a11y_cancel),
                        tint = IslandPalette.secondaryLabel,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            val actions = notification?.actionIntents.orEmpty()
            val replyAction = actions.firstOrNull { it.isQuickReply || it.title.lowercase().contains("reply") }
            val markReadAction = actions.firstOrNull { it !== replyAction && it.title.lowercase().contains("read") }
            val otherActions = actions.filter { it !== replyAction && it !== markReadAction }
            val allKeys = notification?.memberKeys?.ifEmpty { listOf(notification.key) }.orEmpty()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Reply (icon + text; opens the inline field), then any other action the app offers.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (showActions && notification != null && replyAction != null) {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(IslandPalette.fill)
                                .bounceClick {
                                    // Locked: collapse, ask to unlock, and reopen the reply field only after it.
                                    if (KeyguardGate.isLocked(context)) {
                                        val replyKey = notification.key
                                        KeyguardGate.run(context, GatedAction.Reply, onCollapse) {
                                            KeyguardGate.requestReply(replyKey)
                                        }
                                    } else {
                                        isReplying = true
                                    }
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Reply,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Reply",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (showActions && notification != null) {
                        otherActions.take(1).forEach { action ->
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(IslandPalette.fill)
                                    .bounceClick {
                                        KeyguardGate.run(context, GatedAction.AppAction, onCollapse) {
                                            if (action.pendingIntent != null) {
                                                triggerAction(context, notification.packageName, action.pendingIntent, action.title, notification.contentIntent)
                                            } else {
                                                Toast.makeText(context, "Clicked: ${action.title}", Toast.LENGTH_SHORT).show()
                                            }
                                            val repo = SmartIslandRepositories.notificationRepository(context)
                                            allKeys.forEach { key ->
                                                repo.removeNotification(key)
                                                repo.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.CancelNotification(key))
                                            }
                                            onCollapse()
                                        }
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = action.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Right: Mark as read, icon only. Uses the app's own "mark as read" action when it has one,
                // then clears the alert (every message of a folded group) from the island and the shade.
                if (notification != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(IslandPalette.fill)
                            .bounceClick {
                                KeyguardGate.run(context, GatedAction.MarkAsRead, onCollapse) {
                                    if (markReadAction?.pendingIntent != null) {
                                        triggerAction(context, notification.packageName, markReadAction.pendingIntent, markReadAction.title, notification.contentIntent)
                                    }
                                    val repo = SmartIslandRepositories.notificationRepository(context)
                                    allKeys.forEach { key ->
                                        repo.removeNotification(key)
                                        repo.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.CancelNotification(key))
                                    }
                                    onCollapse()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = stringResource(R.string.a11y_mark_as_read),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
