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

package android.os

import android.testing.TestableLooper
import com.android.systemui.kosmos.Kosmos
import com.android.systemui.kosmos.Kosmos.Fixture
import com.android.systemui.kosmos.testCase

val Kosmos.looper by Fixture {
    val testableLooper = TestableLooper.get(testCase)
    checkNotNull(testableLooper) {
        "TestableLooper is null, make sure the test class is annotated with RunWithLooper"
    }
    checkNotNull(testableLooper.looper) {
        "TestableLooper.getLooper() is returning null, make sure the test class is annotated " +
            "with RunWithLooper"
    }
}
