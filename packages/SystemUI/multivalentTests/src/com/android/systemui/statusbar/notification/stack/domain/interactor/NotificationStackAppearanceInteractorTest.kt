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

package com.android.systemui.statusbar.notification.stack.domain.interactor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.systemui.SysuiTestCase
import com.android.systemui.coroutines.collectLastValue
import com.android.systemui.kosmos.testScope
import com.android.systemui.shade.data.repository.shadeRepository
import com.android.systemui.shade.shared.model.ShadeMode
import com.android.systemui.statusbar.notification.stack.shared.model.StackBounds
import com.android.systemui.statusbar.notification.stack.shared.model.StackRounding
import com.android.systemui.testKosmos
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NotificationStackAppearanceInteractorTest : SysuiTestCase() {

    private val kosmos = testKosmos()
    private val testScope = kosmos.testScope
    private val underTest = kosmos.notificationStackAppearanceInteractor

    @Test
    fun stackBounds() =
        testScope.runTest {
            val stackBounds by collectLastValue(underTest.stackBounds)

            val bounds1 =
                StackBounds(
                    top = 100f,
                    bottom = 200f,
                )
            underTest.setStackBounds(bounds1)
            assertThat(stackBounds).isEqualTo(bounds1)

            val bounds2 =
                StackBounds(
                    top = 200f,
                    bottom = 300f,
                )
            underTest.setStackBounds(bounds2)
            assertThat(stackBounds).isEqualTo(bounds2)
        }

    @Test
    fun stackRounding() =
        testScope.runTest {
            val stackRounding by collectLastValue(underTest.stackRounding)

            kosmos.shadeRepository.setShadeMode(ShadeMode.Single)
            assertThat(stackRounding).isEqualTo(StackRounding(roundTop = true, roundBottom = false))

            kosmos.shadeRepository.setShadeMode(ShadeMode.Split)
            assertThat(stackRounding).isEqualTo(StackRounding(roundTop = true, roundBottom = true))
        }

    @Test(expected = IllegalStateException::class)
    fun setStackBounds_withImproperBounds_throwsException() =
        testScope.runTest {
            underTest.setStackBounds(
                StackBounds(
                    top = 100f,
                    bottom = 99f,
                )
            )
        }
}
