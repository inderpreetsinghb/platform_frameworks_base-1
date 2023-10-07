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

package com.android.systemui.bouncer.ui.viewmodel

import androidx.test.filters.SmallTest
import com.android.systemui.R
import com.android.systemui.SysuiTestCase
import com.android.systemui.authentication.data.model.AuthenticationMethodModel
import com.android.systemui.authentication.data.repository.FakeAuthenticationRepository
import com.android.systemui.authentication.shared.model.AuthenticationPatternCoordinate as Point
import com.android.systemui.coroutines.collectLastValue
import com.android.systemui.scene.SceneTestUtils
import com.android.systemui.scene.shared.model.SceneKey
import com.android.systemui.scene.shared.model.SceneModel
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
class PatternBouncerViewModelTest : SysuiTestCase() {

    private val utils = SceneTestUtils(this)
    private val testScope = utils.testScope
    private val authenticationInteractor =
        utils.authenticationInteractor(
            repository = utils.authenticationRepository(),
        )
    private val sceneInteractor = utils.sceneInteractor()
    private val bouncerInteractor =
        utils.bouncerInteractor(
            authenticationInteractor = authenticationInteractor,
            sceneInteractor = sceneInteractor,
        )
    private val bouncerViewModel =
        utils.bouncerViewModel(
            bouncerInteractor = bouncerInteractor,
            authenticationInteractor = authenticationInteractor,
        )
    private val underTest =
        PatternBouncerViewModel(
            applicationContext = context,
            applicationScope = testScope.backgroundScope,
            interactor = bouncerInteractor,
            isInputEnabled = MutableStateFlow(true).asStateFlow(),
        )

    private val containerSize = 90 // px
    private val dotSize = 30 // px

    @Before
    fun setUp() {
        overrideResource(R.string.keyguard_enter_your_pattern, ENTER_YOUR_PATTERN)
        overrideResource(R.string.kg_wrong_pattern, WRONG_PATTERN)
    }

    @Test
    fun onShown() =
        testScope.runTest {
            val currentScene by collectLastValue(sceneInteractor.desiredScene)
            val message by collectLastValue(bouncerViewModel.message)
            val selectedDots by collectLastValue(underTest.selectedDots)
            val currentDot by collectLastValue(underTest.currentDot)
            transitionToPatternBouncer()

            underTest.onShown()

            assertThat(message?.text).isEqualTo(ENTER_YOUR_PATTERN)
            assertThat(selectedDots).isEmpty()
            assertThat(currentDot).isNull()
            assertThat(currentScene).isEqualTo(SceneModel(SceneKey.Bouncer))
        }

    @Test
    fun onDragStart() =
        testScope.runTest {
            val currentScene by collectLastValue(sceneInteractor.desiredScene)
            val message by collectLastValue(bouncerViewModel.message)
            val selectedDots by collectLastValue(underTest.selectedDots)
            val currentDot by collectLastValue(underTest.currentDot)
            transitionToPatternBouncer()
            underTest.onShown()
            runCurrent()

            underTest.onDragStart()

            assertThat(message?.text).isEmpty()
            assertThat(selectedDots).isEmpty()
            assertThat(currentDot).isNull()
            assertThat(currentScene).isEqualTo(SceneModel(SceneKey.Bouncer))
        }

    @Test
    fun onDragEnd_whenCorrect() =
        testScope.runTest {
            val currentScene by collectLastValue(sceneInteractor.desiredScene)
            val selectedDots by collectLastValue(underTest.selectedDots)
            val currentDot by collectLastValue(underTest.currentDot)
            transitionToPatternBouncer()
            underTest.onShown()
            underTest.onDragStart()
            assertThat(currentDot).isNull()
            CORRECT_PATTERN.forEachIndexed { index, coordinate ->
                dragToCoordinate(coordinate)
                assertWithMessage("Wrong selected dots for index $index")
                    .that(selectedDots)
                    .isEqualTo(
                        CORRECT_PATTERN.subList(0, index + 1).map {
                            PatternDotViewModel(
                                x = it.x,
                                y = it.y,
                            )
                        }
                    )
                assertWithMessage("Wrong current dot for index $index")
                    .that(currentDot)
                    .isEqualTo(
                        PatternDotViewModel(
                            x = CORRECT_PATTERN.subList(0, index + 1).last().x,
                            y = CORRECT_PATTERN.subList(0, index + 1).last().y,
                        )
                    )
            }

            underTest.onDragEnd()

            assertThat(currentScene).isEqualTo(SceneModel(SceneKey.Gone))
        }

    @Test
    fun onDragEnd_whenWrong() =
        testScope.runTest {
            val currentScene by collectLastValue(sceneInteractor.desiredScene)
            val message by collectLastValue(bouncerViewModel.message)
            val selectedDots by collectLastValue(underTest.selectedDots)
            val currentDot by collectLastValue(underTest.currentDot)
            transitionToPatternBouncer()
            underTest.onShown()
            underTest.onDragStart()
            CORRECT_PATTERN.subList(0, 3).forEach { coordinate -> dragToCoordinate(coordinate) }

            underTest.onDragEnd()

            assertThat(selectedDots).isEmpty()
            assertThat(currentDot).isNull()
            assertThat(message?.text).isEqualTo(WRONG_PATTERN)
            assertThat(currentScene).isEqualTo(SceneModel(SceneKey.Bouncer))
        }

    @Test
    fun onDrag_shouldIncludeDotsThatWereSkippedOverAlongTheSameRow() =
        testScope.runTest {
            val selectedDots by collectLastValue(underTest.selectedDots)
            transitionToPatternBouncer()
            underTest.onShown()

            /*
             * Pattern setup, coordinates are (column, row)
             *   0  1  2
             * 0 x  x  x
             * 1 x  x  x
             * 2 x  x  x
             */
            // Select (0,0), Skip over (1,0) and select (2,0)
            dragOverCoordinates(Point(0, 0), Point(2, 0))

            assertThat(selectedDots)
                .isEqualTo(
                    listOf(
                        PatternDotViewModel(0, 0),
                        PatternDotViewModel(1, 0),
                        PatternDotViewModel(2, 0)
                    )
                )
        }

    @Test
    fun onDrag_shouldIncludeDotsThatWereSkippedOverAlongTheSameColumn() =
        testScope.runTest {
            val selectedDots by collectLastValue(underTest.selectedDots)
            transitionToPatternBouncer()
            underTest.onShown()

            /*
             * Pattern setup, coordinates are (column, row)
             *   0  1  2
             * 0 x  x  x
             * 1 x  x  x
             * 2 x  x  x
             */
            // Select (1,0), Skip over (1,1) and select (1, 2)
            dragOverCoordinates(Point(1, 0), Point(1, 2))

            assertThat(selectedDots)
                .isEqualTo(
                    listOf(
                        PatternDotViewModel(1, 0),
                        PatternDotViewModel(1, 1),
                        PatternDotViewModel(1, 2)
                    )
                )
        }

    @Test
    fun onDrag_shouldIncludeDotsThatWereSkippedOverAlongTheDiagonal() =
        testScope.runTest {
            val selectedDots by collectLastValue(underTest.selectedDots)
            transitionToPatternBouncer()
            underTest.onShown()

            /*
             * Pattern setup
             *   0  1  2
             * 0 x  x  x
             * 1 x  x  x
             * 2 x  x  x
             *
             * Coordinates are (column, row)
             * Select (2,0), Skip over (1,1) and select (0, 2)
             */
            dragOverCoordinates(Point(2, 0), Point(0, 2))

            assertThat(selectedDots)
                .isEqualTo(
                    listOf(
                        PatternDotViewModel(2, 0),
                        PatternDotViewModel(1, 1),
                        PatternDotViewModel(0, 2)
                    )
                )
        }

    @Test
    fun onDrag_shouldNotIncludeDotIfItIsNotOnTheLine() =
        testScope.runTest {
            val selectedDots by collectLastValue(underTest.selectedDots)
            transitionToPatternBouncer()
            underTest.onShown()

            /*
             * Pattern setup
             *   0  1  2
             * 0 x  x  x
             * 1 x  x  x
             * 2 x  x  x
             *
             * Coordinates are (column, row)
             */
            dragOverCoordinates(Point(0, 0), Point(1, 0), Point(2, 0), Point(0, 1))

            assertThat(selectedDots)
                .isEqualTo(
                    listOf(
                        PatternDotViewModel(0, 0),
                        PatternDotViewModel(1, 0),
                        PatternDotViewModel(2, 0),
                        PatternDotViewModel(0, 1),
                    )
                )
        }

    @Test
    fun onDrag_shouldNotIncludeSkippedOverDotsIfTheyAreAlreadySelected() =
        testScope.runTest {
            val selectedDots by collectLastValue(underTest.selectedDots)
            transitionToPatternBouncer()
            underTest.onShown()

            /*
             * Pattern setup
             *   0  1  2
             * 0 x  x  x
             * 1 x  x  x
             * 2 x  x  x
             *
             * Coordinates are (column, row)
             */
            dragOverCoordinates(Point(1, 0), Point(1, 1), Point(0, 0), Point(2, 0))

            assertThat(selectedDots)
                .isEqualTo(
                    listOf(
                        PatternDotViewModel(1, 0),
                        PatternDotViewModel(1, 1),
                        PatternDotViewModel(0, 0),
                        PatternDotViewModel(2, 0),
                    )
                )
        }

    @Test
    fun onDragEnd_whenPatternTooShort() =
        testScope.runTest {
            val currentScene by collectLastValue(sceneInteractor.desiredScene)
            val message by collectLastValue(bouncerViewModel.message)
            val selectedDots by collectLastValue(underTest.selectedDots)
            val currentDot by collectLastValue(underTest.currentDot)
            val throttlingDialogMessage by
                collectLastValue(bouncerViewModel.throttlingDialogMessage)
            utils.authenticationRepository.setAuthenticationMethod(
                AuthenticationMethodModel.Pattern
            )
            utils.authenticationRepository.setUnlocked(false)
            sceneInteractor.changeScene(SceneModel(SceneKey.Bouncer), "reason")
            sceneInteractor.onSceneChanged(SceneModel(SceneKey.Bouncer), "reason")
            assertThat(currentScene).isEqualTo(SceneModel(SceneKey.Bouncer))
            underTest.onShown()

            // Enter a pattern that's too short more than enough times that would normally trigger
            // throttling if the pattern were not too short and wrong:
            val attempts = FakeAuthenticationRepository.MAX_FAILED_AUTH_TRIES_BEFORE_THROTTLING + 1
            repeat(attempts) { attempt ->
                underTest.onDragStart()
                CORRECT_PATTERN.subList(
                        0,
                        authenticationInteractor.minPatternLength - 1,
                    )
                    .forEach { coordinate ->
                        underTest.onDrag(
                            xPx = 30f * coordinate.x + 15,
                            yPx = 30f * coordinate.y + 15,
                            containerSizePx = 90,
                            verticalOffsetPx = 0f,
                        )
                    }

                underTest.onDragEnd()

                assertWithMessage("Attempt #$attempt").that(message?.text).isEqualTo(WRONG_PATTERN)
                assertWithMessage("Attempt #$attempt").that(throttlingDialogMessage).isNull()
            }
        }

    @Test
    fun onDragEnd_correctAfterWrong() =
        testScope.runTest {
            val currentScene by collectLastValue(sceneInteractor.desiredScene)
            val message by collectLastValue(bouncerViewModel.message)
            val selectedDots by collectLastValue(underTest.selectedDots)
            val currentDot by collectLastValue(underTest.currentDot)
            transitionToPatternBouncer()
            underTest.onShown()
            underTest.onDragStart()
            CORRECT_PATTERN.subList(2, 7).forEach { coordinate -> dragToCoordinate(coordinate) }
            underTest.onDragEnd()
            assertThat(selectedDots).isEmpty()
            assertThat(currentDot).isNull()
            assertThat(message?.text).isEqualTo(WRONG_PATTERN)
            assertThat(currentScene).isEqualTo(SceneModel(SceneKey.Bouncer))

            // Enter the correct pattern:
            CORRECT_PATTERN.forEach { coordinate -> dragToCoordinate(coordinate) }

            underTest.onDragEnd()

            assertThat(currentScene).isEqualTo(SceneModel(SceneKey.Gone))
        }

    private fun dragOverCoordinates(vararg coordinatesDragged: Point) {
        underTest.onDragStart()
        coordinatesDragged.forEach { dragToCoordinate(it) }
    }

    private fun dragToCoordinate(coordinate: Point) {
        underTest.onDrag(
            xPx = dotSize * coordinate.x + 15f,
            yPx = dotSize * coordinate.y + 15f,
            containerSizePx = containerSize,
            verticalOffsetPx = 0f,
        )
    }

    private fun TestScope.transitionToPatternBouncer() {
        utils.authenticationRepository.setAuthenticationMethod(AuthenticationMethodModel.Pattern)
        utils.authenticationRepository.setUnlocked(false)
        sceneInteractor.changeScene(SceneModel(SceneKey.Bouncer), "reason")
        sceneInteractor.onSceneChanged(SceneModel(SceneKey.Bouncer), "reason")
        assertThat(collectLastValue(sceneInteractor.desiredScene).invoke())
            .isEqualTo(SceneModel(SceneKey.Bouncer))
    }

    companion object {
        private const val ENTER_YOUR_PATTERN = "Enter your pattern"
        private const val WRONG_PATTERN = "Wrong pattern"
        private val CORRECT_PATTERN = FakeAuthenticationRepository.PATTERN
    }
}
