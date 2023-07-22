/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.compose.animation.scene

/**
 * A base class to create unique keys, associated to an [identity] that is used to check the
 * equality of two key instances.
 */
sealed class Key(val name: String, val identity: Any) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this.javaClass != other?.javaClass) return false
        return identity == (other as? Key)?.identity
    }

    override fun hashCode(): Int {
        return identity.hashCode()
    }

    override fun toString(): String {
        return "Key(name=$name)"
    }
}

/** Key for a scene. */
class SceneKey(name: String, identity: Any = Object()) : Key(name, identity) {
    override fun toString(): String {
        return "SceneKey(name=$name)"
    }
}

/** Key for an element. */
class ElementKey(
    name: String,
    identity: Any = Object(),

    /**
     * Whether this element is a background and usually drawn below other elements. This should be
     * set to true to make sure that shared backgrounds are drawn below elements of other scenes.
     */
    val isBackground: Boolean = false,
) : Key(name, identity), ElementMatcher {
    override fun matches(key: ElementKey): Boolean {
        return key == this
    }

    override fun toString(): String {
        return "ElementKey(name=$name)"
    }

    companion object {
        /** Matches any element whose [key identity][ElementKey.identity] matches [predicate]. */
        fun withIdentity(predicate: (Any) -> Boolean): ElementMatcher {
            return object : ElementMatcher {
                override fun matches(key: ElementKey): Boolean = predicate(key.identity)
            }
        }
    }
}

/** Key for a shared value of an element. */
class ValueKey(name: String, identity: Any = Object()) : Key(name, identity) {
    override fun toString(): String {
        return "ValueKey(name=$name)"
    }
}
