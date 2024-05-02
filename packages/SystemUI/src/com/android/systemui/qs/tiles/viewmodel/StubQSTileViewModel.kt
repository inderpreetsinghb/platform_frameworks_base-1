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

package com.android.systemui.qs.tiles.viewmodel

import android.os.UserHandle
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

object StubQSTileViewModel : QSTileViewModel {

    override val state: SharedFlow<QSTileState>
        get() = error("Don't call stubs")

    override val config: QSTileConfig
        get() = error("Don't call stubs")

    override val isAvailable: StateFlow<Boolean>
        get() = error("Don't call stubs")

    override fun onUserChanged(user: UserHandle) = error("Don't call stubs")

    override fun forceUpdate() = error("Don't call stubs")

    override fun onActionPerformed(userAction: QSTileUserAction) = error("Don't call stubs")

    override fun destroy() = error("Don't call stubs")
}
