/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import kotlinx.coroutines.launch

private enum class HomeSection {
    Permissions,
    Positions,
    Support,
    About
}

@Composable
fun SmartIslandHomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember(context) { SmartIslandSettingsRepository(context.applicationContext) }
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = SmartIslandSettings.Default)
    val scope = rememberCoroutineScope()
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notificationGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = Settings.canDrawOverlays(context)
                notificationGranted = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings.enabled, overlayGranted) {
        if (settings.enabled && overlayGranted) {
            ContextCompat.startForegroundService(context, Intent(context, SmartIslandOverlayService::class.java))
        } else {
            context.stopService(Intent(context, SmartIslandOverlayService::class.java))
        }
    }

    var activeSection by remember { mutableStateOf<HomeSection?>(null) }
    var transitionDirection by remember { mutableStateOf(1) } // 1 = forward (slide left), -1 = backward (slide right)

    // Handle back press to close details section
    BackHandler(enabled = activeSection != null) {
        transitionDirection = -1
        activeSection = null
    }

    AnimatedContent(
        targetState = activeSection,
        transitionSpec = {
            if (transitionDirection == 1) {
                (slideInHorizontally(initialOffsetX = { it }) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
            } else {
                (slideInHorizontally(initialOffsetX = { -it }) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
            }
        },
        label = "SectionTransition"
    ) { targetSection ->
        if (targetSection == null) {
            // Main Dashboard View - shifted top padding down slightly for cool appearance
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7F8FA))
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                HeaderSection()

                // Main Toggle Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enable Smart Island",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (overlayGranted) "Overlay service is ready" else "Grant overlay permission to start",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF667085)
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            enabled = overlayGranted,
                            onCheckedChange = { enabled ->
                                scope.launch { repository.setEnabled(enabled) }
                            }
                        )
                    }
                }

                // Demo Test Controls Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Test Controls",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF667085),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ElevatedButton(
                                onClick = { SmartIslandOverlayService.showDemo(IslandMode.Notification) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Notify", fontSize = 12.sp)
                            }
                            ElevatedButton(
                                onClick = { SmartIslandOverlayService.showDemo(IslandMode.IncomingCall) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Call", fontSize = 12.sp)
                            }
                            ElevatedButton(
                                onClick = { SmartIslandOverlayService.showDemo(IslandMode.Music) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Music", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Categories heading shifted slightly down
                Text(
                    text = "Configure Features",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF667085),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )

                // Topic Sections
                SectionRow(
                    title = "Permissions",
                    description = "Overlay, notification access, system warnings status",
                    icon = Icons.Rounded.Lock,
                    iconBgColor = Color(0xFFE0E7FF),
                    iconTint = Color(0xFF4F46E5),
                    statusText = if (overlayGranted && notificationGranted) "Active" else "Action Required",
                    statusColor = if (overlayGranted && notificationGranted) Color(0xFF0F9F6E) else Color(0xFFE88C25),
                    onClick = {
                        transitionDirection = 1
                        activeSection = HomeSection.Permissions
                    }
                )

                SectionRow(
                    title = "Positions",
                    description = "Adjust island width, height, corner radius & offsets",
                    icon = Icons.Rounded.Refresh,
                    iconBgColor = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    onClick = {
                        transitionDirection = 1
                        activeSection = HomeSection.Positions
                    }
                )

                SectionRow(
                    title = "Support & Feedback",
                    description = "Request features, star on GitHub, review & bug report",
                    icon = Icons.Rounded.Feedback,
                    iconBgColor = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    onClick = {
                        transitionDirection = 1
                        activeSection = HomeSection.Support
                    }
                )

                SectionRow(
                    title = "About",
                    description = "App version, privacy policy, terms & contact developer",
                    icon = Icons.Rounded.Info,
                    iconBgColor = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF7C3AED),
                    onClick = {
                        transitionDirection = 1
                        activeSection = HomeSection.About
                    }
                )

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Made with ❤️ by Animesh Gupta",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF98A2B3)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        } else {
            // Detailed Topic Views
            when (targetSection) {
                HomeSection.Permissions -> {
                    SectionDetailScreen(
                        title = "Permissions",
                        onBack = {
                            transitionDirection = -1
                            activeSection = null
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            PermissionCard(
                                title = "Overlay permission",
                                description = "Required to draw the pill above other apps.",
                                granted = overlayGranted,
                                buttonText = "Allow",
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                    overlayGranted = Settings.canDrawOverlays(context)
                                }
                            )

                            PermissionCard(
                                title = "Notification listener",
                                description = "Lets Smart Island show incoming notification content.",
                                granted = notificationGranted,
                                buttonText = "Enable",
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    notificationGranted = isNotificationListenerEnabled(context)
                                }
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFF667085)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Overlay system warning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "Redirect to notification settings to hide the \"displaying over other apps\" alert.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5C6675)
                                        )
                                    }
                                    Button(onClick = {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, "android")
                                        }
                                        runCatching { context.startActivity(intent) }
                                    }) {
                                        Text("Hide")
                                    }
                                }
                            }
                        }
                    }
                }
                HomeSection.Positions -> {
                    SectionDetailScreen(
                        title = "Positions",
                        onBack = {
                            transitionDirection = -1
                            activeSection = null
                        }
                    ) {
                        SettingsCard(settings = settings, repository = repository)
                    }
                }
                HomeSection.Support -> {
                    SectionDetailScreen(
                        title = "Support & Feedback",
                        onBack = {
                            transitionDirection = -1
                            activeSection = null
                        }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                AboutItem(
                                    label = "Star on GitHub",
                                    icon = Icons.Rounded.Star,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland"))
                                        runCatching { context.startActivity(intent) }
                                    }
                                )
                                AboutItem(
                                    label = "Request a Feature",
                                    icon = Icons.Rounded.Feedback,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/issues/new?template=feature_request.md"))
                                        runCatching { context.startActivity(intent) }
                                    }
                                )
                                AboutItem(
                                    label = "Report a Bug",
                                    icon = Icons.Rounded.BugReport,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/issues/new?template=bug_report.md"))
                                        runCatching { context.startActivity(intent) }
                                    }
                                )
                                AboutItem(
                                    label = "App Review",
                                    icon = Icons.Rounded.RateReview,
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/agupta07505/SmartIsland/issues/new?template=app_review.md")
                                        )
                                        runCatching { context.startActivity(intent) }
                                    }
                                )
                                AboutItem(
                                    label = "Licence",
                                    icon = Icons.Rounded.Gavel,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/LICENSE"))
                                        runCatching { context.startActivity(intent) }
                                    }
                                )
                            }
                        }
                    }
                }
                HomeSection.About -> {
                    SectionDetailScreen(
                        title = "About",
                        onBack = {
                            transitionDirection = -1
                            activeSection = null
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    AboutItem(
                                        label = "Version",
                                        value = getAppVersion(context),
                                        icon = Icons.Rounded.Info,
                                        onClick = {}
                                    )
                                    AboutItem(
                                        label = "Privacy Policy",
                                        icon = Icons.Rounded.Lock,
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/PRIVACY.md"))
                                            runCatching { context.startActivity(intent) }
                                        }
                                    )
                                    AboutItem(
                                        label = "Terms of Use",
                                        icon = Icons.Rounded.Description,
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland/blob/main/TERMS.md"))
                                            runCatching { context.startActivity(intent) }
                                        }
                                    )
                                    AboutItem(
                                        label = "Open Source",
                                        icon = Icons.Rounded.Code,
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland"))
                                            runCatching { context.startActivity(intent) }
                                        }
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("Contact me", style = MaterialTheme.typography.titleSmall, color = Color(0xFF667085), fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ContactButton(
                                            icon = { GithubIcon(tint = Color(0xFF1F2937)) },
                                            label = "GitHub",
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505"))
                                                runCatching { context.startActivity(intent) }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        ContactButton(
                                            icon = { LinkedinIcon() },
                                            label = "LinkedIn",
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://linkedin.com/in/agupta07505"))
                                                runCatching { context.startActivity(intent) }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ContactButton(
                                            icon = { InstagramIcon() },
                                            label = "Instagram",
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/agupta07505"))
                                                runCatching { context.startActivity(intent) }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        ContactButton(
                                            icon = { EmailIcon(tint = Color(0xFFEA4335)) },
                                            label = "Email",
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:agupta07505@gmail.com")
                                                    putExtra(Intent.EXTRA_SUBJECT, "Smart Island App Feedback")
                                                }
                                                runCatching { context.startActivity(intent) }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo inside a sleek Black Rounded Square Box as requested
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(72.dp)
                .background(Color.Black, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.agupta07505.smartisland.R.mipmap.ic_launcher),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
        Text(
            text = "Smart Island",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF101828),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Smart Island reimagines Android notifications with a beautiful floating experience, interactive controls, and fluid animations—keeping everything important just a glance away.",
            color = Color(0xFF667085),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SectionRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    statusText: String? = null,
    statusColor: Color = Color(0xFF667085),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconBgColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF101828),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF667085)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (statusText != null) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF98A2B3),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SectionDetailScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    // Increased top padding to 36dp to shift detail headers slightly down to look cool
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF101828)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF101828),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun AboutItem(
    label: String,
    icon: ImageVector,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = value == null, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF667085),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF344054)
            )
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF98A2B3)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF98A2B3),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ContactButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF344054),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GithubIcon(tint: Color = Color.Black) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(12f * scaleX, 2f * scaleY)
            cubicTo(6.477f * scaleX, 2f * scaleY, 2f * scaleX, 6.477f * scaleX, 2f * scaleX, 12f * scaleY)
            cubicTo(2f * scaleX, 16.42f * scaleY, 4.865f * scaleX, 20.166f * scaleY, 8.839f * scaleX, 21.489f * scaleY)
            cubicTo(9.339f * scaleX, 21.581f * scaleY, 9.521f * scaleX, 21.272f * scaleY, 9.521f * scaleX, 21.007f * scaleY)
            cubicTo(9.521f * scaleX, 20.77f * scaleY, 9.513f * scaleX, 20.141f * scaleY, 9.508f * scaleX, 19.307f * scaleY)
            cubicTo(6.726f * scaleX, 19.91f * scaleY, 6.139f * scaleX, 17.97f * scaleY, 6.139f * scaleX, 17.97f * scaleY)
            cubicTo(5.685f * scaleX, 16.814f * scaleY, 5.029f * scaleX, 16.506f * scaleY, 5.029f * scaleX, 16.506f * scaleY)
            cubicTo(4.121f * scaleX, 15.886f * scaleY, 5.098f * scaleX, 15.898f * scaleY, 5.098f * scaleX, 15.898f * scaleY)
            cubicTo(6.101f * scaleX, 15.968f * scaleY, 6.629f * scaleX, 16.928f * scaleY, 6.629f * scaleX, 16.928f * scaleY)
            cubicTo(7.521f * scaleX, 18.457f * scaleY, 8.97f * scaleX, 18.015f * scaleY, 9.539f * scaleX, 17.759f * scaleY)
            cubicTo(9.631f * scaleX, 17.113f * scaleY, 9.889f * scaleX, 16.673f * scaleY, 10.175f * scaleX, 16.423f * scaleY)
            cubicTo(7.955f * scaleX, 16.17f * scaleY, 5.62f * scaleX, 15.313f * scaleY, 5.62f * scaleX, 11.48f * scaleY)
            cubicTo(5.62f * scaleX, 10.389f * scaleY, 6.01f * scaleX, 9.496f * scaleY, 6.649f * scaleX, 8.797f * scaleY)
            cubicTo(6.546f * scaleX, 8.544f * scaleY, 6.203f * scaleX, 7.527f * scaleY, 6.747f * scaleX, 6.15f * scaleY)
            cubicTo(6.747f * scaleX, 6.15f * scaleY, 7.587f * scaleX, 5.881f * scaleY, 9.497f * scaleX, 7.175f * scaleY)
            cubicTo(10.295f * scaleX, 6.953f * scaleY, 11.15f * scaleX, 6.842f * scaleY, 12f * scaleX, 6.838f * scaleY)
            cubicTo(12.85f * scaleX, 6.842f * scaleY, 13.705f * scaleX, 6.953f * scaleY, 14.503f * scaleX, 7.175f * scaleY)
            cubicTo(16.413f * scaleX, 5.881f * scaleY, 17.253f * scaleX, 6.15f * scaleY, 17.253f * scaleX, 6.15f * scaleY)
            cubicTo(17.797f * scaleX, 7.527f * scaleY, 17.454f * scaleX, 8.544f * scaleY, 17.351f * scaleX, 8.797f * scaleY)
            cubicTo(17.99f * scaleX, 9.496f * scaleY, 18.38f * scaleX, 10.389f * scaleY, 18.38f * scaleX, 11.48f * scaleY)
            cubicTo(18.38f * scaleX, 15.323f * scaleY, 16.041f * scaleX, 16.168f * scaleY, 13.813f * scaleX, 16.415f * scaleY)
            cubicTo(14.172f * scaleX, 16.724f * scaleY, 14.491f * scaleX, 17.334f * scaleY, 14.491f * scaleX, 18.267f * scaleY)
            cubicTo(14.491f * scaleX, 19.603f * scaleY, 14.479f * scaleX, 20.682f * scaleY, 14.479f * scaleX, 21.01f * scaleY)
            cubicTo(14.479f * scaleX, 21.277f * scaleY, 14.659f * scaleX, 21.589f * scaleY, 15.167f * scaleX, 21.489f * scaleY)
            cubicTo(19.141f * scaleX, 20.16f * scaleY, 22f * scaleX, 16.418f * scaleY, 22f * scaleX, 12f * scaleY)
            cubicTo(22f * scaleX, 6.477f * scaleY, 17.523f * scaleX, 2f * scaleY, 12f * scaleX, 2f * scaleY)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
private fun LinkedinIcon() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(Color(0xFF0A66C2), shape = RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "in",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

@Composable
private fun InstagramIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color(0xFFE1306C),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFE1306C),
            radius = 4f.dp.toPx(),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFE1306C),
            radius = 1f.dp.toPx(),
            center = Offset(w * 0.72f, h * 0.28f)
        )
    }
}

@Composable
private fun EmailIcon(tint: Color = Color.Black) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(2.dp.toPx(), 4.dp.toPx())
            lineTo(w - 2.dp.toPx(), 4.dp.toPx())
            lineTo(w - 2.dp.toPx(), h - 4.dp.toPx())
            lineTo(2.dp.toPx(), h - 4.dp.toPx())
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
        val foldPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(2.dp.toPx(), 5.dp.toPx())
            lineTo(w / 2f, h / 2f)
            lineTo(w - 2.dp.toPx(), 5.dp.toPx())
        }
        drawPath(
            path = foldPath,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
        )
    }
}

@Composable
private fun SettingsCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Island controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SliderSettingItem("Island width", settings.width, 76f..180f, { scope.launch { repository.setWidth(it) } })
            SliderSettingItem("Island height", settings.height, 24f..60f, { scope.launch { repository.setHeight(it) } })
            SliderSettingItem("X offset", settings.xOffset, -140f..140f, { scope.launch { repository.setXOffset(it) } })
            SliderSettingItem("Y offset", settings.yOffset, 0f..80f, { scope.launch { repository.setYOffset(it) } })
            SliderSettingItem("Corner radius", settings.cornerRadius, 8f..40f, { scope.launch { repository.setCornerRadius(it) } })
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { scope.launch { repository.resetPosition() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Reset position")
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return enabled?.split(":")?.any {
        ComponentName.unflattenFromString(it)?.packageName == context.packageName
    } == true
}

private fun getAppVersion(context: Context): String {
    return runCatching {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0"
    }.getOrDefault("1.0")
}
