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

package com.android.wm.shell.flicker.pip

import android.platform.test.annotations.Postsubmit
import android.tools.common.Rotation
import android.tools.common.traces.component.ComponentNameMatcher
import android.tools.device.apphelpers.StandardAppHelper
import android.tools.device.flicker.legacy.LegacyFlickerTest
import android.tools.device.flicker.legacy.LegacyFlickerTestFactory
import org.junit.Test
import org.junit.runners.Parameterized

abstract class AppsEnterPipTransition(flicker: LegacyFlickerTest) : EnterPipTransition(flicker) {
    protected abstract val standardAppHelper: StandardAppHelper

    /** Checks [standardAppHelper] window remains visible throughout the animation */
    @Postsubmit
    @Test
    override fun pipAppWindowAlwaysVisible() {
        flicker.assertWm { this.isAppWindowVisible(standardAppHelper) }
    }

    /** Checks [standardAppHelper] layer remains visible throughout the animation */
    @Postsubmit
    @Test
    override fun pipAppLayerAlwaysVisible() {
        flicker.assertLayers { this.isVisible(standardAppHelper) }
    }

    /** Checks the content overlay appears then disappears during the animation */
    @Postsubmit
    @Test
    override fun pipOverlayLayerAppearThenDisappear() {
        super.pipOverlayLayerAppearThenDisappear()
    }

    /**
     * Checks that [standardAppHelper] window remains inside the display bounds throughout the whole
     * animation
     */
    @Postsubmit
    @Test
    override fun pipWindowRemainInsideVisibleBounds() {
        flicker.assertWmVisibleRegion(standardAppHelper) { coversAtMost(displayBounds) }
    }

    /**
     * Checks that the [standardAppHelper] layer remains inside the display bounds throughout the
     * whole animation
     */
    @Postsubmit
    @Test
    override fun pipLayerOrOverlayRemainInsideVisibleBounds() {
        flicker.assertLayersVisibleRegion(
            standardAppHelper.or(ComponentNameMatcher.PIP_CONTENT_OVERLAY)
        ) {
            coversAtMost(displayBounds)
        }
    }

    /** Checks that the visible region of [standardAppHelper] always reduces during the animation */
    @Postsubmit
    @Test
    override fun pipLayerReduces() {
        flicker.assertLayers {
            val pipLayerList = this.layers {
                standardAppHelper.layerMatchesAnyOf(it) && it.isVisible
            }
            pipLayerList.zipWithNext { previous, current ->
                current.visibleRegion.notBiggerThan(previous.visibleRegion.region)
            }
        }
    }

    /** Checks that [standardAppHelper] window becomes pinned */
    @Postsubmit
    @Test
    override fun pipWindowBecomesPinned() {
        flicker.assertWm {
            invoke("pipWindowIsNotPinned") { it.isNotPinned(standardAppHelper) }
                .then()
                .invoke("pipWindowIsPinned") { it.isPinned(standardAppHelper) }
        }
    }

    /** Checks [ComponentNameMatcher.LAUNCHER] layer remains visible throughout the animation */
    @Postsubmit
    @Test
    override fun launcherLayerBecomesVisible() {
        super.launcherLayerBecomesVisible()
    }

    /**
     * Checks that the focus changes between the [standardAppHelper] window and the launcher when
     * closing the pip window
     */
    @Postsubmit
    @Test
    override fun focusChanges() {
        flicker.assertEventLog {
            this.focusChanges(standardAppHelper.packageName, "NexusLauncherActivity")
        }
    }

    @Postsubmit
    @Test
    override fun hasAtMostOnePipDismissOverlayWindow() = super.hasAtMostOnePipDismissOverlayWindow()

    // ICommonAssertions.kt overrides due to Morris overlay

    /**
     * Checks that the [ComponentNameMatcher.NAV_BAR] layer is visible during the whole transition
     */
    @Postsubmit
    @Test
    override fun navBarLayerIsVisibleAtStartAndEnd() {
        // this fails due to Morris overlay
    }

    /**
     * Checks the position of the [ComponentNameMatcher.NAV_BAR] at the start and end of the
     * transition
     */
    @Postsubmit
    @Test
    override fun navBarLayerPositionAtStartAndEnd() {
        // this fails due to Morris overlay
    }

    /**
     * Checks that the [ComponentNameMatcher.NAV_BAR] window is visible during the whole transition
     *
     * Note: Phones only
     */
    @Postsubmit
    @Test
    override fun navBarWindowIsAlwaysVisible() {
        // this fails due to Morris overlay
    }

    /**
     * Checks that the [ComponentNameMatcher.TASK_BAR] layer is visible during the whole transition
     */
    @Postsubmit
    @Test
    override fun taskBarLayerIsVisibleAtStartAndEnd() = super.taskBarLayerIsVisibleAtStartAndEnd()

    /**
     * Checks that the [ComponentNameMatcher.TASK_BAR] window is visible during the whole transition
     *
     * Note: Large screen only
     */
    @Postsubmit
    @Test
    override fun taskBarWindowIsAlwaysVisible() = super.taskBarWindowIsAlwaysVisible()

    /**
     * Checks that the [ComponentNameMatcher.STATUS_BAR] layer is visible during the whole
     * transition
     */
    @Postsubmit
    @Test
    override fun statusBarLayerIsVisibleAtStartAndEnd() =
        super.statusBarLayerIsVisibleAtStartAndEnd()

    /**
     * Checks the position of the [ComponentNameMatcher.STATUS_BAR] at the start and end of the
     * transition
     */
    @Postsubmit
    @Test
    override fun statusBarLayerPositionAtStartAndEnd() = super.statusBarLayerPositionAtStartAndEnd()

    /**
     * Checks that the [ComponentNameMatcher.STATUS_BAR] window is visible during the whole
     * transition
     */
    @Postsubmit
    @Test override fun statusBarWindowIsAlwaysVisible() = super.statusBarWindowIsAlwaysVisible()

    /**
     * Checks that all layers that are visible on the trace, are visible for at least 2 consecutive
     * entries.
     */
    @Postsubmit
    @Test
    override fun visibleLayersShownMoreThanOneConsecutiveEntry() =
        super.visibleLayersShownMoreThanOneConsecutiveEntry()

    /**
     * Checks that all windows that are visible on the trace, are visible for at least 2 consecutive
     * entries.
     */
    @Postsubmit
    @Test
    override fun visibleWindowsShownMoreThanOneConsecutiveEntry() =
        super.visibleWindowsShownMoreThanOneConsecutiveEntry()

    /** Checks that all parts of the screen are covered during the transition */
    @Postsubmit
    @Test
    override fun entireScreenCovered() = super.entireScreenCovered()

    companion object {
        /**
         * Creates the test configurations.
         *
         * See [LegacyFlickerTestFactory.nonRotationTests] for configuring repetitions, screen
         * orientation and navigation modes.
         */
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun getParams() =
            LegacyFlickerTestFactory.nonRotationTests(
                supportedRotations = listOf(Rotation.ROTATION_0)
            )
    }
}
