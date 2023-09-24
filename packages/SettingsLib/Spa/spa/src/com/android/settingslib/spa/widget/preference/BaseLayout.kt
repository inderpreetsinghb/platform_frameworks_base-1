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

package com.android.settingslib.spa.widget.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.toState
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsOpacity
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.ui.SettingsTitle

@Composable
internal fun BaseLayout(
    title: String,
    subTitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    enabled: State<Boolean> = true.toState(),
    paddingStart: Dp = SettingsDimension.itemPaddingStart,
    paddingEnd: Dp = SettingsDimension.itemPaddingEnd,
    paddingVertical: Dp = SettingsDimension.itemPaddingVertical,
    widget: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = paddingEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alphaModifier =
            Modifier.alpha(if (enabled.value) SettingsOpacity.Full else SettingsOpacity.Disabled)
        BaseIcon(icon, alphaModifier, paddingStart)
        Titles(
            title = title,
            subTitle = subTitle,
            modifier = alphaModifier
                .weight(1f)
                .padding(vertical = paddingVertical),
        )
        widget()
    }
}

@Composable
internal fun BaseIcon(
    icon: @Composable (() -> Unit)?,
    modifier: Modifier,
    paddingStart: Dp,
) {
    if (icon != null) {
        Box(
            modifier = modifier.size(SettingsDimension.itemIconContainerSize),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    } else {
        Spacer(modifier = Modifier.width(width = paddingStart))
    }
}

// Extracts a scope to avoid frequent recompose outside scope.
@Composable
private fun Titles(title: String, subTitle: @Composable () -> Unit, modifier: Modifier) {
    Column(modifier) {
        SettingsTitle(title)
        subTitle()
    }
}

@Preview
@Composable
private fun BaseLayoutPreview() {
    SettingsTheme {
        BaseLayout(
            title = "Title",
            subTitle = {
                HorizontalDivider(thickness = 10.dp)
            }
        )
    }
}
