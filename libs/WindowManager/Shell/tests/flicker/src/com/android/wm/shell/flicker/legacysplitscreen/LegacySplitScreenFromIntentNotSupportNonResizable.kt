/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.wm.shell.flicker.legacysplitscreen

import android.platform.test.annotations.Presubmit
import android.provider.Settings
import android.view.Surface
import androidx.test.filters.RequiresDevice
import com.android.server.wm.flicker.FlickerParametersRunnerFactory
import com.android.server.wm.flicker.FlickerTestParameter
import com.android.server.wm.flicker.FlickerTestParameterFactory
import com.android.server.wm.flicker.appWindowBecomesInVisible
import com.android.server.wm.flicker.appWindowBecomesVisible
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.helpers.launchSplitScreen
import com.android.server.wm.flicker.layerBecomesInvisible
import com.android.server.wm.flicker.layerBecomesVisible
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.android.wm.shell.flicker.DOCKED_STACK_DIVIDER
import com.android.wm.shell.flicker.dockedStackDividerIsInvisible
import com.android.wm.shell.flicker.helpers.SplitScreenHelper
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized

/**
 * Test launch non-resizable activity via intent in split screen mode. When the device does not
 * support non-resizable in multi window, it should trigger exit split screen.
 * To run this test: `atest WMShellFlickerTests:LegacySplitScreenFromIntentNotSupportNonResizable`
 */
@RequiresDevice
@RunWith(Parameterized::class)
@Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LegacySplitScreenFromIntentNotSupportNonResizable(
    testSpec: FlickerTestParameter
) : LegacySplitScreenTransition(testSpec) {
    var prevSupportNonResizableInMultiWindow = 0

    override val transition: FlickerBuilder.(Map<String, Any?>) -> Unit
        get() = { configuration ->
            cleanSetup(this, configuration)
            setup {
                eachRun {
                    splitScreenApp.launchViaIntent(wmHelper)
                    device.launchSplitScreen(wmHelper)
                }
            }
            transitions {
                nonResizeableApp.launchViaIntent(wmHelper)
                wmHelper.waitForAppTransitionIdle()
            }
        }

    override val ignoredWindows: List<String>
        get() = listOf(DOCKED_STACK_DIVIDER, LAUNCHER_PACKAGE_NAME, LETTERBOX_NAME,
            nonResizeableApp.defaultWindowName, splitScreenApp.defaultWindowName,
            WindowManagerStateHelper.SPLASH_SCREEN_NAME,
            WindowManagerStateHelper.SNAPSHOT_WINDOW_NAME)

    @Before
    fun setup() {
        prevSupportNonResizableInMultiWindow = Settings.Global.getInt(context.contentResolver,
                Settings.Global.DEVELOPMENT_ENABLE_NON_RESIZABLE_MULTI_WINDOW)
        if (prevSupportNonResizableInMultiWindow == 1) {
            // Not support non-resizable in multi window
            Settings.Global.putInt(context.contentResolver,
                    Settings.Global.DEVELOPMENT_ENABLE_NON_RESIZABLE_MULTI_WINDOW, 0)
        }
    }

    @After
    fun teardown() {
        Settings.Global.putInt(context.contentResolver,
                Settings.Global.DEVELOPMENT_ENABLE_NON_RESIZABLE_MULTI_WINDOW,
                prevSupportNonResizableInMultiWindow)
    }

    @Presubmit
    @Test
    fun resizableAppLayerBecomesInvisible() =
            testSpec.layerBecomesInvisible(splitScreenApp.defaultWindowName)

    @Presubmit
    @Test
    fun nonResizableAppLayerBecomesVisible() =
            testSpec.layerBecomesVisible(nonResizeableApp.defaultWindowName)

    @Presubmit
    @Test
    fun resizableAppWindowBecomesInvisible() =
            testSpec.appWindowBecomesInVisible(splitScreenApp.defaultWindowName)

    @Presubmit
    @Test
    fun nonResizableAppWindowBecomesVisible() =
            testSpec.appWindowBecomesVisible(nonResizeableApp.defaultWindowName)

    @Presubmit
    @Test
    fun dockedStackDividerIsInvisibleAtEnd() = testSpec.dockedStackDividerIsInvisible()

    @Presubmit
    @Test
    fun onlyNonResizableAppWindowIsVisibleAtEnd() {
        testSpec.assertWmEnd {
            isInvisible(splitScreenApp.defaultWindowName)
            isVisible(nonResizeableApp.defaultWindowName)
        }
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun getParams(): Collection<FlickerTestParameter> {
            return FlickerTestParameterFactory.getInstance().getConfigNonRotationTests(
                repetitions = SplitScreenHelper.TEST_REPETITIONS,
                supportedRotations = listOf(Surface.ROTATION_0)) // b/178685668
        }
    }
}
