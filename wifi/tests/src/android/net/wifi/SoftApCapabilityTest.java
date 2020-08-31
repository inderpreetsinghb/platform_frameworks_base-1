/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.net.wifi;

import android.os.Build;
import android.os.Parcel;

import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;

import org.junit.Test;

/**
 * Unit tests for {@link android.net.wifi.SoftApCapability}.
 */
@SmallTest
public class SoftApCapabilityTest {

    /**
     * Verifies copy constructor.
     */
    @Test
    public void testCopyOperator() throws Exception {
        long testSoftApFeature = SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT
                | SoftApCapability.SOFTAP_FEATURE_ACS_OFFLOAD;
        int[] testSupported2Glist = {1, 2, 3, 4};
        int[] testSupported5Glist = {36, 149};
        SoftApCapability capability = new SoftApCapability(testSoftApFeature);
        capability.setMaxSupportedClients(10);
        capability.setSupportedChannelList(SoftApConfiguration.BAND_2GHZ, testSupported2Glist);
        capability.setSupportedChannelList(SoftApConfiguration.BAND_5GHZ, testSupported5Glist);

        SoftApCapability copiedCapability = new SoftApCapability(capability);

        assertEquals(capability, copiedCapability);
        assertEquals(capability.hashCode(), copiedCapability.hashCode());
    }

    /**
     * Verifies parcel serialization/deserialization.
     */
    @Test
    public void testParcelOperation() throws Exception {
        long testSoftApFeature = SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT
                | SoftApCapability.SOFTAP_FEATURE_ACS_OFFLOAD;
        SoftApCapability capability = new SoftApCapability(testSoftApFeature);
        capability.setMaxSupportedClients(10);
        int[] testSupported2Glist = {1, 2, 3, 4};
        int[] testSupported5Glist = {36, 149};

        capability.setSupportedChannelList(SoftApConfiguration.BAND_2GHZ, testSupported2Glist);
        capability.setSupportedChannelList(SoftApConfiguration.BAND_5GHZ, testSupported5Glist);

        Parcel parcelW = Parcel.obtain();
        capability.writeToParcel(parcelW, 0);
        byte[] bytes = parcelW.marshall();
        parcelW.recycle();

        Parcel parcelR = Parcel.obtain();
        parcelR.unmarshall(bytes, 0, bytes.length);
        parcelR.setDataPosition(0);
        SoftApCapability fromParcel = SoftApCapability.CREATOR.createFromParcel(parcelR);

        assertEquals(capability, fromParcel);
        assertEquals(capability.hashCode(), fromParcel.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSupportedChannelListWithInvalidBand() {
        long testSoftApFeature = SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT
                | SoftApCapability.SOFTAP_FEATURE_ACS_OFFLOAD;
        SoftApCapability capability = new SoftApCapability(testSoftApFeature);
        capability.setSupportedChannelList(
                SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ, new int[0]);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSupportedChannelListWithInvalidBand() {
        long testSoftApFeature = SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT
                | SoftApCapability.SOFTAP_FEATURE_ACS_OFFLOAD;
        SoftApCapability capability = new SoftApCapability(testSoftApFeature);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            capability.getSupportedChannelList(
                    SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ);
        } else {
            throw new IllegalArgumentException("API doesn't support in current SDK version");
        }
    }
}
