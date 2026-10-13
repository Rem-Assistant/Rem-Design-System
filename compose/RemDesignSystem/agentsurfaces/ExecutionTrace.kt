package com.rem.designsystem.agentsurfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **ExecutionTrace** — Compose sibling of the SwiftUI [ExecutionTrace]. The agent "show your work"
 * surface: a header (status pill · title · subtitle · timestamp · close), a divider, a vertical list of
 * step rows grouped into lanes (MAIN / SUBAGENT), and a "Working" footer while the run is in progress.
 *
 * Each step row is `status-icon · (title + detail) · chevron`: a green check for a completed step, a red
 * cross for a failed step, a brand-blue hollow circle for the step in flight. A lane header (MAIN,
 * SUBAGENT 01) is emitted whenever the lane changes between consecutive steps.
 *
 * Cross-platform contract (SPEC): intent + tokens are shared, form is native — step glyphs are Material
 * [Icons] here (CheckCircle / Cancel / RadioButtonUnchecked / Refresh / Close / ChevronRight), SF Symbols
 * on iOS. Figma canonical: **ExecutionTrace** `431:21`, built on the base **Timeline** component
 * `482:56`.
 *
 * Packaging note: the Figma specimen is a full-bleed white screen; here it is packaged as a
 * backgroundSecondary rounded surface (radius.large) per the DS card language. The header status pill is
 * a filled *tone* capsule (not the quiet RemPill), matching the specimen's solid "In progress" pill.
 */
enum class ExecutionTraceStatus { InProgress, Completed, Failed }

sealed interface ExecutionTraceLane {
    data object Main : ExecutionTraceLane
    data class Subagent(val ordinal: String? = null) : ExecutionTraceLane
}

enum class ExecutionStepStatus {
    /** In flight — brand-blue hollow circle. */
    Active,
    /** Completed — green filled check. */
    Done,
    /** Failed — red filled cross. */
    Failed,
}

data class ExecutionStep(
    val label: String,
    val detail: String? = null,
    val lane: ExecutionTraceLane = ExecutionTraceLane.Main,
    val status: ExecutionStepStatus = ExecutionStepStatus.Done,
)

@Composable
fun ExecutionTrace(
    title: String,
    steps: List<ExecutionStep>,
    modifier: Modifier = Modifier,
    status: ExecutionTraceStatus = ExecutionTraceStatus.InProgress,
    subtitle: String? = null,
    timestamp: String? = null,
    footer: String = "Working",
    onClose: (() -> Unit)? = null,
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RemRadius.large))
            .background(colors.backgroundSecondary),
    ) {
        Header(status, title, subtitle, timestamp, onClose)
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
        Body(status, steps, footer)
    }
}

@Composable
private fun Header(
    status: ExecutionTraceStatus,
    title: String,
    subtitle: String?,
    timestamp: String?,
    onClose: (() -> Unit)?,
) {
    val colors = RemColors.current
    Column(
        modifier = Modifier.padding(RemSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(status)
            Spacer(Modifier.weight(1f))
            val closeModifier = if (onClose != null) Modifier.clickable { onClose() } else Modifier
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = colors.labelSecondary,
                modifier = closeModifier.size(20.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = RemTypography.title3Bold, color = colors.labelPrimary)
            if (subtitle != null) {
                Text(text = subtitle, style = RemTypography.subheadline, color = colors.labelSecondary)
            }
            if (timestamp != null) {
                Text(text = timestamp, style = RemTypography.caption1, color = colors.labelTertiary)
            }
        }
    }
}

@Composable
private fun StatusPill(status: ExecutionTraceStatus) {
    val colors = RemColors.current
    val tone = when (status) {
        ExecutionTraceStatus.InProgress -> colors.labelPrimary
        ExecutionTraceStatus.Completed -> colors.systemGreen
        ExecutionTraceStatus.Failed -> colors.systemRed
    }
    val label = when (status) {
        ExecutionTraceStatus.InProgress -> "In progress"
        ExecutionTraceStatus.Completed -> "Completed"
        ExecutionTraceStatus.Failed -> "Failed"
    }
    Text(
        text = label.uppercase(),
        style = RemTypography.caption1Bold,
        color = Color.White,
        modifier = Modifier
            .clip(CircleShape)
            .background(tone)
            .padding(horizontal = RemSpacing.md, vertical = 6.dp),
    )
}

@Composable
private fun Body(
    status: ExecutionTraceStatus,
    steps: List<ExecutionStep>,
    footer: String,
) {
    Column(
        modifier = Modifier.padding(RemSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        steps.forEachIndexed { index, step ->
            if (index == 0 || steps[index - 1].lane != step.lane) {
                LaneHeader(
                    lane = step.lane,
                    modifier = if (index == 0) Modifier else Modifier.padding(top = RemSpacing.sm),
                )
            }
            StepRow(step)
        }
        if (status == ExecutionTraceStatus.InProgress) {
            WorkingFooter(footer, Modifier.padding(top = RemSpacing.xs))
        }
    }
}

@Composable
private fun LaneHeader(lane: ExecutionTraceLane, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    val text = when (lane) {
        is ExecutionTraceLane.Main -> "MAIN"
        is ExecutionTraceLane.Subagent -> lane.ordinal?.let { "SUBAGENT $it" } ?: "SUBAGENT"
    }
    Text(
        text = text,
        style = RemTypography.caption1,
        color = colors.labelTertiary,
        letterSpacing = 0.5.sp,
        modifier = modifier,
    )
}

@Composable
private fun StepRow(step: ExecutionStep) {
    val colors = RemColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.md)) {
        StepIcon(step.status)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = step.label,
                style = RemTypography.subheadline,
                fontWeight = FontWeight.SemiBold,
                color = colors.labelPrimary,
            )
            if (step.detail != null) {
                Text(text = step.detail, style = RemTypography.footnote, color = colors.labelSecondary)
            }
        }
        Spacer(Modifier.size(RemSpacing.sm))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = colors.labelTertiary,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
    }
}

@Composable
private fun StepIcon(status: ExecutionStepStatus) {
    val colors = RemColors.current
    val (icon: ImageVector, tint: Color) = when (status) {
        ExecutionStepStatus.Active -> Icons.Filled.RadioButtonUnchecked to colors.brandBlue
        ExecutionStepStatus.Done -> Icons.Filled.CheckCircle to colors.systemGreen
        ExecutionStepStatus.Failed -> Icons.Filled.Cancel to colors.systemRed
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun WorkingFooter(footer: String, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            tint = colors.brandBlue,
            modifier = Modifier.size(16.dp),
        )
        Text(text = footer, style = RemTypography.subheadline, color = colors.labelSecondary)
    }
}

@Preview(name = "ExecutionTrace", showBackground = true, widthDp = 402)
@Composable
private fun ExecutionTracePreview() {
    RemTheme {
        Column(Modifier.padding(24.dp)) {
            ExecutionTrace(
                title = "Build RFE checklist",
                subtitle = "Writing the RFE checklist PDF template",
                timestamp = "10:49pm",
                status = ExecutionTraceStatus.InProgress,
                steps = listOf(
                    ExecutionStep(
                        label = "Launched H-1B RFE Checklist Tailoring Subagent",
                        detail = "Delegated the checklist prep to a subagent via artifact.send_input, covering a tailored checklist for USCIS.",
                        lane = ExecutionTraceLane.Main,
                        status = ExecutionStepStatus.Done,
                    ),
                    ExecutionStep(
                        label = "Prepared RFE Checklist Source Directories",
                        detail = "Staged the checklist sources; the concatenate step returned an incomplete JSON payload.",
                        lane = ExecutionTraceLane.Subagent("01"),
                        status = ExecutionStepStatus.Failed,
                    ),
                    ExecutionStep(
                        label = "Found USCIS RFE Official Results",
                        detail = "Web search targeting USCIS official guidance for H-1B Requests for Evidence.",
                        lane = ExecutionTraceLane.Subagent("01"),
                        status = ExecutionStepStatus.Done,
                    ),
                    ExecutionStep(
                        label = "Created index.html source file",
                        detail = "Wrote the RFE checklist HTML template used to render the PDF.",
                        lane = ExecutionTraceLane.Subagent("01"),
                        status = ExecutionStepStatus.Done,
                    ),
                ),
            )
        }
    }
}
