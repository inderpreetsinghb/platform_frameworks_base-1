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
 */

package android.window;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.Dialog;
import android.view.Window;

/**
 * Callback allowing applications to handle back events in place of the system.
 * <p>
 * Callback instances can be added to and removed from {@link OnBackInvokedDispatcher}, which
 * is held at window level and accessible through {@link Activity#getOnBackInvokedDispatcher()},
 * {@link Dialog#getOnBackInvokedDispatcher()} and {@link Window#getOnBackInvokedDispatcher()}.
 * <p>
 * When back is triggered, callbacks on the in-focus window are invoked in reverse order in which
 * they are added within the same priority. Between different priorities, callbacks with higher
 * priority are invoked first.
 * <p>
 * This replaces {@link Activity#onBackPressed()}, {@link Dialog#onBackPressed()} and
 * {@link android.view.KeyEvent#KEYCODE_BACK}
 * <p>
 * @see OnBackInvokedDispatcher#registerOnBackInvokedCallback(int, OnBackInvokedCallback)
 * registerOnBackInvokedCallback(priority, OnBackInvokedCallback)
 * to specify callback priority.
 */
@SuppressWarnings("deprecation")
public interface OnBackInvokedCallback {
    /**
     * Called when a back gesture has been started, or back button has been pressed down.
     *
     * @param backEvent The {@link BackEvent} containing information about the touch or
     *                  button press.
     *
     * @hide
     */
    default void onBackStarted(@NonNull BackEvent backEvent) {}

    /**
     * Called when a back gesture has been progressed.
     *
     * @param backEvent The {@link BackEvent} containing information about the latest touch point
     *                  and the progress that the back animation should seek to.
     *
     * @hide
     */
    default void onBackProgressed(@NonNull BackEvent backEvent) {}

    /**
     * Called when a back gesture has been completed and committed, or back button pressed
     * has been released and committed.
     */
    void onBackInvoked();

    /**
     * Called when a back gesture or button press has been cancelled.
     *
     * @hide
     */
    default void onBackCancelled() {}
}
