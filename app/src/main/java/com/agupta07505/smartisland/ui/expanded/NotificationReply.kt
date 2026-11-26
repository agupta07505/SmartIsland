/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.app.ActivityOptions
import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.replyTarget
import com.agupta07505.smartisland.model.withSentReply
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.deliverReply
import com.agupta07505.smartisland.ui.IslandText as Text

/**
 * Hands [text] to the app's reply action, the same way for the reply box and for a suggested reply.
 * False when it could not be sent, the caller then keeps the draft and shows no sent reply
 */
private fun sendRemoteReply(
    context: android.content.Context,
    action: IslandNotificationAction,
    text: String,
    /** A tapped suggestion, not typed text: apps can tell the 2 apart from the results source. */
    fromChoice: Boolean = false
): Boolean {
    val intent = Intent()
    val bundle = Bundle()
    val key = action.remoteInputKey ?: "key_text_reply"
    bundle.putCharSequence(key, text)
    val remoteInput = RemoteInput.Builder(key).build()
    RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        RemoteInput.setResultsSource(
            intent,
            if (fromChoice) RemoteInput.SOURCE_CHOICE else RemoteInput.SOURCE_FREE_FORM_INPUT
        )
    }

    return action.deliverReply { pendingIntent ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                .toBundle()
            pendingIntent.send(context, 0, intent, null, null, null, options)
        } else {
            pendingIntent.send(context, 0, intent)
        }
    }
}

/** One-tap replies the app or Android's assistant suggests, a tap sends it like the reply box does. */
@Composable
internal fun SuggestedReplyChips(notification: IslandNotification) {
    val context = LocalContext.current
    val replyDrafts = LocalReplyDrafts.current
    val action = notification.actionIntents.replyTarget()
        ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        notification.suggestedReplies.forEach { reply ->
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(NOTIFICATION_CHIP_HEIGHT)
                    .clip(RoundedCornerShape(15.dp))
                    .background(LocalIslandPalette.current.smartChipFill)
                    .bounceClick {
                        if (sendRemoteReply(context, action, reply, fromChoice = true)) {
                            val repo = SmartIslandRepositories.notificationRepository(context)
                            replyDrafts.clear(notification.key)
                            // The app's re-post of that key must not close or reopen anything
                            repo.sendCommand(SmartIslandCommand.ReplySent(notification.key))
                            repo.updateNotification(notification.key) {
                                it.withSentReply(reply)
                            }
                        } else {
                            Toast.makeText(context, R.string.reply_unavailable, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reply,
                    color = LocalIslandPalette.current.smartChipText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun ReplyRow(
    notification: IslandNotification,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onReplyingChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    onReplyStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val replyDrafts = LocalReplyDrafts.current
    val sendReplyAction: () -> Unit = {
        if (replyText.isNotBlank()) {
            val target = notification.actionIntents.replyTarget()
            if (target == null) {
                // The app took its reply action away while the box was open: nothing is sent, nothing is lost
                Toast.makeText(context, R.string.reply_unavailable, Toast.LENGTH_SHORT).show()
            } else {
                val sentText = replyText
                if (sendRemoteReply(context, target, sentText)) {
                    val repo = SmartIslandRepositories.notificationRepository(context)
                    replyDrafts.clear(notification.key)
                    onReplyingChange(false)
                    onReplyStateChanged(false)
                    keyboardController?.hide()
                    // The app's re-post of that key (or its removal) must not close or reopen anything.
                    repo.sendCommand(SmartIslandCommand.ReplySent(notification.key))
                    repo.updateNotification(notification.key) { it.withSentReply(sentText) }
                    onReplyTextChange("")
                } else {
                    // The intent was cancelled or refused: the box stays open and the draft stays as typed
                    Toast.makeText(context, R.string.reply_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    ReplyInputRow(
        notification = notification,
        replyText = replyText,
        onTextChange = {
            onReplyTextChange(it)
            replyDrafts.set(notification.key, it)
        },
        focusRequester = focusRequester,
        onSend = sendReplyAction,
        onCancel = {
            onReplyingChange(false)
            onReplyStateChanged(false)
            keyboardController?.hide()
        }
    )
}

@Composable
private fun ReplyInputRow(
    notification: IslandNotification,
    replyText: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    val selection = LocalTextSelection.current
    var field by remember { mutableStateOf(TextFieldValue(replyText, TextRange(replyText.length))) }
    // The text can change from outside (sent, cleared)
    LaunchedEffect(replyText) {
        if (field.text != replyText) field = TextFieldValue(replyText, TextRange(replyText.length))
    }
    val selecting = !field.selection.collapsed
    fun dropSelection() { field = field.copy(selection = TextRange(field.selection.end)) }

    // A selection in the reply box counts like one in the card text: the island holds off its auto-collapse and
    // takes the first tap anywhere, or Back, to drop it
    val setSelecting = rememberUpdatedState(selection?.setSelecting)
    LaunchedEffect(selecting) { setSelecting.value?.invoke(selecting) }
    DisposableEffect(Unit) { onDispose { setSelecting.value?.invoke(false) } }
    val clearTick = selection?.clearTick ?: 0
    var seenClearTick by remember { mutableStateOf(clearTick) }
    LaunchedEffect(clearTick) {
        if (clearTick == seenClearTick) return@LaunchedEffect
        seenClearTick = clearTick
        dropSelection()
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
                .background(LocalIslandPalette.current.pick(Color(0xFF222222), Color(0xFFD9D8DB)))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            ReplySelectionToolbar(selectionActive = selecting, onCopied = { dropSelection() }) {
                BasicTextField(
                    value = field,
                    onValueChange = { value ->
                        val textChanged = value.text != field.text
                        field = value
                        if (textChanged) onTextChange(value.text)
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(LocalIslandPalette.current.text),
                    textStyle = TextStyle(
                        color = LocalIslandPalette.current.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onSend() }
                    ),
                    decorationBox = { innerTextField ->
                        if (replyText.isEmpty()) {
                            Text(
                                text = "Reply to ${notification.title.ifBlank { notification.appName }}...",
                                color = LocalIslandPalette.current.placeholder,
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
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else LocalIslandPalette.current.pick(Color(0xFF333333), Color(0xFFCFCED1))
                )
                .bounceClick {
                    onSend()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send Reply",
                tint = if (replyText.isNotBlank()) Color.White else LocalIslandPalette.current.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LocalIslandPalette.current.pick(Color(0xFF222222), Color(0xFFD9D8DB)))
                .bounceClick(onCancel),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cancel",
                tint = LocalIslandPalette.current.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
