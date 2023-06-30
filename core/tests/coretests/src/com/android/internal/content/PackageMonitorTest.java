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

package com.android.internal.content;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * A unit test for PackageMonitor implementation.
 */
@RunWith(AndroidJUnit4.class)
public class PackageMonitorTest {

    private static final String FAKE_PACKAGE_NAME = "com.android.internal.content.fakeapp";
    private static final int FAKE_PACKAGE_UID = 123;
    private static final int FAKE_USER_ID = 0;

    @Mock
    Context mMockContext;
    @Mock
    Handler mMockHandler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPackageMonitorMultipleRegisterThrowsException() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        spyPackageMonitor.register(mMockContext, UserHandle.ALL, false /* externalStorage */,
                mMockHandler);
        assertThat(spyPackageMonitor.getRegisteredHandler()).isEqualTo(mMockHandler);
        verify(mMockContext, times(2)).registerReceiverAsUser(any(), eq(UserHandle.ALL), any(),
                eq(null), eq(mMockHandler));

        assertThrows(IllegalStateException.class,
                () -> spyPackageMonitor.register(mMockContext, UserHandle.ALL,
                        false /* externalStorage */, mMockHandler));
    }

    @Test
    public void testPackageMonitorRegisterMultipleUnRegisterThrowsException() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        spyPackageMonitor.register(mMockContext, UserHandle.ALL, false /* externalStorage */,
                mMockHandler);
        spyPackageMonitor.unregister();

        assertThrows(IllegalStateException.class, spyPackageMonitor::unregister);
    }

    @Test
    public void testPackageMonitorNotRegisterUnRegisterThrowsException() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        assertThrows(IllegalStateException.class, spyPackageMonitor::unregister);
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventUidRemoved() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_UID_REMOVED);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1)).onUidRemoved(eq(FAKE_PACKAGE_UID));
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageSuspended() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGES_SUSPENDED);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        String [] packageList = new String[]{FAKE_PACKAGE_NAME};
        intent.putExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST, packageList);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1)).onPackagesSuspended(eq(packageList));
        verify(spyPackageMonitor, times(1)).onSomePackagesChanged();
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageUnSuspended() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGES_UNSUSPENDED);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        String [] packageList = new String[]{FAKE_PACKAGE_NAME};
        intent.putExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST, packageList);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1)).onPackagesUnsuspended(eq(packageList));
        verify(spyPackageMonitor, times(1)).onSomePackagesChanged();
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventUserStop() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_USER_STOPPED);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1)).onHandleUserStop(eq(intent), eq(FAKE_USER_ID));
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventExternalApplicationAvailable()
            throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        String [] packageList = new String[]{FAKE_PACKAGE_NAME};
        intent.putExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST, packageList);
        intent.putExtra(Intent.EXTRA_REPLACING, true);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1)).onPackagesAvailable(eq(packageList));
        verify(spyPackageMonitor, times(1)).onPackageAppeared(eq(FAKE_PACKAGE_NAME),
                eq(PackageMonitor.PACKAGE_UPDATING));
        verify(spyPackageMonitor, times(1)).onSomePackagesChanged();
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventExternalApplicationUnavailable()
            throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        String [] packageList = new String[]{FAKE_PACKAGE_NAME};
        intent.putExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST, packageList);
        intent.putExtra(Intent.EXTRA_REPLACING, true);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1)).onPackagesUnavailable(eq(packageList));
        verify(spyPackageMonitor, times(1)).onPackageDisappeared(eq(FAKE_PACKAGE_NAME),
                eq(PackageMonitor.PACKAGE_UPDATING));
        verify(spyPackageMonitor, times(1)).onSomePackagesChanged();
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageRestarted() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGE_RESTARTED);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        intent.setData(Uri.fromParts("package", FAKE_PACKAGE_NAME, null));
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1)).onHandleForceStop(eq(intent),
                eq(new String[]{FAKE_PACKAGE_NAME}), eq(FAKE_PACKAGE_UID), eq(true));
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageQueryRestarted() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        String [] packageList = new String[]{FAKE_PACKAGE_NAME};
        intent.putExtra(Intent.EXTRA_PACKAGES, packageList);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1)).onHandleForceStop(eq(intent),
                eq(packageList), eq(FAKE_PACKAGE_UID), eq(false));
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageDataClear() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGE_DATA_CLEARED);
        intent.setData(Uri.fromParts("package", FAKE_PACKAGE_NAME, null));
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1))
                .onPackageDataCleared(eq(FAKE_PACKAGE_NAME), eq(FAKE_PACKAGE_UID));
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageChanged() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGE_CHANGED);
        intent.setData(Uri.fromParts("package", FAKE_PACKAGE_NAME, null));
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        String [] packageList = new String[]{FAKE_PACKAGE_NAME};
        intent.putExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST, packageList);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1))
                .onPackageChanged(eq(FAKE_PACKAGE_NAME), eq(FAKE_PACKAGE_UID), eq(packageList));
        verify(spyPackageMonitor, times(1)).onPackageModified(eq(FAKE_PACKAGE_NAME));
        verify(spyPackageMonitor, times(1)).onSomePackagesChanged();
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageRemovedReplacing() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGE_REMOVED);
        intent.setData(Uri.fromParts("package", FAKE_PACKAGE_NAME, null));
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        intent.putExtra(Intent.EXTRA_REPLACING, true);
        intent.putExtra(Intent.EXTRA_REMOVED_FOR_ALL_USERS, true);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1))
                .onPackageUpdateStarted(eq(FAKE_PACKAGE_NAME), eq(FAKE_PACKAGE_UID));
        verify(spyPackageMonitor, times(1))
                .onPackageDisappeared(eq(FAKE_PACKAGE_NAME), eq(PackageMonitor.PACKAGE_UPDATING));
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageRemovedNotReplacing()
            throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGE_REMOVED);
        intent.setData(Uri.fromParts("package", FAKE_PACKAGE_NAME, null));
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        intent.putExtra(Intent.EXTRA_REPLACING, false);
        intent.putExtra(Intent.EXTRA_REMOVED_FOR_ALL_USERS, true);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1))
                .onPackageRemoved(eq(FAKE_PACKAGE_NAME), eq(FAKE_PACKAGE_UID));
        verify(spyPackageMonitor, times(1))
                .onPackageRemovedAllUsers(eq(FAKE_PACKAGE_NAME), eq(FAKE_PACKAGE_UID));
        verify(spyPackageMonitor, times(1)).onPackageDisappeared(eq(FAKE_PACKAGE_NAME),
                eq(PackageMonitor.PACKAGE_PERMANENT_CHANGE));
        verify(spyPackageMonitor, times(1)).onSomePackagesChanged();
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageAddReplacing() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGE_ADDED);
        intent.setData(Uri.fromParts("package", FAKE_PACKAGE_NAME, null));
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        intent.putExtra(Intent.EXTRA_REPLACING, true);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1))
                .onPackageUpdateFinished(eq(FAKE_PACKAGE_NAME), eq(FAKE_PACKAGE_UID));
        verify(spyPackageMonitor, times(1)).onPackageModified(eq(FAKE_PACKAGE_NAME));
        verify(spyPackageMonitor, times(1))
                .onPackageAppeared(eq(FAKE_PACKAGE_NAME), eq(PackageMonitor.PACKAGE_UPDATING));
        verify(spyPackageMonitor, times(1)).onSomePackagesChanged();
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    @Test
    public void testPackageMonitorDoHandlePackageEventPackageAddNotReplacing() throws Exception {
        PackageMonitor spyPackageMonitor = spy(new TestPackageMonitor());

        Intent intent = new Intent(Intent.ACTION_PACKAGE_ADDED);
        intent.setData(Uri.fromParts("package", FAKE_PACKAGE_NAME, null));
        intent.putExtra(Intent.EXTRA_USER_HANDLE, FAKE_USER_ID);
        intent.putExtra(Intent.EXTRA_UID, FAKE_PACKAGE_UID);
        intent.putExtra(Intent.EXTRA_REPLACING, false);
        spyPackageMonitor.doHandlePackageEvent(intent);

        verify(spyPackageMonitor, times(1)).onBeginPackageChanges();
        verify(spyPackageMonitor, times(1))
                .onPackageAdded(eq(FAKE_PACKAGE_NAME), eq(FAKE_PACKAGE_UID));
        verify(spyPackageMonitor, times(1)).onPackageAppeared(eq(FAKE_PACKAGE_NAME),
                eq(PackageMonitor.PACKAGE_PERMANENT_CHANGE));
        verify(spyPackageMonitor, times(1)).onSomePackagesChanged();
        verify(spyPackageMonitor, times(1)).onFinishPackageChanges();
    }

    public static class TestPackageMonitor extends PackageMonitor {
    }
}
