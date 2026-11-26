/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Region
import android.view.View
import androidx.compose.ui.platform.AbstractComposeView
import com.agupta07505.smartisland.model.ExemptMode
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Proxy
import kotlin.math.ceil

/**
 * Internal insets listener, read by reflection, everything outside falls through to the app below
 * The service owns the window, so it hears about the listener being attached through [onListenerRegistered]
 */
internal class OverlayTouchableRegion(
    private val service: AccessibilityService,
    private val viewModelHolder: OverlayViewModelHolder,
    private val geometry: OverlayTouchGeometry,
    private val rotation: OverlayRotationController,
    private val isTouchableRegionSupported: MutableStateFlow<Boolean>,
    private val onListenerRegistered: () -> Unit,
) {
    private val viewModel: IslandViewModel get() = viewModelHolder.get()

    @SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
    internal fun setupTouchableRegion(view: AbstractComposeView) {
        runCatchingLogged(TAG, "Failed to setup touchable region") {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val insetsClass = Class.forName("android.view.ViewTreeObserver\$InternalInsetsInfo")

            val setTouchableInsetsMethod = insetsClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
            val touchableRegionField = insetsClass.getDeclaredField("touchableRegion").apply {
                isAccessible = true
            }

            val TOUCHABLE_INSETS_FRAME = 0
            val TOUCHABLE_INSETS_REGION = 3

            // equals/hashCode/toString must be answered too
            val proxyHolder = arrayOfNulls<Any>(1)
            val proxyListener = Proxy.newProxyInstance(
                service.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "equals" && args != null && args.size == 1) {
                    return@newProxyInstance args[0] === proxyHolder[0]
                }
                if (method.name == "hashCode") {
                    return@newProxyInstance System.identityHashCode(proxyHolder[0])
                }
                if (method.name == "toString") {
                    return@newProxyInstance "SmartIslandInsetsListener"
                }
                if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                    val insets = args[0]
                    val isExpanded = viewModel.expanded.value
                    val landscape = service.resources.configuration.orientation ==
                        Configuration.ORIENTATION_LANDSCAPE
                    // gone, or a static exempt-app pill
                    val isGone = view.visibility == View.GONE ||
                        viewModel.exemptionNow() == ExemptMode.Static
                    val settingsVal = viewModel.displaySettings.value
                    val notificationsCount = viewModel.visibleNotifications.value.size
                    val idleDot = settingsVal.idleCutout && !isExpanded && !viewModel.widgetIsland.open.value
                    val isIdleHidden = idleDot && !settingsVal.enableAppShortcuts
                    if (viewModel.fullscreenHidden.value && !isGone && !isIdleHidden) {
                        // A fullscreen app is in front: only the camera cutout takes a touch, the rest is the app's
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        val region = touchableRegionField.get(insets) as Region
                        region.setEmpty()
                        geometry.fullscreenCutoutTouchRectPx(settingsVal, landscape)?.let { region.set(it) }
                    } else if (isGone || isIdleHidden) {
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        val region = touchableRegionField.get(insets) as Region
                        region.setEmpty()
                        // Edge pill hidden by the long press
                        if (!isGone) {
                            geometry.edgeHiddenTouchRectPx(settingsVal, landscape, notificationsCount)
                                ?.let { region.set(it) }
                        }
                    } else if (isExpanded) {
                        val surface = viewModel.expandedSurfaceBounds.value
                        if (viewModel.outsideTouchPassesThrough()) {
                            // Only the island catches touches, a touch outside goes to the app below
                            setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                            val pad = (8 * service.resources.displayMetrics.density).toInt()
                            val region = touchableRegionField.get(insets) as Region
                            region.setEmpty()
                            if (surface != null) {
                                region.set(
                                    (surface.left - pad).coerceAtLeast(0), (surface.top - pad).coerceAtLeast(0),
                                    surface.right + pad, surface.bottom + pad
                                )
                            }
                            region.union(
                                geometry.edgeTouchRectPx(settingsVal, landscape, notificationsCount)
                                    ?: geometry.collapsedGroupTouchRect(settingsVal, landscape).rect
                            )
                        } else {
                            // Used by the user: the whole frame intercepts touches
                            setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_FRAME)
                        }
                    } else if (viewModel.widgetIsland.open.value) {
                        // The semi-expanded widget island is open
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_FRAME)
                    } else {
                        // The region is local to this already-offset window
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)

                        val density = service.resources.displayMetrics.density
                        val screenWidth = rotation.screenWidthPxNow()
                        geometry.edgeTouchRectPx(settingsVal, landscape, notificationsCount)?.let { edgeRect ->
                            val edgeRegion = touchableRegionField.get(insets) as Region
                            edgeRegion.set(edgeRect)
                            return@newProxyInstance null
                        }
                        if (idleDot) {
                            val width = settingsVal.width * density
                            val left = geometry.placedMainLeftPx(settingsVal, landscape, screenWidth.toFloat(), density, width)
                            val top = if (landscape) settingsVal.yOffset * density else 0f
                            val region = touchableRegionField.get(insets) as Region
                            region.set(
                                left.toInt(), top.toInt(),
                                ceil(left + width).toInt(), ceil(top + settingsVal.height * density).toInt()
                            )
                            return@newProxyInstance null
                        }
                        val group = geometry.collapsedGroupTouchRect(settingsVal, landscape)
                        val left = group.rect.left
                        val top = group.rect.top
                        val right = group.rect.right
                        val bottom = group.rect.bottom
                        val groupStartPx = group.startPx
                        val groupEndPx = group.endPx
                        val pillHeightPx = group.heightPx

                        val region = touchableRegionField.get(insets) as Region
                        region.set(left, top, right, bottom)
                        geometry.cutoutTouchRect(settingsVal, landscape, bottom, group.mainLeftPx)
                            ?.let { region.union(it) }
                        // landscape idle dot (separate tap target while a card is showing)
                        geometry.idleDotRectPx(
                            settingsVal, notificationsCount > 0 || viewModel.landscapeWidgetPill.value, groupStartPx,
                                groupEndPx, pillHeightPx, density, screenWidth,
                            isLandscape = landscape
                        )?.let { region.union(it) }
                    }
                }
                null
            }
            proxyHolder[0] = proxyListener

            val registerListener = {
                val observer = view.viewTreeObserver
                if (observer.isAlive) {
                    val addListenerMethod = observer.javaClass.getMethod(
                        "addOnComputeInternalInsetsListener",
                        listenerClass
                    )
                    addListenerMethod.invoke(observer, proxyListener)
                    isTouchableRegionSupported.value = true
                    onListenerRegistered()
                }
            }

            // ViewTreeObserver changes when the view is attached to a window.
            // We must register the listener on the live ViewTreeObserver of the attached window.
            if (view.isAttachedToWindow) {
                registerListener()
            } else {
                view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        registerListener()
                    }
                    override fun onViewDetachedFromWindow(v: View) = Unit
                })
            }
        } ?: run {
            isTouchableRegionSupported.value = false
            AppLogRecorder.w(TAG, "touchable region unsupported, using physical bounds")
        }
    }

    private companion object {
        const val TAG = "SmartIslandOverlayService"
    }
}
