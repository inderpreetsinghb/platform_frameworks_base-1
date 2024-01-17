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

package com.android.systemui.statusbar.notification.icon.ui.viewbinder

import com.android.systemui.common.ui.configurationState
import com.android.systemui.kosmos.Kosmos
import com.android.systemui.kosmos.Kosmos.Fixture
import com.android.systemui.statusbar.notification.icon.ui.viewmodel.notificationIconContainerShelfViewModel
import com.android.systemui.statusbar.notification.stack.ui.viewbinder.notifCollection
import com.android.systemui.statusbar.ui.systemBarUtilsState

val Kosmos.notificationIconContainerShelfViewBinder by Fixture {
    NotificationIconContainerShelfViewBinder(
        notificationIconContainerShelfViewModel,
        configurationState,
        systemBarUtilsState,
        statusBarIconViewBindingFailureTracker,
        shelfNotificationIconViewStore,
    )
}

val Kosmos.shelfNotificationIconViewStore by Fixture {
    ShelfNotificationIconViewStore(notifCollection = notifCollection)
}
