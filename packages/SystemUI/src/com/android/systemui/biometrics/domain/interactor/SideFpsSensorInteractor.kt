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

package com.android.systemui.biometrics.domain.interactor

import android.content.Context
import android.hardware.biometrics.SensorLocationInternal
import android.view.WindowManager
import com.android.systemui.biometrics.FingerprintInteractiveToAuthProvider
import com.android.systemui.biometrics.data.repository.FingerprintPropertyRepository
import com.android.systemui.biometrics.domain.model.SideFpsSensorLocation
import com.android.systemui.biometrics.shared.model.DisplayRotation
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.isDefaultOrientation
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.log.SideFpsLogger
import com.android.systemui.res.R
import java.util.Optional
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@SysUISingleton
class SideFpsSensorInteractor
@Inject
constructor(
    private val context: Context,
    fingerprintPropertyRepository: FingerprintPropertyRepository,
    windowManager: WindowManager,
    displayStateInteractor: DisplayStateInteractor,
    fingerprintInteractiveToAuthProvider: Optional<FingerprintInteractiveToAuthProvider>,
    private val logger: SideFpsLogger,
) {

    private val sensorLocationForCurrentDisplay =
        combine(
                displayStateInteractor.displayChanges,
                fingerprintPropertyRepository.sensorLocations,
                ::Pair
            )
            .map { (_, locations) -> locations[context.display?.uniqueId] }
            .filterNotNull()

    val isAvailable: Flow<Boolean> =
        fingerprintPropertyRepository.sensorType.map { it == FingerprintSensorType.POWER_BUTTON }

    val authenticationDuration: Long =
        context.resources?.getInteger(R.integer.config_restToUnlockDuration)?.toLong() ?: 0L

    val isProlongedTouchRequiredForAuthentication: Flow<Boolean> =
        if (fingerprintInteractiveToAuthProvider.isEmpty) {
            flowOf(false)
        } else {
            combine(
                isAvailable,
                fingerprintInteractiveToAuthProvider.get().enabledForCurrentUser
            ) { sfpsAvailable, isSettingEnabled ->
                sfpsAvailable && isSettingEnabled
            }
        }

    val sensorLocation: Flow<SideFpsSensorLocation> =
        combine(displayStateInteractor.currentRotation, sensorLocationForCurrentDisplay, ::Pair)
            .map { (rotation, sensorLocation: SensorLocationInternal) ->
                val isSensorVerticalInDefaultOrientation = sensorLocation.sensorLocationY != 0
                // device dimensions in the current rotation
                val windowMetrics = windowManager.maximumWindowMetrics
                val size = windowMetrics.bounds
                val isDefaultOrientation = rotation.isDefaultOrientation()
                // Width and height are flipped is device is not in rotation_0 or rotation_180
                // Flipping it to the width and height of the device in default orientation.
                val displayWidth = if (isDefaultOrientation) size.width() else size.height()
                val displayHeight = if (isDefaultOrientation) size.height() else size.width()
                val sensorLengthInPx = sensorLocation.sensorRadius * 2

                val (sensorLeft, sensorTop) =
                    if (isSensorVerticalInDefaultOrientation) {
                        when (rotation) {
                            DisplayRotation.ROTATION_0 -> {
                                Pair(displayWidth, sensorLocation.sensorLocationY)
                            }
                            DisplayRotation.ROTATION_90 -> {
                                Pair(sensorLocation.sensorLocationY, 0)
                            }
                            DisplayRotation.ROTATION_180 -> {
                                Pair(
                                    0,
                                    displayHeight -
                                        sensorLocation.sensorLocationY -
                                        sensorLengthInPx
                                )
                            }
                            DisplayRotation.ROTATION_270 -> {
                                Pair(
                                    displayHeight -
                                        sensorLocation.sensorLocationY -
                                        sensorLengthInPx,
                                    displayWidth
                                )
                            }
                        }
                    } else {
                        when (rotation) {
                            DisplayRotation.ROTATION_0 -> {
                                Pair(sensorLocation.sensorLocationX, 0)
                            }
                            DisplayRotation.ROTATION_90 -> {
                                Pair(
                                    0,
                                    displayWidth - sensorLocation.sensorLocationX - sensorLengthInPx
                                )
                            }
                            DisplayRotation.ROTATION_180 -> {
                                Pair(
                                    displayWidth -
                                        sensorLocation.sensorLocationX -
                                        sensorLengthInPx,
                                    displayHeight
                                )
                            }
                            DisplayRotation.ROTATION_270 -> {
                                Pair(displayHeight, sensorLocation.sensorLocationX)
                            }
                        }
                    }

                SideFpsSensorLocation(
                    left = sensorLeft,
                    top = sensorTop,
                    length = sensorLengthInPx,
                    isSensorVerticalInDefaultOrientation = isSensorVerticalInDefaultOrientation
                )
            }
            .distinctUntilChanged()
            .onEach {
                logger.sensorLocationStateChanged(
                    it.left,
                    it.top,
                    it.length,
                    it.isSensorVerticalInDefaultOrientation
                )
            }
}
