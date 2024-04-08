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

package com.android.server.display.brightness;

import com.android.server.display.brightness.strategy.DisplayBrightnessStrategy;

import java.util.Objects;

/**
 * A wrapper class to encapsulate the request to notify the strategies about the selection of a
 * DisplayBrightnessStrategy
 */
public final class StrategySelectionNotifyRequest {
    // The strategy that was selected with the current request
    private final DisplayBrightnessStrategy mSelectedDisplayBrightnessStrategy;

    public StrategySelectionNotifyRequest(DisplayBrightnessStrategy displayBrightnessStrategy) {
        mSelectedDisplayBrightnessStrategy = displayBrightnessStrategy;
    }

    public DisplayBrightnessStrategy getSelectedDisplayBrightnessStrategy() {
        return mSelectedDisplayBrightnessStrategy;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StrategySelectionNotifyRequest)) {
            return false;
        }
        StrategySelectionNotifyRequest other = (StrategySelectionNotifyRequest) obj;
        return other.getSelectedDisplayBrightnessStrategy()
                == getSelectedDisplayBrightnessStrategy();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSelectedDisplayBrightnessStrategy);
    }
}
