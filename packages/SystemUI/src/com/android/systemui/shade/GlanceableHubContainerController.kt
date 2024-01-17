/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.shade

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.android.internal.annotations.VisibleForTesting
import com.android.systemui.communal.domain.interactor.CommunalInteractor
import com.android.systemui.communal.ui.viewmodel.CommunalViewModel
import com.android.systemui.compose.ComposeFacade.createCommunalContainer
import com.android.systemui.compose.ComposeFacade.isComposeAvailable
import com.android.systemui.keyguard.domain.interactor.KeyguardTransitionInteractor
import com.android.systemui.keyguard.shared.model.KeyguardState
import com.android.systemui.res.R
import com.android.systemui.shade.domain.interactor.ShadeInteractor
import com.android.systemui.util.kotlin.collectFlow
import javax.inject.Inject

/**
 * Controller that's responsible for the glanceable hub container view and its touch handling.
 *
 * This will be used until the glanceable hub is integrated into Flexiglass.
 */
class GlanceableHubContainerController
@Inject
constructor(
    private val communalInteractor: CommunalInteractor,
    private val communalViewModel: CommunalViewModel,
    private val keyguardTransitionInteractor: KeyguardTransitionInteractor,
    private val shadeInteractor: ShadeInteractor,
) {
    /** The container view for the hub. This will not be initialized until [initView] is called. */
    private lateinit var communalContainerView: View

    /**
     * The width of the area in which a right edge swipe can open the hub, in pixels. Read from
     * resources when [initView] is called.
     */
    private var edgeSwipeRegionWidth: Int = 0

    /**
     * True if we are currently tracking a gesture for opening the hub that started in the edge
     * swipe region.
     */
    private var isTrackingOpenGesture = false

    /**
     * True if the hub UI is fully open, meaning it should receive touch input.
     *
     * Tracks [CommunalInteractor.isCommunalShowing].
     */
    private var hubShowing = false

    /**
     * True if either the primary or alternate bouncer are open, meaning the hub should not receive
     * any touch input.
     *
     * Tracks [KeyguardTransitionInteractor.isFinishedInState] for [KeyguardState.isBouncerState].
     */
    private var anyBouncerShowing = false

    /**
     * True if the shade is fully expanded, meaning the hub should not receive any touch input.
     *
     * Tracks [ShadeInteractor.isAnyFullyExpanded].
     */
    private var shadeShowing = false

    /** Returns true if the glanceable hub is enabled and the container view can be created. */
    fun isEnabled(): Boolean {
        return communalInteractor.isCommunalEnabled && isComposeAvailable()
    }

    /**
     * Creates the container view containing the glanceable hub UI.
     *
     * @throws RuntimeException if [isEnabled] is false or the view is already initialized
     */
    fun initView(
        context: Context,
    ): View {
        return initView(createCommunalContainer(context, communalViewModel))
    }

    /** Override for testing. */
    @VisibleForTesting
    internal fun initView(containerView: View): View {
        if (!isEnabled()) {
            throw RuntimeException("Glanceable hub is not enabled")
        }
        if (::communalContainerView.isInitialized) {
            throw RuntimeException("Communal view has already been initialized")
        }

        communalContainerView = containerView

        edgeSwipeRegionWidth =
            communalContainerView.resources.getDimensionPixelSize(R.dimen.communal_grid_gutter_size)

        collectFlow(
            communalContainerView,
            keyguardTransitionInteractor.isFinishedInStateWhere(KeyguardState::isBouncerState),
            { anyBouncerShowing = it }
        )
        collectFlow(
            communalContainerView,
            communalInteractor.isCommunalShowing,
            { hubShowing = it }
        )
        collectFlow(
            communalContainerView,
            shadeInteractor.isAnyFullyExpanded,
            { shadeShowing = it }
        )

        return communalContainerView
    }

    /**
     * Notifies the hub container of a touch event. Returns true if it's determined that the touch
     * should go to the hub container and no one else.
     *
     * Special handling is needed because the hub container sits at the lowest z-order in
     * [NotificationShadeWindowView] and would not normally receive touches. We also cannot use a
     * [GestureDetector] as the hub container's SceneTransitionLayout is a Compose view that expects
     * to be fully in control of its own touch handling.
     */
    fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!::communalContainerView.isInitialized) {
            return false
        }

        val isDown = ev.actionMasked == MotionEvent.ACTION_DOWN
        val isUp = ev.actionMasked == MotionEvent.ACTION_UP
        val isCancel = ev.actionMasked == MotionEvent.ACTION_CANCEL

        // TODO(b/315207481): also account for opening animations of shade/bouncer and not just
        //  fully showing state
        val hubOccluded = anyBouncerShowing || shadeShowing

        // If the hub is fully visible, send all touch events to it.
        val communalVisible = hubShowing && !hubOccluded
        if (communalVisible) {
            return communalContainerView.dispatchTouchEvent(ev)
        }

        if (edgeSwipeRegionWidth == 0) {
            // If the edge region width has not been read yet or whatever reason, don't bother
            // intercepting touches to open the hub.
            return false
        }

        if (!isTrackingOpenGesture && isDown) {
            val x = ev.rawX
            val inOpeningSwipeRegion: Boolean =
                x >= communalContainerView.width - edgeSwipeRegionWidth
            if (inOpeningSwipeRegion && !hubOccluded) {
                isTrackingOpenGesture = true
                return communalContainerView.dispatchTouchEvent(ev)
            }
        } else if (isTrackingOpenGesture) {
            if (isUp || isCancel) {
                isTrackingOpenGesture = false
            }
            return communalContainerView.dispatchTouchEvent(ev)
        }

        return false
    }
}
