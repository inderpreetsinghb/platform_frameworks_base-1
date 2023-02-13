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

package android.net.wifi.sharedconnectivity.app;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.net.wifi.sharedconnectivity.service.SharedConnectivityService;

import java.util.List;

/**
 * Interface for clients of {@link SharedConnectivityManager} to register for changes in network
 * status.
 *
 * @hide
 */
@SystemApi
public interface SharedConnectivityClientCallback {
    /**
     * This method is being called by {@link SharedConnectivityService} to notify of a change in the
     * list of available Tether Networks.
     * @param networks Updated Tether Network list.
     */
    void onTetherNetworksUpdated(@NonNull List<TetherNetwork> networks);

    /**
     * This method is being called by {@link SharedConnectivityService} to notify of a change in the
     * list of available Known Networks.
     * @param networks Updated Known Network list.
     */
    void onKnownNetworksUpdated(@NonNull List<KnownNetwork> networks);

    /**
     * This method is being called by {@link SharedConnectivityService} to notify of a change in the
     * state of share connectivity settings.
     * @param state The new state.
     */
    void onSharedConnectivitySettingsChanged(@NonNull SharedConnectivitySettingsState state);

    /**
     * This method is being called by {@link SharedConnectivityService} to notify of a change in the
     * status of the current tether network connection.
     * @param status The new status.
     */
    void onTetherNetworkConnectionStatusChanged(@NonNull TetherNetworkConnectionStatus status);

    /**
     * This method is being called by {@link SharedConnectivityService} to notify of a change in the
     * status of the current known network connection.
     * @param status The new status.
     */
    void onKnownNetworkConnectionStatusChanged(@NonNull KnownNetworkConnectionStatus status);
}

