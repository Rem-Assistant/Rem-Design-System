package com.rem.designsystem.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.R

/**
 * The **Rem app-icon mark** — the brand-blue squircle bloom the sign-in lockup shows. This is the
 * *app icon*, not [RemFaceMark]: the sign-in lockup uses the icon (blue tile + white bloom), while
 * the face mark (blue outline bloom + eyes + smile) is the chat/thinking identity. They are
 * different marks — see the founder reference `tasks/refs/onboarding/01-sign-in.png`.
 *
 * Source of truth: the shipping raster `AppIcon` (`rem-assistant/remclaw`
 * `Rem/Assets.xcassets/AppIcon.appiconset/Logo.png`), transferred verbatim into this module's
 * drawables (`res/drawable-nodpi/rem_app_icon.png`) — the same asset the SwiftUI [RemAppIcon]
 * renders. The bloom ships as a raster in the app (no vector source), so the design system carries
 * the real asset rather than a re-drawn approximation that would drift from it.
 *
 * Treatment reproduced from `OnboardingLogoView`: `ContentScale.Fit` in a square, clipped to the
 * `small` (8dp) corner radius. Default size (40dp) matches the sign-in lockup.
 */
@Composable
fun RemAppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = 8.dp,
) {
    Image(
        painter = painterResource(id = R.drawable.rem_app_icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
    )
}
