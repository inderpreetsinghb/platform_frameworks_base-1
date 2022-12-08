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

package com.android.server.input;

import android.annotation.BinderThread;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.hardware.BatteryState;
import android.hardware.input.IInputDeviceBatteryListener;
import android.hardware.input.IInputDeviceBatteryState;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UEventObserver;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.view.InputDevice;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.input.BatteryController.UEventManager.UEventBatteryListener;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A thread-safe component of {@link InputManagerService} responsible for managing the battery state
 * of input devices.
 *
 * Interactions with BatteryController can happen on several threads, including Binder threads, the
 * {@link UEventObserver}'s thread, or its own Handler thread, among others. All public methods, and
 * private methods prefixed with "handle-" (e.g. {@link #handleListeningProcessDied(int)}),
 * serve as entry points for these threads.
 */
final class BatteryController {
    private static final String TAG = BatteryController.class.getSimpleName();

    // To enable these logs, run:
    // 'adb shell setprop log.tag.BatteryController DEBUG' (requires restart)
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    static final long POLLING_PERIOD_MILLIS = 10_000; // 10 seconds
    @VisibleForTesting
    static final long USI_BATTERY_VALIDITY_DURATION_MILLIS = 60 * 60_000; // 1 hour

    private final Object mLock = new Object();
    private final Context mContext;
    private final NativeInputManagerService mNative;
    private final Handler mHandler;
    private final UEventManager mUEventManager;

    // Maps a pid to the registered listener record for that process. There can only be one battery
    // listener per process.
    @GuardedBy("mLock")
    private final ArrayMap<Integer, ListenerRecord> mListenerRecords = new ArrayMap<>();

    // Maps a deviceId that is being monitored to the monitor for the battery state of the device.
    @GuardedBy("mLock")
    private final ArrayMap<Integer, DeviceMonitor> mDeviceMonitors = new ArrayMap<>();

    @GuardedBy("mLock")
    private boolean mIsPolling = false;
    @GuardedBy("mLock")
    private boolean mIsInteractive = true;

    BatteryController(Context context, NativeInputManagerService nativeService, Looper looper) {
        this(context, nativeService, looper, new UEventManager() {});
    }

    @VisibleForTesting
    BatteryController(Context context, NativeInputManagerService nativeService, Looper looper,
            UEventManager uEventManager) {
        mContext = context;
        mNative = nativeService;
        mHandler = new Handler(looper);
        mUEventManager = uEventManager;
    }

    public void systemRunning() {
        final InputManager inputManager =
                Objects.requireNonNull(mContext.getSystemService(InputManager.class));
        inputManager.registerInputDeviceListener(mInputDeviceListener, mHandler);
        for (int deviceId : inputManager.getInputDeviceIds()) {
            mInputDeviceListener.onInputDeviceAdded(deviceId);
        }
    }

    /**
     * Register the battery listener for the given input device and start monitoring its battery
     * state.
     */
    @BinderThread
    public void registerBatteryListener(int deviceId, @NonNull IInputDeviceBatteryListener listener,
            int pid) {
        synchronized (mLock) {
            ListenerRecord listenerRecord = mListenerRecords.get(pid);

            if (listenerRecord == null) {
                listenerRecord = new ListenerRecord(pid, listener);
                try {
                    listener.asBinder().linkToDeath(listenerRecord.mDeathRecipient, 0);
                } catch (RemoteException e) {
                    Slog.i(TAG, "Client died before battery listener could be registered.");
                    return;
                }
                mListenerRecords.put(pid, listenerRecord);
                if (DEBUG) Slog.d(TAG, "Battery listener added for pid " + pid);
            }

            if (listenerRecord.mListener.asBinder() != listener.asBinder()) {
                throw new SecurityException(
                        "Cannot register a new battery listener when there is already another "
                                + "registered listener for pid "
                                + pid);
            }
            if (!listenerRecord.mMonitoredDevices.add(deviceId)) {
                throw new IllegalArgumentException(
                        "The battery listener for pid " + pid
                                + " is already monitoring deviceId " + deviceId);
            }

            DeviceMonitor monitor = mDeviceMonitors.get(deviceId);
            if (monitor == null) {
                // This is the first listener that is monitoring this device.
                monitor = new DeviceMonitor(deviceId);
                mDeviceMonitors.put(deviceId, monitor);
            }

            if (DEBUG) {
                Slog.d(TAG, "Battery listener for pid " + pid
                        + " is monitoring deviceId " + deviceId);
            }

            updatePollingLocked(true /*delayStart*/);
            notifyBatteryListener(listenerRecord, monitor.getBatteryStateForReporting());
        }
    }

    private static void notifyBatteryListener(ListenerRecord listenerRecord, State state) {
        try {
            listenerRecord.mListener.onBatteryStateChanged(state);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to notify listener", e);
        }
        if (DEBUG) {
            Slog.d(TAG, "Notified battery listener from pid " + listenerRecord.mPid
                    + " of state of deviceId " + state.deviceId);
        }
    }

    private void notifyAllListenersForDevice(State state) {
        synchronized (mLock) {
            if (DEBUG) Slog.d(TAG, "Notifying all listeners of battery state: " + state);
            mListenerRecords.forEach((pid, listenerRecord) -> {
                if (listenerRecord.mMonitoredDevices.contains(state.deviceId)) {
                    notifyBatteryListener(listenerRecord, state);
                }
            });
        }
    }

    @GuardedBy("mLock")
    private void updatePollingLocked(boolean delayStart) {
        if (!mIsInteractive || !anyOf(mDeviceMonitors, DeviceMonitor::requiresPolling)) {
            // Stop polling.
            mIsPolling = false;
            mHandler.removeCallbacks(this::handlePollEvent);
            return;
        }

        if (mIsPolling) {
            return;
        }
        // Start polling.
        mIsPolling = true;
        mHandler.postDelayed(this::handlePollEvent, delayStart ? POLLING_PERIOD_MILLIS : 0);
    }

    private String getInputDeviceName(int deviceId) {
        final InputDevice device =
                Objects.requireNonNull(mContext.getSystemService(InputManager.class))
                        .getInputDevice(deviceId);
        return device != null ? device.getName() : "<none>";
    }

    private boolean hasBattery(int deviceId) {
        final InputDevice device =
                Objects.requireNonNull(mContext.getSystemService(InputManager.class))
                        .getInputDevice(deviceId);
        return device != null && device.hasBattery();
    }

    private boolean isUsiDevice(int deviceId) {
        final InputDevice device =
                Objects.requireNonNull(mContext.getSystemService(InputManager.class))
                        .getInputDevice(deviceId);
        return device != null && device.supportsUsi();
    }

    @GuardedBy("mLock")
    private DeviceMonitor getDeviceMonitorOrThrowLocked(int deviceId) {
        return Objects.requireNonNull(mDeviceMonitors.get(deviceId),
                "Maps are out of sync: Cannot find device state for deviceId " + deviceId);
    }

    /**
     * Unregister the battery listener for the given input device and stop monitoring its battery
     * state. If there are no other input devices that this listener is monitoring, the listener is
     * removed.
     */
    @BinderThread
    public void unregisterBatteryListener(int deviceId,
            @NonNull IInputDeviceBatteryListener listener, int pid) {
        synchronized (mLock) {
            final ListenerRecord listenerRecord = mListenerRecords.get(pid);
            if (listenerRecord == null) {
                throw new IllegalArgumentException(
                        "Cannot unregister battery callback: No listener registered for pid "
                                + pid);
            }

            if (listenerRecord.mListener.asBinder() != listener.asBinder()) {
                throw new IllegalArgumentException(
                        "Cannot unregister battery callback: The listener is not the one that "
                                + "is registered for pid "
                                + pid);
            }

            if (!listenerRecord.mMonitoredDevices.contains(deviceId)) {
                throw new IllegalArgumentException(
                        "Cannot unregister battery callback: The device is not being "
                                + "monitored for deviceId " + deviceId);
            }

            unregisterRecordLocked(listenerRecord, deviceId);
        }
    }

    @GuardedBy("mLock")
    private void unregisterRecordLocked(ListenerRecord listenerRecord, int deviceId) {
        final int pid = listenerRecord.mPid;

        if (!listenerRecord.mMonitoredDevices.remove(deviceId)) {
            throw new IllegalStateException("Cannot unregister battery callback: The deviceId "
                    + deviceId
                    + " is not being monitored by pid "
                    + pid);
        }

        if (!hasRegisteredListenerForDeviceLocked(deviceId)) {
            // There are no more listeners monitoring this device.
            final DeviceMonitor monitor = getDeviceMonitorOrThrowLocked(deviceId);
            if (!monitor.isPersistent()) {
                monitor.onMonitorDestroy();
                mDeviceMonitors.remove(deviceId);
            }
        }

        if (listenerRecord.mMonitoredDevices.isEmpty()) {
            // There are no more devices being monitored by this listener.
            listenerRecord.mListener.asBinder().unlinkToDeath(listenerRecord.mDeathRecipient, 0);
            mListenerRecords.remove(pid);
            if (DEBUG) Slog.d(TAG, "Battery listener removed for pid " + pid);
        }

        updatePollingLocked(false /*delayStart*/);
    }

    @GuardedBy("mLock")
    private boolean hasRegisteredListenerForDeviceLocked(int deviceId) {
        for (int i = 0; i < mListenerRecords.size(); i++) {
            if (mListenerRecords.valueAt(i).mMonitoredDevices.contains(deviceId)) {
                return true;
            }
        }
        return false;
    }

    private void handleListeningProcessDied(int pid) {
        synchronized (mLock) {
            final ListenerRecord listenerRecord = mListenerRecords.get(pid);
            if (listenerRecord == null) {
                return;
            }
            if (DEBUG) {
                Slog.d(TAG,
                        "Removing battery listener for pid " + pid + " because the process died");
            }
            for (final int deviceId : listenerRecord.mMonitoredDevices) {
                unregisterRecordLocked(listenerRecord, deviceId);
            }
        }
    }

    private void handleUEventNotification(int deviceId, long eventTime) {
        synchronized (mLock) {
            final DeviceMonitor monitor = mDeviceMonitors.get(deviceId);
            if (monitor == null) {
                return;
            }
            monitor.onUEvent(eventTime);
        }
    }

    private void handlePollEvent() {
        synchronized (mLock) {
            if (!mIsPolling) {
                return;
            }
            final long eventTime = SystemClock.uptimeMillis();
            mDeviceMonitors.forEach((deviceId, monitor) -> monitor.onPoll(eventTime));
            mHandler.postDelayed(this::handlePollEvent, POLLING_PERIOD_MILLIS);
        }
    }

    private void handleMonitorTimeout(int deviceId) {
        synchronized (mLock) {
            final DeviceMonitor monitor = mDeviceMonitors.get(deviceId);
            if (monitor == null) {
                return;
            }
            final long updateTime = SystemClock.uptimeMillis();
            monitor.onTimeout(updateTime);
        }
    }

    /** Gets the current battery state of an input device. */
    public IInputDeviceBatteryState getBatteryState(int deviceId) {
        synchronized (mLock) {
            final long updateTime = SystemClock.uptimeMillis();
            final DeviceMonitor monitor = mDeviceMonitors.get(deviceId);
            if (monitor == null) {
                // The input device's battery is not being monitored by any listener.
                return queryBatteryStateFromNative(deviceId, updateTime, hasBattery(deviceId));
            }
            // Force the battery state to update, and notify listeners if necessary.
            monitor.onPoll(updateTime);
            return monitor.getBatteryStateForReporting();
        }
    }

    public void onInteractiveChanged(boolean interactive) {
        synchronized (mLock) {
            mIsInteractive = interactive;
            updatePollingLocked(false /*delayStart*/);
        }
    }

    public void notifyStylusGestureStarted(int deviceId, long eventTime) {
        synchronized (mLock) {
            final DeviceMonitor monitor = mDeviceMonitors.get(deviceId);
            if (monitor == null) {
                return;
            }

            monitor.onStylusGestureStarted(eventTime);
        }
    }

    public void dump(PrintWriter pw) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw);
        synchronized (mLock) {
            ipw.println(TAG + ":");
            ipw.increaseIndent();
            ipw.println("State: Polling = " + mIsPolling
                    + ", Interactive = " + mIsInteractive);

            ipw.println("Listeners: " + mListenerRecords.size() + " battery listeners");
            ipw.increaseIndent();
            for (int i = 0; i < mListenerRecords.size(); i++) {
                ipw.println(i + ": " + mListenerRecords.valueAt(i));
            }
            ipw.decreaseIndent();

            ipw.println("Device Monitors: " + mDeviceMonitors.size() + " monitors");
            ipw.increaseIndent();
            for (int i = 0; i < mDeviceMonitors.size(); i++) {
                ipw.println(i + ": " + mDeviceMonitors.valueAt(i));
            }
            ipw.decreaseIndent();
            ipw.decreaseIndent();
        }
    }

    @SuppressWarnings("all")
    public void monitor() {
        synchronized (mLock) {
            return;
        }
    }

    private final InputManager.InputDeviceListener mInputDeviceListener =
            new InputManager.InputDeviceListener() {
        @Override
        public void onInputDeviceAdded(int deviceId) {
            synchronized (mLock) {
                if (isUsiDevice(deviceId) && !mDeviceMonitors.containsKey(deviceId)) {
                    // Start monitoring USI device immediately.
                    mDeviceMonitors.put(deviceId, new UsiDeviceMonitor(deviceId));
                }
            }
        }

        @Override
        public void onInputDeviceRemoved(int deviceId) {}

        @Override
        public void onInputDeviceChanged(int deviceId) {
            synchronized (mLock) {
                final DeviceMonitor monitor = mDeviceMonitors.get(deviceId);
                if (monitor == null) {
                    return;
                }
                final long eventTime = SystemClock.uptimeMillis();
                monitor.onConfiguration(eventTime);
            }
        }
    };

    // A record of a registered battery listener from one process.
    private class ListenerRecord {
        public final int mPid;
        public final IInputDeviceBatteryListener mListener;
        public final IBinder.DeathRecipient mDeathRecipient;
        // The set of deviceIds that are currently being monitored by this listener.
        public final Set<Integer> mMonitoredDevices;

        ListenerRecord(int pid, IInputDeviceBatteryListener listener) {
            mPid = pid;
            mListener = listener;
            mMonitoredDevices = new ArraySet<>();
            mDeathRecipient = () -> handleListeningProcessDied(pid);
        }

        @Override
        public String toString() {
            return "pid=" + mPid
                    + ", monitored devices=" + Arrays.toString(mMonitoredDevices.toArray());
        }
    }

    // Queries the battery state of an input device from native code.
    private State queryBatteryStateFromNative(int deviceId, long updateTime, boolean isPresent) {
        return new State(
                deviceId,
                updateTime,
                isPresent,
                isPresent ? mNative.getBatteryStatus(deviceId) : BatteryState.STATUS_UNKNOWN,
                isPresent ? mNative.getBatteryCapacity(deviceId) / 100.f : Float.NaN);
    }

    // Holds the state of an InputDevice for which battery changes are currently being monitored.
    private class DeviceMonitor {
        protected final State mState;
        // Represents whether the input device has a sysfs battery node.
        protected boolean mHasBattery = false;

        @Nullable
        private UEventBatteryListener mUEventBatteryListener;

        DeviceMonitor(int deviceId) {
            mState = new State(deviceId);

            // Load the initial battery state and start monitoring.
            final long eventTime = SystemClock.uptimeMillis();
            configureDeviceMonitor(eventTime);
        }

        protected void processChangesAndNotify(long eventTime, Consumer<Long> changes) {
            final State oldState = getBatteryStateForReporting();
            changes.accept(eventTime);
            final State newState = getBatteryStateForReporting();
            if (!oldState.equals(newState)) {
                notifyAllListenersForDevice(newState);
            }
        }

        public void onConfiguration(long eventTime) {
            processChangesAndNotify(eventTime, this::configureDeviceMonitor);
        }

        private void configureDeviceMonitor(long eventTime) {
            if (mHasBattery != hasBattery(mState.deviceId)) {
                mHasBattery = !mHasBattery;
                if (mHasBattery) {
                    startMonitoring();
                } else {
                    stopMonitoring();
                }
                updateBatteryStateFromNative(eventTime);
            }
        }

        private void startMonitoring() {
            final String batteryPath = mNative.getBatteryDevicePath(mState.deviceId);
            if (batteryPath == null) {
                return;
            }
            final int deviceId = mState.deviceId;
            mUEventBatteryListener = new UEventBatteryListener() {
                @Override
                public void onBatteryUEvent(long eventTime) {
                    handleUEventNotification(deviceId, eventTime);
                }
            };
            mUEventManager.addListener(
                    mUEventBatteryListener, "DEVPATH=" + formatDevPath(batteryPath));
        }

        private String formatDevPath(@NonNull String path) {
            // Remove the "/sys" prefix if it has one.
            return path.startsWith("/sys") ? path.substring(4) : path;
        }

        private void stopMonitoring() {
            if (mUEventBatteryListener != null) {
                mUEventManager.removeListener(mUEventBatteryListener);
                mUEventBatteryListener = null;
            }
        }

        // This must be called when the device is no longer being monitored.
        public void onMonitorDestroy() {
            stopMonitoring();
        }

        protected void updateBatteryStateFromNative(long eventTime) {
            mState.updateIfChanged(
                    queryBatteryStateFromNative(mState.deviceId, eventTime, mHasBattery));
        }

        public void onPoll(long eventTime) {
            processChangesAndNotify(eventTime, this::updateBatteryStateFromNative);
        }

        public void onUEvent(long eventTime) {
            processChangesAndNotify(eventTime, this::updateBatteryStateFromNative);
        }

        public boolean requiresPolling() {
            return true;
        }

        public boolean isPersistent() {
            return false;
        }

        public void onTimeout(long eventTime) {}

        public void onStylusGestureStarted(long eventTime) {}

        // Returns the current battery state that can be used to notify listeners BatteryController.
        public State getBatteryStateForReporting() {
            return new State(mState);
        }

        @Override
        public String toString() {
            return "DeviceId=" + mState.deviceId
                    + ", Name='" + getInputDeviceName(mState.deviceId) + "'"
                    + ", NativeBattery=" + mState
                    + ", UEventListener=" + (mUEventBatteryListener != null ? "added" : "none");
        }
    }

    // Battery monitoring logic that is specific to stylus devices that support the
    // Universal Stylus Initiative (USI) protocol.
    private class UsiDeviceMonitor extends DeviceMonitor {

        // For USI devices, we only treat the battery state as valid for a fixed amount of time
        // after receiving a battery update. Once the timeout has passed, we signal to all listeners
        // that there is no longer a battery present for the device. The battery state is valid
        // as long as this callback is non-null.
        @Nullable
        private Runnable mValidityTimeoutCallback;

        UsiDeviceMonitor(int deviceId) {
            super(deviceId);
        }

        @Override
        public void onPoll(long eventTime) {
            // Disregard polling for USI devices.
        }

        @Override
        public void onUEvent(long eventTime) {
            processChangesAndNotify(eventTime, (time) -> {
                updateBatteryStateFromNative(time);
                markUsiBatteryValid();
            });
        }

        @Override
        public void onStylusGestureStarted(long eventTime) {
            processChangesAndNotify(eventTime, (time) -> {
                final boolean wasValid = mValidityTimeoutCallback != null;
                if (!wasValid && mState.capacity == 0.f) {
                    // Handle a special case where the USI device reports a battery capacity of 0
                    // at boot until the first battery update. To avoid wrongly sending out a
                    // battery capacity of 0 if we detect stylus presence before the capacity
                    // is first updated, do not validate the battery state when the state is not
                    // valid and the capacity is 0.
                    return;
                }
                markUsiBatteryValid();
            });
        }

        @Override
        public void onTimeout(long eventTime) {
            processChangesAndNotify(eventTime, (time) -> markUsiBatteryInvalid());
        }

        @Override
        public void onConfiguration(long eventTime) {
            super.onConfiguration(eventTime);

            if (!mHasBattery) {
                throw new IllegalStateException(
                        "UsiDeviceMonitor: USI devices are always expected to "
                                + "report a valid battery, but no battery was detected!");
            }
        }

        private void markUsiBatteryValid() {
            if (mValidityTimeoutCallback != null) {
                mHandler.removeCallbacks(mValidityTimeoutCallback);
            } else {
                final int deviceId = mState.deviceId;
                mValidityTimeoutCallback =
                        () -> BatteryController.this.handleMonitorTimeout(deviceId);
            }
            mHandler.postDelayed(mValidityTimeoutCallback, USI_BATTERY_VALIDITY_DURATION_MILLIS);
        }

        private void markUsiBatteryInvalid() {
            if (mValidityTimeoutCallback == null) {
                return;
            }
            mHandler.removeCallbacks(mValidityTimeoutCallback);
            mValidityTimeoutCallback = null;
        }

        @Override
        public State getBatteryStateForReporting() {
            return mValidityTimeoutCallback != null
                    ? new State(mState) : new State(mState.deviceId);
        }

        @Override
        public boolean requiresPolling() {
            // Do not poll the battery state for USI devices.
            return false;
        }

        @Override
        public boolean isPersistent() {
            // Do not remove the battery monitor for USI devices.
            return true;
        }

        @Override
        public String toString() {
            return super.toString()
                    + ", UsiStateIsValid=" + (mValidityTimeoutCallback != null);
        }
    }

    // An interface used to change the API of UEventObserver to a more test-friendly format.
    @VisibleForTesting
    interface UEventManager {

        @VisibleForTesting
        abstract class UEventBatteryListener {
            private final UEventObserver mObserver = new UEventObserver() {
                @Override
                public void onUEvent(UEvent event) {
                    final long eventTime = SystemClock.uptimeMillis();
                    if (DEBUG) {
                        Slog.d(TAG,
                                "UEventListener: Received UEvent: "
                                        + event + " eventTime: " + eventTime);
                    }
                    if (!"CHANGE".equalsIgnoreCase(event.get("ACTION"))
                            || !"POWER_SUPPLY".equalsIgnoreCase(event.get("SUBSYSTEM"))) {
                        // Disregard any UEvents that do not correspond to battery changes.
                        return;
                    }
                    UEventBatteryListener.this.onBatteryUEvent(eventTime);
                }
            };

            public abstract void onBatteryUEvent(long eventTime);
        }

        default void addListener(UEventBatteryListener listener, String match) {
            listener.mObserver.startObserving(match);
        }

        default void removeListener(UEventBatteryListener listener) {
            listener.mObserver.stopObserving();
        }
    }

    // Helper class that adds copying and printing functionality to IInputDeviceBatteryState.
    private static class State extends IInputDeviceBatteryState {

        State(int deviceId) {
            reset(deviceId);
        }

        State(IInputDeviceBatteryState s) {
            copyFrom(s);
        }

        State(int deviceId, long updateTime, boolean isPresent, int status, float capacity) {
            initialize(deviceId, updateTime, isPresent, status, capacity);
        }

        // Updates this from other if there is a difference between them, ignoring the updateTime.
        public void updateIfChanged(IInputDeviceBatteryState other) {
            if (!equalsIgnoringUpdateTime(other)) {
                copyFrom(other);
            }
        }

        public void reset(int deviceId) {
            initialize(deviceId, 0 /*updateTime*/, false /*isPresent*/, BatteryState.STATUS_UNKNOWN,
                    Float.NaN /*capacity*/);
        }

        private void copyFrom(IInputDeviceBatteryState s) {
            initialize(s.deviceId, s.updateTime, s.isPresent, s.status, s.capacity);
        }

        private void initialize(int deviceId, long updateTime, boolean isPresent, int status,
                float capacity) {
            this.deviceId = deviceId;
            this.updateTime = updateTime;
            this.isPresent = isPresent;
            this.status = status;
            this.capacity = capacity;
        }

        private boolean equalsIgnoringUpdateTime(IInputDeviceBatteryState other) {
            long updateTime = this.updateTime;
            this.updateTime = other.updateTime;
            boolean eq = this.equals(other);
            this.updateTime = updateTime;
            return eq;
        }

        @Override
        public String toString() {
            if (!isPresent) {
                return "State{<not present>}";
            }
            return "State{time=" + updateTime
                    + ", isPresent=" + isPresent
                    + ", status=" + status
                    + ", capacity=" + capacity
                    + "}";
        }
    }

    // Check if any value in an ArrayMap matches the predicate in an optimized way.
    private static <K, V> boolean anyOf(ArrayMap<K, V> arrayMap, Predicate<V> test) {
        for (int i = 0; i < arrayMap.size(); i++) {
            if (test.test(arrayMap.valueAt(i))) {
                return true;
            }
        }
        return false;
    }
}
