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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.agupta07505.smartisland.model.withSentReply
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.formatNotificationTime
import com.agupta07505.smartisland.util.lastReaderLine
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// Tallest the title + message block grows once expanded; anything longer scrolls inside it.
private val EXPANDED_TEXT_MAX_HEIGHT = 150.dp

// Preview step of a card with pictures: height of the row (the picture fills it) and the widest the picture gets.
private val PREVIEW_ROW_HEIGHT = 96.dp
private val PREVIEW_MEDIA_MAX_WIDTH = 140.dp

internal fun expandableNotificationText(shortText: String, bigText: String): String =
    if (bigText.isNotBlank() && bigText != shortText && bigText.length > shortText.length) bigText else shortText

@Composable
fun NotificationExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit,
    showActions: Boolean = true,
    settings: SmartIslandSettings = SmartIslandSettings.Default,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onTextExpandedChanged: (Boolean) -> Unit = {},
    // The copy drawn under a card step that follows the finger: shows the given steps, reports nothing, owns no state.
    frozen: Boolean = false,
    stepsOverride: com.agupta07505.smartisland.model.CardSteps? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isReplying by remember(notification?.key) { mutableStateOf(false) }
    // The draft lives in the view model's store, so it survives Cancel, a collapse and the card being recreated.
    val replyDrafts = LocalReplyDrafts.current
    var replyText by remember(notification?.key) { mutableStateOf(notification?.key?.let { replyDrafts.get(it) }.orEmpty()) }

    val cardSteps = LocalCardSteps.current
    val steps = stepsOverride ?: cardSteps.stepsFor(notification?.key)
    val stepDrag = LocalStepDrag.current
    if (!frozen) {
        // The step the card is at while nothing follows the finger: what a drag that starts now fades away from.
        androidx.compose.runtime.SideEffect { if (!stepDrag.active) stepDrag.fromSteps = steps }
    }
    val textExpanded = steps.reader
    // Cards with 2 or more pictures have a middle preview step before the reader; one picture skips it (CardSteps).
    val previewOpen = steps.preview
    var titleCutOff by remember(notification?.key) { mutableStateOf(false) }
    var textCutOff by remember(notification?.key) { mutableStateOf(false) }
    val textScrollState = rememberScrollState()
    // The reader's scrolling is reported so a finger that only stops a fling is not taken for a tap (island gestures).
    if (!frozen) ReportReaderScroll(textScrollState)
    val shortText = notification?.text?.takeIf { it.isNotBlank() } ?: "New activity"
    val fullText = expandableNotificationText(shortText, notification?.bigText.orEmpty())
    val mediaList = notification?.media.orEmpty()
    val hasMedia = mediaList.isNotEmpty()
    // No arrows beside pictures. A card with a sticker has no preview or picture steps; the step logic counts no pictures for it.
    val stepMediaCount = if (mediaList.any { it.isSticker }) 0 else mediaList.size
    val canExpandText = titleCutOff || textCutOff || fullText != shortText || stepMediaCount > 0
    val previewShown = previewOpen && stepMediaCount >= 2 && !textExpanded
    LaunchedEffect(notification?.key, canExpandText, frozen) {
        if (!frozen) notification?.key?.let { cardSteps.reportExpandable(it, canExpandText) }
    }

    // Typing resumes here after a call took the island: the box opens again with the draft in it.
    val resumeKey by replyDrafts.resume.collectAsState()
    LaunchedEffect(resumeKey, notification?.key) {
        if (!frozen && resumeKey != null && resumeKey == notification?.key) isReplying = true
    }

    LaunchedEffect(isReplying) {
        if (frozen) return@LaunchedEffect
        onReplyStateChanged(isReplying)
        if (isReplying) {
            kotlinx.coroutines.delay(60)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // The picture open at full size comes from the overlay, so a tap outside the island can close it first.
    val mediaExpansion = LocalMediaExpansion.current
    val openMediaIndex = if (frozen) {
        steps.pictureIndex?.takeIf { it in mediaList.indices }
    } else if (notification != null && mediaExpansion.key == notification.key &&
        mediaExpansion.index in mediaList.indices
    ) mediaExpansion.index else null

    // While a long message or a picture is open the island waits for the reader instead of auto-collapsing.
    LaunchedEffect(textExpanded, previewShown, openMediaIndex != null) {
        if (frozen) return@LaunchedEffect
        onTextExpandedChanged(textExpanded || previewShown || openMediaIndex != null)
        if (!textExpanded) textScrollState.scrollTo(0)
    }

    DisposableEffect(notification?.key, frozen) {
        onDispose {
            if (!frozen) {
                onReplyStateChanged(false)
                onTextExpandedChanged(false)
            }
        }
    }

    // The image selector (2 or more pictures) takes the card's place at half size; in landscape it is the island beside the card.
    if (notification != null && steps.depth == com.agupta07505.smartisland.model.CardDepth.Selector && !mediaExpansion.besideCard &&
        openMediaIndex == null
    ) {
        PictureSelector(
            media = mediaList,
            onPick = { index ->
                if (!frozen) cardSteps.set(notification.key, com.agupta07505.smartisland.model.CardSteps.pickFromSelector(steps, index))
            },
            modifier = Modifier.fillMaxWidth().height(PICTURE_MID_HEIGHT)
        )
        return
    }

    if (notification != null && openMediaIndex != null && !mediaExpansion.besideCard) {
        ExpandedMediaViewer(
            media = mediaList[openMediaIndex],
            onClose = { if (!frozen) mediaExpansion.onChange(null, 0) },
            modifier = Modifier.fillMaxWidth().height(if (steps.pictureHalf) PICTURE_MID_HEIGHT else EXPANDED_MEDIA_HEIGHT)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Top-anchored while a card step follows the finger: the frozen copy of the step being left is laid out
            // in the new step's (taller) slot, and centring it there would draw its text lower than the real card's.
            .wrapContentHeight(align = rememberStepAwareAlignment())
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (previewShown) Modifier.height(PREVIEW_ROW_HEIGHT) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = if (textExpanded) Alignment.Top else Alignment.CenterVertically
        ) {
            val largeIcon = notification?.largeIcon
            val icon = notification?.icon
            val mainIcon = largeIcon ?: icon
            if (mainIcon != null) {
                val clipShape = if (largeIcon != null) CircleShape else RoundedCornerShape(8.dp)
                Box(modifier = Modifier.size(42.dp)) {
                    Image(
                        bitmap = mainIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(clipShape)
                    )
                    if (largeIcon != null && icon != null) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .background(LocalIslandPalette.current.surface, CircleShape)
                                .padding(1.5.dp)
                        ) {
                            Image(
                                bitmap = icon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(settings.notificationDotColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(notification?.appName?.firstOrNull()?.uppercase() ?: "S", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (textExpanded) {
                            Modifier
                                .heightIn(max = EXPANDED_TEXT_MAX_HEIGHT)
                                .verticalScroll(textScrollState)
                        } else Modifier
                    )
            ) {
                Text(
                    text = notification?.title?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Notification",
                    color = LocalIslandPalette.current.text,
                    maxLines = if (textExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    onTextLayout = { if (!textExpanded) titleCutOff = it.hasVisualOverflow }
                )
                // With pictures the reader keeps the normal text and drops only the picture's own label and caption.
                val bodyText = when {
                    previewShown -> lastReaderLine(notification?.readerText.orEmpty())
                    textExpanded && hasMedia -> notification?.readerText.orEmpty()
                    textExpanded -> fullText
                    else -> shortText
                }
                if (bodyText.isNotBlank()) Text(
                    text = bodyText,
                    color = LocalIslandPalette.current.textSecondary,
                    minLines = if (previewShown) 1 else 2,
                    maxLines = if (textExpanded) Int.MAX_VALUE else if (previewShown) 3 else 2,
                    overflow = if (textExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    onTextLayout = { if (!textExpanded) textCutOff = it.hasVisualOverflow }
                )
            }

            // Expanded reader with pictures: a thumbnail per picture or sticker on the right, each with its own arrow
            // that opens just that one at full size.
            if (notification != null && textExpanded && hasMedia) {
                val thumbScrollState = rememberScrollState()
                if (!frozen) ReportReaderScroll(thumbScrollState)
                Column(
                    modifier = Modifier
                        .heightIn(max = EXPANDED_TEXT_MAX_HEIGHT)
                        .verticalScroll(thumbScrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mediaList.forEachIndexed { index, media ->
                        MediaThumbnail(
                            media = media,
                            onExpand = when {
                                frozen -> null
                                // No arrows: the picture itself is the tap target. A sticker does nothing, one picture opens at half
                                // size, several open the image selector.
                                stepMediaCount == 0 -> null
                                stepMediaCount >= 2 -> ({
                                    cardSteps.set(
                                        notification.key,
                                        com.agupta07505.smartisland.model.CardSteps(reader = true, preview = steps.preview, selector = true)
                                    )
                                })
                                else -> ({
                                    cardSteps.set(
                                        notification.key,
                                        com.agupta07505.smartisland.model.CardSteps(
                                            reader = true, preview = steps.preview, pictureIndex = index, pictureHalf = true
                                        )
                                    )
                                })
                            }
                        )
                    }
                }
            }

            if (previewShown) {
                val last = mediaList.last()
                val ratio = (last.still.width.toFloat() / last.still.height.coerceAtLeast(1)).coerceIn(0.7f, 1.6f)
                NotificationMediaView(
                    media = last,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = PREVIEW_MEDIA_MAX_WIDTH)
                        .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = if (last.isSticker) {
                        androidx.compose.ui.layout.ContentScale.Fit
                    } else {
                        androidx.compose.ui.layout.ContentScale.Crop
                    },
                    maxSidePx = 384
                )
            }

            Text(
                text = notification?.let { formatNotificationTime(it.timeMillis) } ?: "",
                color = LocalIslandPalette.current.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (isReplying && notification != null) {
            val quickReplyAction = notification.actionIntents.firstOrNull { it.isQuickReply }
                ?: notification.actionIntents.firstOrNull { it.title.lowercase().contains("reply") }

            val sendReplyAction: () -> Unit = {
                if (replyText.isNotBlank()) {
                    val sentThroughApp = quickReplyAction?.pendingIntent != null
                    val sentText = replyText
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
                    replyDrafts.clear(notification.key)
                    isReplying = false
                    onReplyStateChanged(false)
                    keyboardController?.hide()
                    if (sentThroughApp) {
                        // The island stays open on the same card: the typed message is added to it, and the app's
                        // own re-post of that key (or its removal) no longer closes or reopens anything.
                        repo.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.ReplySent(notification.key))
                        repo.updateNotification(notification.key) { it.withSentReply(sentText) }
                        replyText = ""
                    } else {
                        // Nothing was really sent (no reply action).
                        repo.removeNotification(notification.key)
                        repo.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.CancelNotification(notification.key))
                        onCollapse()
                    }
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
                        .background(LocalIslandPalette.current.pick(Color(0xFF222222), Color(0xFFD9D8DB)))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = {
                            replyText = it
                            replyDrafts.set(notification.key, it)
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
                            onSend = { sendReplyAction() }
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

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else LocalIslandPalette.current.pick(Color(0xFF333333), Color(0xFFCFCED1))
                        )
                        .bounceClick {
                            sendReplyAction()
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
                        .bounceClick {
                            isReplying = false
                            onReplyStateChanged(false)
                            keyboardController?.hide()
                        },
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
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showActions && notification != null && notification.actionIntents.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        notification.actionIntents.forEach { action ->
                            val isReplyAction = action.isQuickReply || action.title.lowercase().contains("reply")
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(LocalIslandPalette.current.chipFill)
                                    .bounceClick {
                                        if (isReplyAction) {
                                            isReplying = true
                                        } else {
                                            if (action.pendingIntent != null) {
                                                triggerAction(context, notification.packageName, action.pendingIntent, action.title, notification.contentIntent)
                                            } else {
                                                Toast.makeText(context, "Clicked: ${action.title}", Toast.LENGTH_SHORT).show()
                                            }
                                            val repo = SmartIslandRepositories.notificationRepository(context)
                                            repo.removeNotification(notification.key)
                                            repo.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.CancelNotification(notification.key))
                                            onCollapse()
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = action.title,
                                    color = Color(0xFF1F2937),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Expands / collapses a shortened message; a message that is not cut off has no button (tapping the card opens the app).
                if (canExpandText) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(LocalIslandPalette.current.pick(Color(0xFF222222), Color(0xFFD9D8DB)))
                            .bounceClick {
                                // First -> preview (2+ pictures) -> reader; one picture or a long text goes straight to the
                                // reader; the reader's arrow closes straight back to the first state.
                                notification?.key?.let { key ->
                                    cardSteps.set(key, com.agupta07505.smartisland.model.CardSteps.arrow(steps, stepMediaCount))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (textExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (textExpanded) "Show less" else "Show more",
                            tint = LocalIslandPalette.current.text,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(media: com.agupta07505.smartisland.model.NotificationMedia, onExpand: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LocalIslandPalette.current.tint(0.08f))
            .then(if (onExpand != null) Modifier.bounceClick(onExpand) else Modifier)
    ) {
        NotificationMediaView(
            media = media,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (media.isSticker) {
                androidx.compose.ui.layout.ContentScale.Fit
            } else {
                androidx.compose.ui.layout.ContentScale.Crop
            },
            maxSidePx = 192
        )
    }
}

/**
 * One picture or sticker at the largest size an expanded island gets, with a corner arrow that closes it. A tap on
 * the picture itself does nothing (it must not open the app by accident).
 */
@Composable
internal fun ExpandedMediaViewer(
    media: com.agupta07505.smartisland.model.NotificationMedia,
    onClose: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(EXPANDED_MEDIA_HEIGHT)
) {
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        NotificationMediaView(
            media = media,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            maxSidePx = 1024
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .bounceClick(onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Close picture",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
