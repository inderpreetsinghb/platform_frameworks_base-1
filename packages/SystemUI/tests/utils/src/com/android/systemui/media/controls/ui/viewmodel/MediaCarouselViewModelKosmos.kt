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

package com.android.systemui.media.controls.ui.viewmodel

import android.content.applicationContext
import com.android.systemui.concurrency.fakeExecutor
import com.android.systemui.kosmos.Kosmos
import com.android.systemui.kosmos.applicationCoroutineScope
import com.android.systemui.kosmos.testDispatcher
import com.android.systemui.media.controls.domain.pipeline.interactor.factory.mediaControlInteractorFactory
import com.android.systemui.media.controls.domain.pipeline.interactor.mediaCarouselInteractor
import com.android.systemui.media.controls.util.mediaFlags
import com.android.systemui.media.controls.util.mediaUiEventLogger
import com.android.systemui.statusbar.notification.collection.provider.visualStabilityProvider

val Kosmos.mediaCarouselViewModel by
    Kosmos.Fixture {
        MediaCarouselViewModel(
            applicationScope = applicationCoroutineScope,
            applicationContext = applicationContext,
            backgroundDispatcher = testDispatcher,
            backgroundExecutor = fakeExecutor,
            visualStabilityProvider = visualStabilityProvider,
            interactor = mediaCarouselInteractor,
            controlInteractorFactory = mediaControlInteractorFactory,
            recommendationsViewModel = mediaRecommendationsViewModel,
            logger = mediaUiEventLogger,
            mediaFlags = mediaFlags,
        )
    }
