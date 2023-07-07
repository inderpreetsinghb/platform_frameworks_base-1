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

package com.android.wm.shell.flicker.service.splitscreen.benchmark

import android.platform.test.annotations.PlatinumTest
import android.platform.test.annotations.Presubmit
import android.tools.common.Rotation
import androidx.test.filters.RequiresDevice
import com.android.wm.shell.flicker.service.splitscreen.scenarios.EnterSplitScreenByDragFromTaskbar
import org.junit.Test

@RequiresDevice
class EnterSplitScreenByDragFromTaskbarGesturalNavLandscapeBenchmark :
    EnterSplitScreenByDragFromTaskbar(Rotation.ROTATION_90) {
    @PlatinumTest(focusArea = "sysui")
    @Presubmit
    @Test
    override fun enterSplitScreenByDragFromTaskbar() = super.enterSplitScreenByDragFromTaskbar()
}
