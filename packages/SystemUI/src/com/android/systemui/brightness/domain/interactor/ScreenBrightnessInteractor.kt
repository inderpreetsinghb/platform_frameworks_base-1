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

package com.android.systemui.brightness.domain.interactor

import com.android.settingslib.display.BrightnessUtils
import com.android.systemui.brightness.data.model.LinearBrightness
import com.android.systemui.brightness.data.repository.ScreenBrightnessRepository
import com.android.systemui.brightness.shared.GammaBrightness
import com.android.systemui.dagger.SysUISingleton
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

/**
 * Converts between [GammaBrightness] and [LinearBrightness].
 *
 * @see BrightnessUtils
 */
@SysUISingleton
class ScreenBrightnessInteractor
@Inject
constructor(
    private val screenBrightnessRepository: ScreenBrightnessRepository,
) {
    /** Maximum value in the Gamma space for brightness */
    val maxGammaBrightness = GammaBrightness(BrightnessUtils.GAMMA_SPACE_MAX)

    /** Minimum value in the Gamma space for brightness */
    val minGammaBrightness = GammaBrightness(BrightnessUtils.GAMMA_SPACE_MIN)

    /**
     * Brightness in the Gamma space for the current display. It will always represent a value
     * between [minGammaBrightness] and [maxGammaBrightness]
     */
    val gammaBrightness =
        with(screenBrightnessRepository) {
            combine(
                linearBrightness,
                minLinearBrightness,
                maxLinearBrightness,
            ) { brightness, min, max ->
                brightness.toGammaBrightness(min, max)
            }
        }

    /** Sets the brightness temporarily, while the user is changing it. */
    suspend fun setTemporaryBrightness(gammaBrightness: GammaBrightness) {
        screenBrightnessRepository.setTemporaryBrightness(
            gammaBrightness.clamp().toLinearBrightness()
        )
    }

    /** Sets the brightness definitely. */
    suspend fun setBrightness(gammaBrightness: GammaBrightness) {
        screenBrightnessRepository.setBrightness(gammaBrightness.clamp().toLinearBrightness())
    }

    private suspend fun GammaBrightness.toLinearBrightness(): LinearBrightness {
        val bounds = screenBrightnessRepository.getMinMaxLinearBrightness()
        return LinearBrightness(
            BrightnessUtils.convertGammaToLinearFloat(
                value,
                bounds.first.floatValue,
                bounds.second.floatValue
            )
        )
    }

    private fun GammaBrightness.clamp(): GammaBrightness {
        return GammaBrightness(value.coerceIn(minGammaBrightness.value, maxGammaBrightness.value))
    }

    private fun LinearBrightness.toGammaBrightness(
        min: LinearBrightness,
        max: LinearBrightness,
    ): GammaBrightness {
        return GammaBrightness(
            BrightnessUtils.convertLinearToGammaFloat(floatValue, min.floatValue, max.floatValue)
        )
    }
}
