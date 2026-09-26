package com.rem.designsystem.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * An ordered slot in the onboarding sequence. [content] is a self-contained step that renders an
 * [OnboardingScaffold] using the [OnboardingStepScope] it is handed (for the back affordance, the
 * progress indicator, and forward/skip navigation).
 *
 * Steps are plain data, so the #12 middle steps (Connectors → Check-in → Voice) plug in as additional
 * entries in the same list — the sequencer needs no knowledge of what a step contains.
 */
class OnboardingStep(
    val id: String,
    val content: @Composable (OnboardingStepScope) -> Unit,
)

/**
 * What a step can see and do inside the sequence: where it sits, how to move, and the back affordance
 * (null on the first step, so the scaffold hides the chevron). Navigation is the sequencer's; a step
 * that also runs async work (sign-in auth, consent persistence) does that work through its own host
 * callbacks and calls [advance] only once the work succeeds.
 */
interface OnboardingStepScope {
    /** Zero-based index of this step in the sequence. */
    val index: Int

    /** Total number of steps. */
    val count: Int

    val isFirst: Boolean
    val isLast: Boolean

    /** Progress model for the shell's indicator. */
    val progress: OnboardingProgress

    /** Back target, or null when there is nowhere to go back to (first step). */
    val onBack: (() -> Unit)?

    /** Move forward one step, or complete the sequence when on the last step. */
    fun advance()

    /** Skip this step. Semantically a skip; mechanically advances forward. */
    fun skip()
}

/**
 * The **onboarding sequencer** — drives the ordered flow: it owns the current index, renders the
 * current step, and provides forward (Continue/Skip) + backward navigation with a progress indicator.
 * There is deliberately **no deploy/provisioning step**: the path is exactly the [steps] the host
 * passes (Sign-in → Consent → the #12 middle steps), nothing injected.
 *
 * It holds no product logic and no auth — a step wires real behaviour through its own callbacks and
 * advances via [OnboardingStepScope.advance]. [onComplete] fires when the last step advances.
 */
@Composable
fun OnboardingSequencer(
    steps: List<OnboardingStep>,
    modifier: Modifier = Modifier,
    state: OnboardingSequencerState = rememberOnboardingSequencerState(steps.size),
    showProgress: Boolean = true,
    onComplete: () -> Unit = {},
) {
    require(steps.isNotEmpty()) { "OnboardingSequencer requires at least one step." }
    val current = state.currentIndex.coerceIn(0, steps.lastIndex)

    AnimatedContent(
        targetState = current,
        transitionSpec = {
            // Forward = fade in from ahead; backward = fade from behind. Kept to a cross-fade so it
            // reads as one continuous sequence without inventing platform-specific chrome motion.
            (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
        },
        modifier = modifier.fillMaxSize(),
        label = "onboarding-step",
    ) { targetIndex ->
        val scope = object : OnboardingStepScope {
            override val index = targetIndex
            override val count = steps.size
            override val isFirst = targetIndex == 0
            override val isLast = targetIndex == steps.lastIndex
            override val progress =
                if (showProgress) OnboardingProgress(current = targetIndex, total = steps.size)
                else OnboardingProgress(current = 0, total = 0)
            override val onBack: (() -> Unit)? = if (targetIndex == 0) null else ({ state.back() })
            override fun advance() {
                if (targetIndex >= steps.lastIndex) onComplete() else state.advance()
            }
            override fun skip() = advance()
        }
        steps[targetIndex].content(scope)
    }
}

/** Holds the sequencer's position. Survives configuration changes via [rememberOnboardingSequencerState]. */
class OnboardingSequencerState internal constructor(initialIndex: Int, private val stepCount: Int) {
    var currentIndex by mutableIntStateOf(initialIndex)
        private set

    val isFirst: Boolean get() = currentIndex == 0
    val isLast: Boolean get() = currentIndex >= stepCount - 1

    fun advance() {
        if (currentIndex < stepCount - 1) currentIndex += 1
    }

    fun back() {
        if (currentIndex > 0) currentIndex -= 1
    }

    fun goTo(index: Int) {
        currentIndex = index.coerceIn(0, (stepCount - 1).coerceAtLeast(0))
    }

    internal companion object {
        fun saver(stepCount: Int): Saver<OnboardingSequencerState, Int> = Saver(
            save = { it.currentIndex },
            restore = { OnboardingSequencerState(it, stepCount) },
        )
    }
}

@Composable
fun rememberOnboardingSequencerState(stepCount: Int, initialIndex: Int = 0): OnboardingSequencerState =
    rememberSaveable(stepCount, saver = OnboardingSequencerState.saver(stepCount)) {
        OnboardingSequencerState(initialIndex, stepCount)
    }
