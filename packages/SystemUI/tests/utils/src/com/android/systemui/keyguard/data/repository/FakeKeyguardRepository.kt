/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.systemui.keyguard.data.repository

import com.android.systemui.common.shared.model.Position
import com.android.systemui.keyguard.shared.model.BiometricUnlockModel
import com.android.systemui.keyguard.shared.model.StatusBarState
import com.android.systemui.keyguard.shared.model.WakefulnessModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Fake implementation of [KeyguardRepository] */
class FakeKeyguardRepository : KeyguardRepository {

    private val _animateBottomAreaDozingTransitions = MutableStateFlow(false)
    override val animateBottomAreaDozingTransitions: StateFlow<Boolean> =
        _animateBottomAreaDozingTransitions

    private val _bottomAreaAlpha = MutableStateFlow(1f)
    override val bottomAreaAlpha: StateFlow<Float> = _bottomAreaAlpha

    private val _clockPosition = MutableStateFlow(Position(0, 0))
    override val clockPosition: StateFlow<Position> = _clockPosition

    private val _isKeyguardShowing = MutableStateFlow(false)
    override val isKeyguardShowing: Flow<Boolean> = _isKeyguardShowing

    private val _isDozing = MutableStateFlow(false)
    override val isDozing: Flow<Boolean> = _isDozing

    private val _isDreaming = MutableStateFlow(false)
    override val isDreaming: Flow<Boolean> = _isDreaming

    private val _dozeAmount = MutableStateFlow(0f)
    override val dozeAmount: Flow<Float> = _dozeAmount

    private val _statusBarState = MutableStateFlow(StatusBarState.SHADE)
    override val statusBarState: Flow<StatusBarState> = _statusBarState

    private val _wakefulnessState = MutableStateFlow(WakefulnessModel.ASLEEP)
    override val wakefulnessState: Flow<WakefulnessModel> = _wakefulnessState

    private val _isUdfpsSupported = MutableStateFlow(false)

    private val _isBouncerShowing = MutableStateFlow(false)
    override val isBouncerShowing: Flow<Boolean> = _isBouncerShowing

    private val _isKeyguardGoingAway = MutableStateFlow(false)
    override val isKeyguardGoingAway: Flow<Boolean> = _isKeyguardGoingAway

    private val _biometricUnlockState = MutableStateFlow(BiometricUnlockModel.NONE)
    override val biometricUnlockState: Flow<BiometricUnlockModel> = _biometricUnlockState

    override fun isKeyguardShowing(): Boolean {
        return _isKeyguardShowing.value
    }

    override fun setAnimateDozingTransitions(animate: Boolean) {
        _animateBottomAreaDozingTransitions.tryEmit(animate)
    }

    override fun setBottomAreaAlpha(alpha: Float) {
        _bottomAreaAlpha.value = alpha
    }

    override fun setClockPosition(x: Int, y: Int) {
        _clockPosition.value = Position(x, y)
    }

    fun setKeyguardShowing(isShowing: Boolean) {
        _isKeyguardShowing.value = isShowing
    }

    fun setDozing(isDozing: Boolean) {
        _isDozing.value = isDozing
    }

    fun setDozeAmount(dozeAmount: Float) {
        _dozeAmount.value = dozeAmount
    }

    override fun isUdfpsSupported(): Boolean {
        return _isUdfpsSupported.value
    }
}
