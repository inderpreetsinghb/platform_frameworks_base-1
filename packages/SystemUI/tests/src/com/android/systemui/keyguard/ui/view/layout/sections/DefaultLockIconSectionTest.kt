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
 *
 */

package com.android.systemui.keyguard.ui.view.layout.sections

import android.graphics.Point
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.test.filters.SmallTest
import com.android.keyguard.KeyguardUpdateMonitor
import com.android.keyguard.LockIconViewController
import com.android.systemui.res.R
import com.android.systemui.SysuiTestCase
import com.android.systemui.biometrics.AuthController
import com.android.systemui.flags.FeatureFlags
import com.android.systemui.flags.Flags
import com.android.systemui.shade.NotificationPanelView
import com.android.systemui.util.mockito.whenever
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(JUnit4::class)
@SmallTest
class DefaultLockIconSectionTest : SysuiTestCase() {
    @Mock private lateinit var keyguardUpdateMonitor: KeyguardUpdateMonitor
    @Mock private lateinit var authController: AuthController
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private lateinit var windowManager: WindowManager
    @Mock private lateinit var notificationPanelView: NotificationPanelView
    @Mock private lateinit var featureFlags: FeatureFlags
    @Mock private lateinit var lockIconViewController: LockIconViewController
    private lateinit var underTest: DefaultLockIconSection

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underTest =
            DefaultLockIconSection(
                keyguardUpdateMonitor,
                authController,
                windowManager,
                context,
                notificationPanelView,
                featureFlags,
                lockIconViewController
            )
    }

    @Test
    fun addViewsConditionally() {
        whenever(featureFlags.isEnabled(Flags.MIGRATE_LOCK_ICON)).thenReturn(true)
        val constraintLayout = ConstraintLayout(context, null)
        underTest.addViews(constraintLayout)
        assertThat(constraintLayout.childCount).isGreaterThan(0)
    }

    @Test
    fun addViewsConditionally_migrateFlagOff() {
        whenever(featureFlags.isEnabled(Flags.MIGRATE_LOCK_ICON)).thenReturn(false)
        val constraintLayout = ConstraintLayout(context, null)
        underTest.addViews(constraintLayout)
        assertThat(constraintLayout.childCount).isEqualTo(0)
    }

    @Test
    fun applyConstraints() {
        val cs = ConstraintSet()
        underTest.applyConstraints(cs)

        val constraint = cs.getConstraint(R.id.lock_icon_view)

        assertThat(constraint.layout.topToTop).isEqualTo(ConstraintSet.PARENT_ID)
        assertThat(constraint.layout.startToStart).isEqualTo(ConstraintSet.PARENT_ID)
    }

    @Test
    fun testCenterLockIcon() {
        val cs = ConstraintSet()
        underTest.centerLockIcon(Point(5, 6), 1F, cs)

        val constraint = cs.getConstraint(R.id.lock_icon_view)

        assertThat(constraint.layout.mWidth).isEqualTo(2)
        assertThat(constraint.layout.mHeight).isEqualTo(2)
        assertThat(constraint.layout.topToTop).isEqualTo(ConstraintSet.PARENT_ID)
        assertThat(constraint.layout.startToStart).isEqualTo(ConstraintSet.PARENT_ID)
        assertThat(constraint.layout.topMargin).isEqualTo(5)
        assertThat(constraint.layout.startMargin).isEqualTo(4)
    }
}
