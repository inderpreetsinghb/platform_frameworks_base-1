/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.systemui.surfaceeffects.gloweffect

import android.graphics.Color
import android.graphics.RenderEffect
import android.testing.AndroidTestingRunner
import android.testing.TestableLooper
import androidx.test.filters.SmallTest
import com.android.systemui.animation.AnimatorTestRule
import com.android.systemui.model.SysUiStateTest
import com.android.systemui.surfaceeffects.RenderEffectDrawCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidTestingRunner::class)
@TestableLooper.RunWithLooper(setAsMainLooper = true)
class GlowPieEffectTest : SysUiStateTest() {

    @get:Rule val animatorTestRule = AnimatorTestRule(this)

    @Test
    fun play_triggersDrawCallback() {
        var effectFromCallback: RenderEffect? = null
        val glowPieEffectConfig =
            GlowPieEffectConfig(
                centerX = 0f,
                centerY = 0f,
                width = 1f,
                height = 1f,
                cornerRadius = 0.5f,
                colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE)
            )
        val drawCallback =
            object : RenderEffectDrawCallback {
                override fun onDraw(renderEffect: RenderEffect) {
                    effectFromCallback = renderEffect
                }
            }
        val glowPieEffect = GlowPieEffect(glowPieEffectConfig, drawCallback)

        assertThat(effectFromCallback).isNull()

        glowPieEffect.play()

        animatorTestRule.advanceTimeBy(100L)

        assertThat(effectFromCallback).isNotNull()
    }

    @Test
    fun finish_cancelsAnimator() {
        val glowPieEffectConfig =
            GlowPieEffectConfig(
                centerX = 0f,
                centerY = 0f,
                width = 1f,
                height = 1f,
                cornerRadius = 0.5f,
                colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE)
            )
        val drawCallback =
            object : RenderEffectDrawCallback {
                override fun onDraw(renderEffect: RenderEffect) {}
            }
        val glowPieEffect = GlowPieEffect(glowPieEffectConfig, drawCallback)

        glowPieEffect.play()
        animatorTestRule.advanceTimeBy(100L)

        assertThat(glowPieEffect.mainAnimator.isRunning).isTrue()

        glowPieEffect.finish()

        assertThat(glowPieEffect.mainAnimator.isRunning).isFalse()
    }
}
