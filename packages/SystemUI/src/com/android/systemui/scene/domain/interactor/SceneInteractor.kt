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

package com.android.systemui.scene.domain.interactor

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.scene.data.repository.SceneContainerRepository
import com.android.systemui.scene.shared.logger.SceneLogger
import com.android.systemui.scene.shared.model.ObservableTransitionState
import com.android.systemui.scene.shared.model.RemoteUserInput
import com.android.systemui.scene.shared.model.SceneKey
import com.android.systemui.scene.shared.model.SceneModel
import com.android.systemui.util.kotlin.pairwise
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Generic business logic and app state accessors for the scene framework.
 *
 * Note that this class should not depend on state or logic of other modules or features. Instead,
 * other feature modules should depend on and call into this class when their parts of the
 * application state change.
 */
@SysUISingleton
class SceneInteractor
@Inject
constructor(
    private val repository: SceneContainerRepository,
    private val logger: SceneLogger,
) {

    /**
     * The currently *desired* scene.
     *
     * **Important:** this value will _commonly be different_ from what is being rendered in the UI,
     * by design.
     *
     * There are two intended sources for this value:
     * 1. Programmatic requests to transition to another scene (calls to [changeScene]).
     * 2. Reports from the UI about completing a transition to another scene (calls to
     *    [onSceneChanged]).
     *
     * Both the sources above cause the value of this flow to change; however, they cause mismatches
     * in different ways.
     *
     * **Updates from programmatic transitions**
     *
     * When an external bit of code asks the framework to switch to another scene, the value here
     * will update immediately. Downstream, the UI will detect this change and initiate the
     * transition animation. As the transition animation progresses, a threshold will be reached, at
     * which point the UI and the state here will match each other.
     *
     * **Updates from the UI**
     *
     * When the user interacts with the UI, the UI runs a transition animation that tracks the user
     * pointer (for example, the user's finger). During this time, the state value here and what the
     * UI shows will likely not match. Once/if a threshold is met, the UI reports it and commits the
     * change, making the value here match the UI again.
     */
    val desiredScene: StateFlow<SceneModel> = repository.desiredScene

    /**
     * The current state of the transition.
     *
     * Consumers should use this state to know:
     * 1. Whether there is an ongoing transition or if the system is at rest.
     * 2. When transitioning, which scenes are being transitioned between.
     * 3. When transitioning, what the progress of the transition is.
     */
    val transitionState: StateFlow<ObservableTransitionState> = repository.transitionState

    /** Whether the scene container is visible. */
    val isVisible: StateFlow<Boolean> = repository.isVisible

    private val _remoteUserInput: MutableStateFlow<RemoteUserInput?> = MutableStateFlow(null)
    /** A flow of motion events originating from outside of the scene framework. */
    val remoteUserInput: StateFlow<RemoteUserInput?> = _remoteUserInput.asStateFlow()

    /**
     * Returns the keys of all scenes in the container.
     *
     * The scenes will be sorted in z-order such that the last one is the one that should be
     * rendered on top of all previous ones.
     */
    fun allSceneKeys(): List<SceneKey> {
        return repository.allSceneKeys()
    }

    /**
     * Requests a scene change to the given scene.
     *
     * The change is animated. Therefore, while the value in [desiredScene] will update immediately,
     * it will be some time before the UI will switch to the desired scene. The scene change
     * requested is remembered here but served by the UI layer, which will start a transition
     * animation. Once enough of the transition has occurred, the system will come into agreement
     * between the [desiredScene] and the UI.
     */
    fun changeScene(scene: SceneModel, loggingReason: String) {
        updateDesiredScene(scene, loggingReason, logger::logSceneChangeRequested)
    }

    /** Sets the visibility of the container. */
    fun setVisible(isVisible: Boolean, loggingReason: String) {
        val wasVisible = repository.isVisible.value
        if (wasVisible == isVisible) {
            return
        }

        logger.logVisibilityChange(
            from = wasVisible,
            to = isVisible,
            reason = loggingReason,
        )
        return repository.setVisible(isVisible)
    }

    /**
     * Binds the given flow so the system remembers it.
     *
     * Note that you must call is with `null` when the UI is done or risk a memory leak.
     */
    fun setTransitionState(transitionState: Flow<ObservableTransitionState>?) {
        repository.setTransitionState(transitionState)
    }

    /**
     * Returns a stream of events that emits one [Unit] every time the framework transitions from
     * [from] to [to].
     */
    fun finishedSceneTransitions(from: SceneKey, to: SceneKey): Flow<Unit> {
        return transitionState
            .mapNotNull { it as? ObservableTransitionState.Idle }
            .map { idleState -> idleState.scene }
            .distinctUntilChanged()
            .pairwise()
            .mapNotNull { (previousSceneKey, currentSceneKey) ->
                Unit.takeIf { previousSceneKey == from && currentSceneKey == to }
            }
    }

    /** Handles a remote user input. */
    fun onRemoteUserInput(input: RemoteUserInput) {
        _remoteUserInput.value = input
    }

    /**
     * Notifies that the UI has transitioned sufficiently to the given scene.
     *
     * *Not intended for external use!*
     *
     * Once a transition between one scene and another passes a threshold, the UI invokes this
     * method to report it, updating the value in [desiredScene] to match what the UI shows.
     */
    internal fun onSceneChanged(scene: SceneModel, loggingReason: String) {
        updateDesiredScene(scene, loggingReason, logger::logSceneChangeCommitted)
    }

    private fun updateDesiredScene(
        scene: SceneModel,
        loggingReason: String,
        log: (from: SceneKey, to: SceneKey, loggingReason: String) -> Unit,
    ) {
        val currentSceneKey = desiredScene.value.key
        if (currentSceneKey == scene.key) {
            return
        }

        log(
            /* from= */ currentSceneKey,
            /* to= */ scene.key,
            /* loggingReason= */ loggingReason,
        )
        repository.setDesiredScene(scene)
    }
}
