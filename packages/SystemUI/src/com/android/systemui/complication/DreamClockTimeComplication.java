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

package com.android.systemui.complication;

import static com.android.systemui.complication.dagger.DreamClockTimeComplicationModule.DREAM_CLOCK_TIME_COMPLICATION_VIEW;
import static com.android.systemui.complication.dagger.RegisteredComplicationsModule.DREAM_CLOCK_TIME_COMPLICATION_LAYOUT_PARAMS;

import android.view.View;

import com.android.systemui.CoreStartable;
import com.android.systemui.dagger.qualifiers.SystemUser;
import com.android.systemui.dreams.DreamOverlayStateController;
import com.android.systemui.shared.condition.Monitor;
import com.android.systemui.util.condition.ConditionalCoreStartable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 * Clock Time Complication that produce Clock Time view holder.
 */
public class DreamClockTimeComplication implements Complication {
    private final Provider<DreamClockTimeViewHolder> mDreamClockTimeViewHolderProvider;

    /**
     * Default constructor for {@link DreamClockTimeComplication}.
     */
    @Inject
    public DreamClockTimeComplication(
            Provider<DreamClockTimeViewHolder> dreamClockTimeViewHolderProvider) {
        mDreamClockTimeViewHolderProvider = dreamClockTimeViewHolderProvider;
    }

    @Override
    public int getRequiredTypeAvailability() {
        return COMPLICATION_TYPE_TIME;
    }

    /**
     * Create {@link DreamClockTimeViewHolder}.
     */
    @Override
    public ViewHolder createView(ComplicationViewModel model) {
        return mDreamClockTimeViewHolderProvider.get();
    }

    /**
     * {@link CoreStartable} responsible for registering {@link DreamClockTimeComplication} with
     * SystemUI.
     */
    public static class Registrant extends ConditionalCoreStartable {
        private final DreamOverlayStateController mDreamOverlayStateController;
        private final DreamClockTimeComplication mComplication;

        /**
         * Default constructor to register {@link DreamClockTimeComplication}.
         */
        @Inject
        public Registrant(
                DreamOverlayStateController dreamOverlayStateController,
                DreamClockTimeComplication dreamClockTimeComplication,
                @SystemUser Monitor monitor) {
            super(monitor);
            mDreamOverlayStateController = dreamOverlayStateController;
            mComplication = dreamClockTimeComplication;
        }

        @Override
        public void onStart() {
            mDreamOverlayStateController.addComplication(mComplication);
        }
    }

    /**
     * {@link ViewHolder} to contain value/logic associated with {@link DreamClockTimeComplication}.
     */
    public static class DreamClockTimeViewHolder implements ViewHolder {
        private final View mView;
        private final ComplicationLayoutParams mLayoutParams;

        @Inject
        DreamClockTimeViewHolder(@Named(DREAM_CLOCK_TIME_COMPLICATION_VIEW) View view,
                @Named(DREAM_CLOCK_TIME_COMPLICATION_LAYOUT_PARAMS)
                        ComplicationLayoutParams layoutParams) {
            mView = view;
            mLayoutParams = layoutParams;
        }

        @Override
        public View getView() {
            return mView;
        }

        @Override
        public ComplicationLayoutParams getLayoutParams() {
            return mLayoutParams;
        }
    }
}
