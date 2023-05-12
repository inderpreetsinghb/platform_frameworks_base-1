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

package android.os;

import android.annotation.NonNull;
import android.annotation.SystemService;
import android.app.AppOpsManager;
import android.content.AttributionSource;
import android.content.Context;
import android.content.PermissionChecker;
import android.content.pm.PackageManager;
import android.permission.PermissionCheckerManager;

/**
 * PermissionEnforcer check permissions for AIDL-generated services which use
 * the @EnforcePermission annotation.
 *
 * <p>AIDL services may be annotated with @EnforcePermission which will trigger
 * the generation of permission check code. This generated code relies on
 * PermissionEnforcer to validate the permissions. The methods available are
 * purposely similar to the AIDL annotation syntax.
 *
 * @see android.permission.PermissionManager
 *
 * @hide
 */
@SystemService(Context.PERMISSION_ENFORCER_SERVICE)
public class PermissionEnforcer {

    private final Context mContext;
    private static final String ACCESS_DENIED = "Access denied, requires: ";

    /** Protected constructor. Allows subclasses to instantiate an object
     *  without using a Context.
     */
    protected PermissionEnforcer() {
        mContext = null;
    }

    /** Constructor, prefer using the fromContext static method when possible */
    public PermissionEnforcer(@NonNull Context context) {
        mContext = context;
    }

    @PermissionCheckerManager.PermissionResult
    protected int checkPermission(@NonNull String permission, @NonNull AttributionSource source) {
        return PermissionChecker.checkPermissionForDataDelivery(
            mContext, permission, PermissionChecker.PID_UNKNOWN, source, "" /* message */);
    }

    @SuppressWarnings("AndroidFrameworkClientSidePermissionCheck")
    @PermissionCheckerManager.PermissionResult
    protected int checkPermission(@NonNull String permission, int pid, int uid) {
        if (mContext.checkPermission(permission, pid, uid) == PackageManager.PERMISSION_GRANTED) {
            return PermissionCheckerManager.PERMISSION_GRANTED;
        }
        return PermissionCheckerManager.PERMISSION_HARD_DENIED;
    }

    private boolean anyAppOps(@NonNull String[] permissions) {
        for (String permission : permissions) {
            if (AppOpsManager.permissionToOpCode(permission) != AppOpsManager.OP_NONE) {
                return true;
            }
        }
        return false;
    }

    public void enforcePermission(@NonNull String permission, @NonNull
            AttributionSource source) throws SecurityException {
        int result = checkPermission(permission, source);
        if (result != PermissionCheckerManager.PERMISSION_GRANTED) {
            throw new SecurityException(ACCESS_DENIED + permission);
        }
    }

    public void enforcePermission(@NonNull String permission, int pid, int uid)
            throws SecurityException {
        if (AppOpsManager.permissionToOpCode(permission) != AppOpsManager.OP_NONE) {
            AttributionSource source = new AttributionSource(uid, null, null);
            enforcePermission(permission, source);
            return;
        }
        int result = checkPermission(permission, pid, uid);
        if (result != PermissionCheckerManager.PERMISSION_GRANTED) {
            throw new SecurityException(ACCESS_DENIED + permission);
        }
    }

    public void enforcePermissionAllOf(@NonNull String[] permissions,
            @NonNull AttributionSource source) throws SecurityException {
        for (String permission : permissions) {
            int result = checkPermission(permission, source);
            if (result != PermissionCheckerManager.PERMISSION_GRANTED) {
                throw new SecurityException(ACCESS_DENIED + "allOf={"
                        + String.join(", ", permissions) + "}");
            }
        }
    }

    public void enforcePermissionAllOf(@NonNull String[] permissions,
            int pid, int uid) throws SecurityException {
        if (anyAppOps(permissions)) {
            AttributionSource source = new AttributionSource(uid, null, null);
            enforcePermissionAllOf(permissions, source);
            return;
        }
        for (String permission : permissions) {
            int result = checkPermission(permission, pid, uid);
            if (result != PermissionCheckerManager.PERMISSION_GRANTED) {
                throw new SecurityException(ACCESS_DENIED + "allOf={"
                        + String.join(", ", permissions) + "}");
            }
        }
    }

    public void enforcePermissionAnyOf(@NonNull String[] permissions,
            @NonNull AttributionSource source) throws SecurityException {
        for (String permission : permissions) {
            int result = checkPermission(permission, source);
            if (result == PermissionCheckerManager.PERMISSION_GRANTED) {
                return;
            }
        }
        throw new SecurityException(ACCESS_DENIED + "anyOf={"
                + String.join(", ", permissions) + "}");
    }

    public void enforcePermissionAnyOf(@NonNull String[] permissions,
            int pid, int uid) throws SecurityException {
        if (anyAppOps(permissions)) {
            AttributionSource source = new AttributionSource(uid, null, null);
            enforcePermissionAnyOf(permissions, source);
            return;
        }
        for (String permission : permissions) {
            int result = checkPermission(permission, pid, uid);
            if (result == PermissionCheckerManager.PERMISSION_GRANTED) {
                return;
            }
        }
        throw new SecurityException(ACCESS_DENIED + "anyOf={"
                + String.join(", ", permissions) + "}");
    }

    /**
     * Returns a new PermissionEnforcer based on a Context.
     *
     * @hide
     */
    public static PermissionEnforcer fromContext(@NonNull Context context) {
        return (PermissionEnforcer) context.getSystemService(Context.PERMISSION_ENFORCER_SERVICE);
    }
}
