/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.CardLinks
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.LinkKind
import com.agupta07505.smartisland.model.OtpChip
import com.agupta07505.smartisland.model.OtpCodes
import com.agupta07505.smartisland.model.opensReplyBox
import com.agupta07505.smartisland.ui.LocalSwapActionsAlpha
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.LinkOpener
import com.agupta07505.smartisland.util.formatNotificationTime
import com.agupta07505.smartisland.util.lastReaderLine
import kotlinx.coroutines.delay
import com.agupta07505.smartisland.ui.IslandText as Text

// Anything longer scrolls inside it
internal val EXPANDED_TEXT_MAX_HEIGHT = 150.dp

/** 1 row of link or suggested-reply chips under the text. */
internal val NOTIFICATION_CHIP_HEIGHT = 30.dp

private val ACTION_BUTTON_HEIGHT = 28.dp

/**
 * The tallest a notification card gets: the reader's text at its cap, a row of link or reply chips and the action
 * row, with the card's padding (20 dp above, 16 below) and the 14 dp between rows
 */
internal val NOTIFICATION_CARD_MAX_HEIGHT =
    20.dp + EXPANDED_TEXT_MAX_HEIGHT + 14.dp + NOTIFICATION_CHIP_HEIGHT + 14.dp + ACTION_BUTTON_HEIGHT + 16.dp

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
    // Copy under a step that follows the finger, owns no state
    frozen: Boolean = false,
    stepsOverride: CardSteps? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isReplying by remember(notification?.key) { mutableStateOf(false) }
    // Kept in the view model so it survives Cancel, a collapse and the card being recreated
    val replyDrafts = LocalReplyDrafts.current
    var replyText by remember(notification?.key) { mutableStateOf(notification?.key?.let { replyDrafts.get(it) }
        .orEmpty()) }

    val cardSteps = LocalCardSteps.current
    val steps = stepsOverride ?: cardSteps.stepsFor(notification?.key)
    val stepDrag = LocalStepDrag.current
    if (!frozen) {
        // What a drag that starts now fades away from
        SideEffect { if (!stepDrag.active) stepDrag.fromSteps = steps }
    }
    val textExpanded = steps.reader
    // 2 or more pictures get a middle preview step before the reader (CardSteps)
    val previewOpen = steps.preview
    var titleCutOff by remember(notification?.key) { mutableStateOf(false) }
    var textCutOff by remember(notification?.key) { mutableStateOf(false) }
    // The frozen copy of a step drag starts at the lines the finger left the live reader on
    val textScrollState = rememberScrollState(if (frozen) stepDrag.readerScroll?.value ?: 0 else 0)
    if (!frozen) SideEffect { if (!stepDrag.active) stepDrag.readerScroll = textScrollState }
    // Reported so a finger that only stops a fling is not taken for a tap
    if (!frozen) ReportReaderScroll(textScrollState)
    val shortText = notification?.text?.takeIf { it.isNotBlank() } ?: "New activity"
    val fullText = expandableNotificationText(shortText, notification?.bigText.orEmpty())
    val mediaList = notification?.media.orEmpty()
    val hasMedia = mediaList.isNotEmpty()
    // A card with a sticker has no preview or picture steps, so no arrows.
    val stepMediaCount = if (mediaList.any { it.isSticker }) 0 else mediaList.size
    val context = LocalContext.current
    val link = remember(notification?.text, notification?.bigText) {
        notification?.let { CardLinks.forCard(it.text, it.bigText) }
    }
    val linkHandler = remember(link) { link?.let { LinkOpener.handlerFor(context, it) } }
    // A one-time code message: the app's own copy button, Android's, else a copy of the code the island found
    val otpChip = remember(notification?.key, notification?.title, notification?.text, notification?.bigText,
        notification?.actionIntents, notification?.systemCopyAction, settings.showCopyCodeChip,
        settings.showSuggestedReplies) {
        notification?.let { OtpCodes.chipFor(it, settings.showCopyCodeChip, settings.showSuggestedReplies) }
    }
    val canExpandText = titleCutOff || textCutOff || fullText != shortText || stepMediaCount > 0
    val previewShown = previewOpen && stepMediaCount >= 2 && !textExpanded
    LaunchedEffect(notification?.key, canExpandText, frozen) {
        if (!frozen) notification?.key?.let { cardSteps.reportExpandable(it, canExpandText) }
    }

    // Reopens with the draft after a call took the island
    val resumeKey by replyDrafts.resume.collectAsState()
    LaunchedEffect(resumeKey, notification?.key) {
        if (!frozen && resumeKey != null && resumeKey == notification?.key) isReplying = true
    }

    // Back closes the reply box alone, the draft stays and the island keeps its card
    val closeTick by replyDrafts.closeTick.collectAsState()
    var seenCloseTick by remember { mutableStateOf(closeTick) }
    LaunchedEffect(closeTick) {
        if (closeTick == seenCloseTick) return@LaunchedEffect
        seenCloseTick = closeTick
        if (frozen || !isReplying) return@LaunchedEffect
        isReplying = false
        keyboardController?.hide()
    }

    LaunchedEffect(isReplying) {
        if (frozen) return@LaunchedEffect
        onReplyStateChanged(isReplying)
        if (isReplying) {
            delay(60)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // comes from the overlay so a tap outside the island can close it first
    val mediaExpansion = LocalMediaExpansion.current
    val openMediaIndex = if (frozen) {
        steps.pictureIndex?.takeIf { it in mediaList.indices }
    } else if (notification != null && mediaExpansion.key == notification.key &&
        mediaExpansion.index in mediaList.indices
    ) mediaExpansion.index else null

    // the island waits for the reader instead of auto-collapsing
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

    // takes the card's place at half size, in landscape it is the island beside the card
    if (notification != null && steps.depth == CardDepth.Selector && !mediaExpansion.besideCard &&
        openMediaIndex == null
    ) {
        PictureSelector(
            media = mediaList,
            onPick = { index ->
                if (!frozen) cardSteps.set(notification.key, CardSteps.pickFromSelector(steps, index))
            },
            modifier = Modifier.fillMaxWidth().height(PICTURE_MID_HEIGHT)
        )
        return
    }

    if (notification != null && openMediaIndex != null && !mediaExpansion.besideCard) {
        ExpandedMediaViewer(
            media = mediaList[openMediaIndex],
            onClose = { if (!frozen) mediaExpansion.onChange(null, 0) },
            modifier = Modifier.fillMaxWidth()
                .height(if (steps.pictureHalf) PICTURE_MID_HEIGHT else EXPANDED_MEDIA_HEIGHT)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Top-anchored while a step follows the finger, centring the frozen copy in the taller slot would draw its
            // text lower than the real card's
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
            NotificationAppIcon(notification, settings)
            NotificationTextColumn(
                notification = notification,
                textExpanded = textExpanded,
                previewShown = previewShown,
                hasMedia = hasMedia,
                shortText = shortText,
                fullText = fullText,
                textScrollState = textScrollState,
                // The reader, or the short text of a card the white pill holds (a frozen step copy is only a picture)
                selectable = !frozen && (textExpanded || LocalTextSelection.current?.heldByPill == true),
                onTitleCutOffChange = { titleCutOff = it },
                onTextCutOffChange = { textCutOff = it }
            )

            // A thumbnail per picture on the right, each with its own arrow that opens just that one
            if (notification != null && textExpanded && hasMedia) {
                ReaderThumbnails(
                    notification = notification,
                    mediaList = mediaList,
                    stepMediaCount = stepMediaCount,
                    steps = steps,
                    cardSteps = cardSteps,
                    frozen = frozen
                )
            }

            if (previewShown) PreviewPicture(mediaList.last())

            Text(
                text = notification?.let { formatNotificationTime(it.timeMillis) } ?: "",
                color = LocalIslandPalette.current.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (isReplying && notification != null) {
            ReplyRow(
                notification = notification,
                replyText = replyText,
                onReplyTextChange = { replyText = it },
                onReplyingChange = { isReplying = it },
                focusRequester = focusRequester,
                onReplyStateChanged = onReplyStateChanged
            )
        } else {
            if (!frozen && showActions && notification != null && otpChip != null) {
                // Copy code takes the place of the Open link chip and the replies, the app's own button wins
                CopyCodeChip(notification = notification, chip = otpChip, onCopied = onCollapse)
            } else if (!frozen && showActions && notification != null && link != null &&
                (linkHandler != null || CardLinks.kind(link) == LinkKind.APP)
            ) {
                // The link's own button takes the replies' place, like Android's Open link does
                OpenLinkChip(link = link, handler = linkHandler, onOpened = onCollapse)
            } else if (!frozen && showActions && settings.showSuggestedReplies && notification != null &&
                notification.suggestedReplies.isNotEmpty()
            ) {
                SuggestedReplyChips(notification)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotificationActionButtons(
                    notification = notification,
                    showActions = showActions,
                    // The chip already is this button
                    hiddenAction = if (!frozen) (otpChip as? OtpChip.AppAction)?.action else null,
                    onReply = { isReplying = true },
                    onCollapse = onCollapse
                )

                // A message that is not cut off has no button, tapping the card opens the app
                if (canExpandText) {
                    ExpandTextButton(
                        expanded = textExpanded,
                        onClick = {
                            // First -> preview (2+ pictures) -> reader, 1 picture or a long text goes straight to the
                            // reader, the reader's arrow closes straight back to the first state
                            notification?.key?.let { key ->
                                cardSteps.set(key, CardSteps.arrow(steps, stepMediaCount))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationAppIcon(notification: IslandNotification?, settings: SmartIslandSettings) {
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
            Text(notification?.appName?.firstOrNull()?.uppercase() ?: "S", color = Color.White,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RowScope.NotificationTextColumn(
    notification: IslandNotification?,
    textExpanded: Boolean,
    previewShown: Boolean,
    hasMedia: Boolean,
    shortText: String,
    fullText: String,
    textScrollState: ScrollState,
    selectable: Boolean,
    onTitleCutOffChange: (Boolean) -> Unit,
    onTextCutOffChange: (Boolean) -> Unit
) {
    val title = notification?.title?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Notification"
    // Drops only the picture's own label and caption
    val bodyText = when {
        previewShown -> lastReaderLine(notification?.readerText.orEmpty())
        textExpanded && hasMedia -> notification?.readerText.orEmpty()
        textExpanded -> fullText
        else -> shortText
    }
    val links = remember(title, bodyText) { CardLinks.find("$title\n$bodyText") }
    IslandSelectionContainer(enabled = selectable, modifier = Modifier.weight(1f), links = links) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (textExpanded) {
                    Modifier
                        .heightIn(max = EXPANDED_TEXT_MAX_HEIGHT)
                        .verticalScroll(textScrollState)
                } else Modifier
            )
    ) {
        Text(
            text = title,
            color = LocalIslandPalette.current.text,
            maxLines = if (textExpanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            onTextLayout = { if (!textExpanded) onTitleCutOffChange(it.hasVisualOverflow) }
        )
        if (bodyText.isNotBlank()) Text(
            text = bodyText,
            color = LocalIslandPalette.current.textSecondary,
            minLines = if (previewShown) 1 else 2,
            maxLines = if (textExpanded) Int.MAX_VALUE else if (previewShown) 3 else 2,
            overflow = if (textExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            onTextLayout = { if (!textExpanded) onTextCutOffChange(it.hasVisualOverflow) }
        )
    }
    }
}

@Composable
private fun RowScope.NotificationActionButtons(
    notification: IslandNotification?,
    showActions: Boolean,
    hiddenAction: IslandNotificationAction?,
    onReply: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val swapActionsAlpha = LocalSwapActionsAlpha.current
    val shownActions = notification?.actionIntents.orEmpty().filter { it !== hiddenAction }
    if (showActions && notification != null && shownActions.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = swapActionsAlpha?.invoke() ?: 1f }
        ) {
            shownActions.forEach { action ->
                // Only an action that takes text opens the reply box, a plain "Reply" button opens the app like any other
                val isReplyAction = action.opensReplyBox()
                Box(
                    modifier = Modifier
                        .height(ACTION_BUTTON_HEIGHT)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LocalIslandPalette.current.chipFill)
                        .bounceClick {
                            if (isReplyAction) {
                                onReply()
                            } else {
                                // A button with no intent, or one the app no longer accepts, did nothing: the card stays
                                val done = triggerAction(
                                    context, notification.packageName, action.pendingIntent, action.title,
                                    notification.contentIntent
                                )
                                if (done) {
                                    val repo = SmartIslandRepositories.notificationRepository(context)
                                    repo.removeNotification(notification.key)
                                    repo.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
                                    onCollapse()
                                }
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
}

@Composable
private fun ExpandTextButton(expanded: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(LocalIslandPalette.current.pick(Color(0xFF222222), Color(0xFFD9D8DB)))
            .bounceClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Show less" else "Show more",
            tint = LocalIslandPalette.current.text,
            modifier = Modifier.size(16.dp)
        )
    }
}
