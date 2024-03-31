/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.asllib;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

/** Safety Label representation containing zero or more {@link DataCategory} for data shared */
public class SafetyLabels implements AslMarshallable {

    private final Long mVersion;
    private final DataLabels mDataLabels;

    public SafetyLabels(Long version, DataLabels dataLabels) {
        this.mVersion = version;
        this.mDataLabels = dataLabels;
    }

    /** Returns the data label for the safety label */
    public DataLabels getDataLabel() {
        return mDataLabels;
    }

    /** Gets the version of the {@link SafetyLabels}. */
    public Long getVersion() {
        return mVersion;
    }

    /** Creates an on-device DOM element from the {@link SafetyLabels}. */
    @Override
    public List<Element> toOdDomElements(Document doc) {
        Element safetyLabelsEle =
                XmlUtils.createPbundleEleWithName(doc, XmlUtils.OD_NAME_SAFETY_LABELS);
        XmlUtils.appendChildren(safetyLabelsEle, mDataLabels.toOdDomElements(doc));
        return List.of(safetyLabelsEle);
    }
}
