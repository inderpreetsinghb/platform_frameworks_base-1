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

package com.android.systemui.statusbar.pipeline.mobile.domain.interactor

import android.telephony.CarrierConfigManager
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import com.android.settingslib.SignalIcon.MobileIconGroup
import com.android.settingslib.mobile.MobileMappings
import com.android.settingslib.mobile.TelephonyIcons.NOT_DEFAULT_DATA
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.log.table.TableLogBuffer
import com.android.systemui.statusbar.pipeline.mobile.data.model.DataConnectionState.Connected
import com.android.systemui.statusbar.pipeline.mobile.data.model.MobileConnectionModel
import com.android.systemui.statusbar.pipeline.mobile.data.model.MobileConnectivityModel
import com.android.systemui.statusbar.pipeline.mobile.data.model.NetworkNameModel
import com.android.systemui.statusbar.pipeline.mobile.data.model.ResolvedNetworkType
import com.android.systemui.statusbar.pipeline.mobile.data.model.ResolvedNetworkType.DefaultNetworkType
import com.android.systemui.statusbar.pipeline.mobile.data.model.ResolvedNetworkType.OverrideNetworkType
import com.android.systemui.statusbar.pipeline.mobile.data.repository.MobileConnectionRepository
import com.android.systemui.statusbar.pipeline.shared.data.model.DataActivityModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

interface MobileIconInteractor {
    /** The table log created for this connection */
    val tableLogBuffer: TableLogBuffer

    /** The current mobile data activity */
    val activity: Flow<DataActivityModel>

    /**
     * This bit is meant to be `true` if and only if the default network capabilities (see
     * [android.net.ConnectivityManager.registerDefaultNetworkCallback]) result in a network that
     * has the [android.net.NetworkCapabilities.TRANSPORT_CELLULAR] represented.
     *
     * Note that this differs from [isDataConnected], which is tracked by telephony and has to do
     * with the state of using this mobile connection for data as opposed to just voice. It is
     * possible for a mobile subscription to be connected but not be in a connected data state, and
     * thus we wouldn't want to show the network type icon.
     */
    val isConnected: Flow<Boolean>

    /**
     * True when telephony tells us that the data state is CONNECTED. See
     * [android.telephony.TelephonyCallback.DataConnectionStateListener] for more details. We
     * consider this connection to be serving data, and thus want to show a network type icon, when
     * data is connected. Other data connection states would typically cause us not to show the icon
     */
    val isDataConnected: StateFlow<Boolean>

    /** Only true if mobile is the default transport but is not validated, otherwise false */
    val isDefaultConnectionFailed: StateFlow<Boolean>

    /** True if we consider this connection to be in service, i.e. can make calls */
    val isInService: StateFlow<Boolean>

    // TODO(b/256839546): clarify naming of default vs active
    /** True if we want to consider the data connection enabled */
    val isDefaultDataEnabled: StateFlow<Boolean>

    /** Observable for the data enabled state of this connection */
    val isDataEnabled: StateFlow<Boolean>

    /** True if the RAT icon should always be displayed and false otherwise. */
    val alwaysShowDataRatIcon: StateFlow<Boolean>

    /** True if the CDMA level should be preferred over the primary level. */
    val alwaysUseCdmaLevel: StateFlow<Boolean>

    /** Observable for RAT type (network type) indicator */
    val networkTypeIconGroup: StateFlow<MobileIconGroup>

    /**
     * Provider name for this network connection. The name can be one of 3 values:
     * 1. The default network name, if one is configured
     * 2. A derived name based off of the intent [ACTION_SERVICE_PROVIDERS_UPDATED]
     * 3. Or, in the case where the repository sends us the default network name, we check for an
     *    override in [connectionInfo.operatorAlphaShort], a value that is derived from
     *    [ServiceState]
     */
    val networkName: StateFlow<NetworkNameModel>

    /** True if this line of service is emergency-only */
    val isEmergencyOnly: StateFlow<Boolean>

    /**
     * True if this connection is considered roaming. The roaming bit can come from [ServiceState],
     * or directly from the telephony manager's CDMA ERI number value. Note that we don't consider a
     * connection to be roaming while carrier network change is active
     */
    val isRoaming: StateFlow<Boolean>

    /** Int describing the connection strength. 0-4 OR 1-5. See [numberOfLevels] */
    val level: StateFlow<Int>

    /** Based on [CarrierConfigManager.KEY_INFLATE_SIGNAL_STRENGTH_BOOL], either 4 or 5 */
    val numberOfLevels: StateFlow<Int>

    /** See [MobileIconsInteractor.isForceHidden]. */
    val isForceHidden: Flow<Boolean>

    /** True if the rsrp level should be preferred over the primary level for LTE. */
    val alwaysUseRsrpLevelForLte: StateFlow<Boolean>

    /** True if the no internet icon should be hidden.  */
    val hideNoInternetState: StateFlow<Boolean>

    val imsInfo: StateFlow<MobileConnectionModel>

    val showVolteIcon: StateFlow<Boolean>
}

/** Interactor for a single mobile connection. This connection _should_ have one subscription ID */
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalCoroutinesApi::class)
class MobileIconInteractorImpl(
    @Application scope: CoroutineScope,
    defaultSubscriptionHasDataEnabled: StateFlow<Boolean>,
    override val alwaysShowDataRatIcon: StateFlow<Boolean>,
    override val alwaysUseCdmaLevel: StateFlow<Boolean>,
    defaultMobileConnectivity: StateFlow<MobileConnectivityModel>,
    defaultMobileIconMapping: StateFlow<Map<String, MobileIconGroup>>,
    defaultMobileIconGroup: StateFlow<MobileIconGroup>,
    defaultDataSubId: StateFlow<Int>,
    override val isDefaultConnectionFailed: StateFlow<Boolean>,
    override val isForceHidden: Flow<Boolean>,
    connectionRepository: MobileConnectionRepository,
    override val alwaysUseRsrpLevelForLte: StateFlow<Boolean>,
    override val hideNoInternetState: StateFlow<Boolean>,
    override val showVolteIcon: StateFlow<Boolean>,
) : MobileIconInteractor {
    private val connectionInfo = connectionRepository.connectionInfo

    override val tableLogBuffer: TableLogBuffer = connectionRepository.tableLogBuffer

    override val activity = connectionInfo.mapLatest { it.dataActivityDirection }

    override val isConnected: Flow<Boolean> = defaultMobileConnectivity.mapLatest { it.isConnected }

    override val isDataEnabled: StateFlow<Boolean> = connectionRepository.dataEnabled

    override val imsInfo: StateFlow<MobileConnectionModel> = connectionRepository.imsInfo

    private val isDefault =
        defaultDataSubId
            .mapLatest { connectionRepository.subId == it }
            .stateIn(
                scope,
                SharingStarted.WhileSubscribed(),
                connectionRepository.subId == defaultDataSubId.value
            )

    override val isDefaultDataEnabled = defaultSubscriptionHasDataEnabled

    override val networkName =
        combine(connectionInfo, connectionRepository.networkName) { connection, networkName ->
                if (
                    networkName is NetworkNameModel.Default && connection.operatorAlphaShort != null
                ) {
                    NetworkNameModel.IntentDerived(connection.operatorAlphaShort)
                } else {
                    networkName
                }
            }
            .stateIn(
                scope,
                SharingStarted.WhileSubscribed(),
                connectionRepository.networkName.value
            )

    /** Observable for the current RAT indicator icon ([MobileIconGroup]) */
    override val networkTypeIconGroup: StateFlow<MobileIconGroup> =
        combine(
                connectionInfo,
                defaultMobileIconMapping,
                defaultMobileIconGroup,
                isDefault,
            ) { info, mapping, defaultGroup, isDefault ->
                if (!isDefault) {
                    return@combine NOT_DEFAULT_DATA
                }

                when (info.resolvedNetworkType) {
                    is ResolvedNetworkType.CarrierMergedNetworkType ->
                        info.resolvedNetworkType.iconGroupOverride
                    else -> getMobileIconGroup(info, mapping) ?: defaultGroup
                }
            }
            .distinctUntilChanged()
            .onEach {
                // Doesn't use [logDiffsForTable] because [MobileIconGroup] can't implement the
                // [Diffable] interface.
                tableLogBuffer.logChange(
                    prefix = "",
                    columnName = "networkTypeIcon",
                    value = it.name
                )
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), defaultMobileIconGroup.value)

    override val isEmergencyOnly: StateFlow<Boolean> =
        connectionInfo
            .mapLatest { it.isEmergencyOnly }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val isRoaming: StateFlow<Boolean> =
        combine(connectionInfo, connectionRepository.cdmaRoaming) { connection, cdmaRoaming ->
                if (connection.carrierNetworkChangeActive) {
                    false
                } else if (connection.isGsm) {
                    connection.isRoaming
                } else {
                    cdmaRoaming
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val level: StateFlow<Int> =
        combine(
            connectionInfo,
            alwaysUseCdmaLevel,
            alwaysUseRsrpLevelForLte
        ) { connection, alwaysUseCdmaLevel, alwaysUseRsrpLevelForLte ->
                when {
                    alwaysUseRsrpLevelForLte -> {
                        if (isLteCamped(connection)) {
                            connection.lteRsrpLevel
                        } else {
                            connection.primaryLevel
                        }
                    }
                    // GSM connections should never use the CDMA level
                    connection.isGsm -> connection.primaryLevel
                    alwaysUseCdmaLevel -> connection.cdmaLevel
                    else -> connection.primaryLevel
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), 0)

    override val numberOfLevels: StateFlow<Int> =
        connectionRepository.numberOfLevels.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            connectionRepository.numberOfLevels.value,
        )

    override val isDataConnected: StateFlow<Boolean> =
        connectionInfo
            .mapLatest { connection -> connection.dataConnectionState == Connected }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val isInService =
        connectionRepository.connectionInfo
            .mapLatest { it.isInService }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private fun isLteCamped(connectionInfo: MobileConnectionModel): Boolean {
        return (connectionInfo.dataNetworkType == TelephonyManager.NETWORK_TYPE_LTE
            || connectionInfo.dataNetworkType == TelephonyManager.NETWORK_TYPE_LTE_CA
            || connectionInfo.voiceNetworkType == TelephonyManager.NETWORK_TYPE_LTE
            || connectionInfo.voiceNetworkType == TelephonyManager.NETWORK_TYPE_LTE_CA)
    }

    private fun getMobileIconGroup(info: MobileConnectionModel,
                                   mapping: Map<String, MobileIconGroup>): MobileIconGroup ?{
        return if (info.fiveGServiceState.isNrIconTypeValid) {
            info.fiveGServiceState.iconGroup
        } else {
            when (info.resolvedNetworkType) {
                is DefaultNetworkType ->
                    mapping[info.resolvedNetworkType.lookupKey]
                is OverrideNetworkType ->
                    mapping[getLookupKey(info)]
                else ->
                    mapping[MobileMappings.toIconKey(info.voiceNetworkType)]
            }
        }
    }

    private fun getLookupKey(connectionInfo: MobileConnectionModel): String {
        return if (isNsa(connectionInfo)) {
            if (connectionInfo.dataNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                MobileMappings.toIconKey(connectionInfo.voiceNetworkType)
            }else {
                MobileMappings.toIconKey(connectionInfo.dataNetworkType)
            }
        }else {
            connectionInfo.resolvedNetworkType.lookupKey
        }
    }

    private fun isNsa(connectionInfo: MobileConnectionModel): Boolean {
        val networkType = connectionInfo.resolvedNetworkType.networkType
        return networkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE
                || networkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
    }
}
