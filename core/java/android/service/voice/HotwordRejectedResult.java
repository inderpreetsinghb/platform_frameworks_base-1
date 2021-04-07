/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.service.voice;

import static android.service.voice.HotwordDetector.CONFIDENCE_LEVEL_NONE;

import android.annotation.SystemApi;
import android.os.Parcelable;

import com.android.internal.util.DataClass;

/**
 * Represents a result supporting the rejected hotword trigger.
 *
 * @hide
 */
@DataClass(
        genConstructor = false,
        genHiddenBuilder = true,
        genEqualsHashCode = true,
        genHiddenConstDefs = true,
        genParcelable = true,
        genToString = true
)
@SystemApi
public final class HotwordRejectedResult implements Parcelable {

    /** Confidence level in the trigger outcome. */
    @HotwordDetector.HotwordConfidenceLevelValue
    private final int mConfidenceLevel;
    private static int defaultConfidenceLevel() {
        return CONFIDENCE_LEVEL_NONE;
    }



    // Code below generated by codegen v1.0.22.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/core/java/android/service/voice/HotwordRejectedResult.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ HotwordRejectedResult(
            @HotwordDetector.HotwordConfidenceLevelValue int confidenceLevel) {
        this.mConfidenceLevel = confidenceLevel;
        com.android.internal.util.AnnotationValidations.validate(
                HotwordDetector.HotwordConfidenceLevelValue.class, null, mConfidenceLevel);

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * Confidence level in the trigger outcome.
     */
    @DataClass.Generated.Member
    public @HotwordDetector.HotwordConfidenceLevelValue int getConfidenceLevel() {
        return mConfidenceLevel;
    }

    @Override
    @DataClass.Generated.Member
    public String toString() {
        // You can override field toString logic by defining methods like:
        // String fieldNameToString() { ... }

        return "HotwordRejectedResult { " +
                "confidenceLevel = " + mConfidenceLevel +
        " }";
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@android.annotation.Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(HotwordRejectedResult other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        HotwordRejectedResult that = (HotwordRejectedResult) o;
        //noinspection PointlessBooleanExpression
        return true
                && mConfidenceLevel == that.mConfidenceLevel;
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + mConfidenceLevel;
        return _hash;
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@android.annotation.NonNull android.os.Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        dest.writeInt(mConfidenceLevel);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ HotwordRejectedResult(@android.annotation.NonNull android.os.Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        int confidenceLevel = in.readInt();

        this.mConfidenceLevel = confidenceLevel;
        com.android.internal.util.AnnotationValidations.validate(
                HotwordDetector.HotwordConfidenceLevelValue.class, null, mConfidenceLevel);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @android.annotation.NonNull Parcelable.Creator<HotwordRejectedResult> CREATOR
            = new Parcelable.Creator<HotwordRejectedResult>() {
        @Override
        public HotwordRejectedResult[] newArray(int size) {
            return new HotwordRejectedResult[size];
        }

        @Override
        public HotwordRejectedResult createFromParcel(@android.annotation.NonNull android.os.Parcel in) {
            return new HotwordRejectedResult(in);
        }
    };

    /**
     * A builder for {@link HotwordRejectedResult}
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static final class Builder {

        private @HotwordDetector.HotwordConfidenceLevelValue int mConfidenceLevel;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        /**
         * Confidence level in the trigger outcome.
         */
        @DataClass.Generated.Member
        public @android.annotation.NonNull Builder setConfidenceLevel(@HotwordDetector.HotwordConfidenceLevelValue int value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mConfidenceLevel = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @android.annotation.NonNull HotwordRejectedResult build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2; // Mark builder used

            if ((mBuilderFieldsSet & 0x1) == 0) {
                mConfidenceLevel = defaultConfidenceLevel();
            }
            HotwordRejectedResult o = new HotwordRejectedResult(
                    mConfidenceLevel);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x2) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1616108967315L,
            codegenVersion = "1.0.22",
            sourceFile = "frameworks/base/core/java/android/service/voice/HotwordRejectedResult.java",
            inputSignatures = "private final @android.service.voice.HotwordDetector.HotwordConfidenceLevelValue int mConfidenceLevel\nprivate static  int defaultConfidenceLevel()\nclass HotwordRejectedResult extends java.lang.Object implements [android.os.Parcelable]\n@com.android.internal.util.DataClass(genConstructor=false, genHiddenBuilder=true, genEqualsHashCode=true, genHiddenConstDefs=true, genParcelable=true, genToString=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
