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

package android.app.servertransaction;

import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.android.window.flags.Flags.FLAG_BUNDLE_CLIENT_TRANSACTION_FLAG;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.IDisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.DisplayInfo;
import android.window.ActivityWindowInfo;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.window.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.BiConsumer;

/**
 * Tests for {@link ClientTransactionListenerController}.
 *
 * Build/Install/Run:
 *  atest FrameworksCoreTests:ClientTransactionListenerControllerTest
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
@Presubmit
public class ClientTransactionListenerControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    @Mock
    private IDisplayManager mIDisplayManager;
    @Mock
    private DisplayManager.DisplayListener mListener;
    @Mock
    private BiConsumer<IBinder, ActivityWindowInfo> mActivityWindowInfoListener;
    @Mock
    private IBinder mActivityToken;

    private DisplayManagerGlobal mDisplayManager;
    private Handler mHandler;
    private ClientTransactionListenerController mController;

    @Before
    public void setup() {
        mSetFlagsRule.enableFlags(FLAG_BUNDLE_CLIENT_TRANSACTION_FLAG);

        MockitoAnnotations.initMocks(this);
        mDisplayManager = new DisplayManagerGlobal(mIDisplayManager);
        mHandler = getInstrumentation().getContext().getMainThreadHandler();
        mController = ClientTransactionListenerController.createInstanceForTesting(mDisplayManager);
    }

    @Test
    public void testOnDisplayChanged() throws RemoteException {
        // Mock IDisplayManager to return a display info to trigger display change.
        final DisplayInfo newDisplayInfo = new DisplayInfo();
        doReturn(newDisplayInfo).when(mIDisplayManager).getDisplayInfo(123);

        mDisplayManager.registerDisplayListener(mListener, mHandler,
                DisplayManager.EVENT_FLAG_DISPLAY_CHANGED, null /* packageName */);

        mController.onDisplayChanged(123);
        mHandler.runWithScissors(() -> { }, 0);

        verify(mListener).onDisplayChanged(123);
    }

    @Test
    public void testActivityWindowInfoChangedListener() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ACTIVITY_WINDOW_INFO_FLAG);

        mController.registerActivityWindowInfoChangedListener(mActivityWindowInfoListener);
        final ActivityWindowInfo activityWindowInfo = new ActivityWindowInfo();
        activityWindowInfo.set(true /* isEmbedded */, new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));
        mController.onActivityWindowInfoChanged(mActivityToken, activityWindowInfo);

        verify(mActivityWindowInfoListener).accept(mActivityToken, activityWindowInfo);

        clearInvocations(mActivityWindowInfoListener);
        mController.unregisterActivityWindowInfoChangedListener(mActivityWindowInfoListener);

        mController.onActivityWindowInfoChanged(mActivityToken, activityWindowInfo);

        verify(mActivityWindowInfoListener, never()).accept(any(), any());
    }
}
