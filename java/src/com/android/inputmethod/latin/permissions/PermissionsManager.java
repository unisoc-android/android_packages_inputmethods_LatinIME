/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.inputmethod.latin.permissions;

import android.app.Activity;
import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manager to perform permission related tasks. Always call on the UI thread.
 */
public class PermissionsManager {

    public interface PermissionsResultCallback {
        void onRequestPermissionsResult(boolean allGranted);
    }

    private int mRequestCodeId;

    private final Context mContext;
    private final Map<Integer, PermissionsResultCallback> mRequestIdToCallback = new HashMap<>();

    private static PermissionsManager sInstance;

    // UNISOC: Bug 1098081,683245 java.lang.NullPointerException, when request permission of contacts
    private PermissionsResultCallback mResultCallback;

    public PermissionsManager(Context context) {
        mContext = context;
    }

    @Nonnull
    public static synchronized PermissionsManager get(@Nonnull Context context) {
        if (sInstance == null) {
            sInstance = new PermissionsManager(context);
        }
        return sInstance;
    }

    private synchronized int getNextRequestId() {
        return ++mRequestCodeId;
    }

    /* UNISOC: Bug 1098081,683245 java.lang.NullPointerException, when request permission of contacts  @{ */
    public void setResultCallback(PermissionsResultCallback callback){
        mResultCallback = callback;
    }
    /* @} */

    public synchronized void requestPermissions(@Nonnull PermissionsResultCallback callback,
                                   @Nullable Activity activity,
                                   String... permissionsToRequest) {
        List<String> deniedPermissions = PermissionsUtil.getDeniedPermissions(
                mContext, permissionsToRequest);
        if (deniedPermissions.isEmpty()) {
            return;
        }
        // otherwise request the permissions.
        int requestId = getNextRequestId();
        String[] permissionsArray = deniedPermissions.toArray(
                new String[deniedPermissions.size()]);

        mRequestIdToCallback.put(requestId, callback);
        if (activity != null) {
            PermissionsUtil.requestPermissions(activity, requestId, permissionsArray);
        } else {
            PermissionsActivity.run(mContext, requestId, permissionsArray);
        }
    }

    public synchronized void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        PermissionsResultCallback permissionsResultCallback = mRequestIdToCallback.get(requestCode);
        mRequestIdToCallback.remove(requestCode);

        boolean allGranted = PermissionsUtil.allGranted(grantResults);
        /* UNISOC: Bug 1098081,683245 java.lang.NullPointerException, when request permission of contacts  @{ */
        if(permissionsResultCallback != null){
            permissionsResultCallback.onRequestPermissionsResult(allGranted);
        } else {
            mResultCallback.onRequestPermissionsResult(allGranted);
        }
        /* @} */
    }
}
