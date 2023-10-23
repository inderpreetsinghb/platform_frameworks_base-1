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
package com.android.systemui.log.dagger

import javax.inject.Qualifier

/**
 * A default [com.android.systemui.log.LogBuffer] for QS tiles messages. It's used exclusively in
 * [com.android.systemui.qs.tiles.base.logging.QSTileLogger]. If you need to increase it for you
 * tile, add one to the map provided by the [QSTilesLogBuffers]
 */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class QSTilesDefaultLog
