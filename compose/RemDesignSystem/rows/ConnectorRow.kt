package com.rem.designsystem.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rem.designsystem.R
import com.rem.designsystem.tokens.*

/** Controlled shared ConnectorRow 2213:9330. No internal connection or switch state. */
enum class ConnectorRowState(val subtitle: String) {
    Available("Not connected"), Connecting("Connecting…"), Connected("Connected"), Error("Couldn’t connect · Try again"),
}
sealed interface ConnectorRowAccessory {
    data class Action(val label: String, val onClick: () -> Unit) : ConnectorRowAccessory
    data object Progress : ConnectorRowAccessory
    data object Disclosure : ConnectorRowAccessory
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : ConnectorRowAccessory
}
@Composable
fun ConnectorRow(
    title: String, state: ConnectorRowState, accessory: ConnectorRowAccessory,
    modifier: Modifier = Modifier, subtitle: String = state.subtitle,
    onClick: (() -> Unit)? = null, showsDivider: Boolean = false,
    leading: @Composable () -> Unit, content: (@Composable () -> Unit)? = null,
) {
    val fontScale = LocalDensity.current.fontScale
    val accessoryContent: @Composable () -> Unit = {
        when (accessory) {
            is ConnectorRowAccessory.Action -> TextButton(onClick = accessory.onClick,
                modifier = Modifier.semantics { contentDescription = "${accessory.label} $title" },
                shape = CircleShape,
                colors = ButtonDefaults.textButtonColors(contentColor = RemColors.current.brandBlue, containerColor = RemColors.current.fillTertiary)) {
                Text(accessory.label, style = RemTypography.body)
            }
            ConnectorRowAccessory.Progress -> CircularProgressIndicator(Modifier.size(20.dp)
                .semantics { contentDescription = "Connecting $title" }, strokeWidth = 2.dp)
            ConnectorRowAccessory.Disclosure -> DisclosureChevron()
            is ConnectorRowAccessory.Toggle -> Switch(checked = accessory.checked,
                onCheckedChange = accessory.onCheckedChange,
                modifier = Modifier.semantics { contentDescription = title })
        }
    }
    BoxWithConstraints {
        // Keep the label readable as the available width shrinks or text grows. An action
        // can move below its label; disclosure, progress and switch accessories retain their slot.
        val stackAction = accessory is ConnectorRowAccessory.Action && maxWidth < (280 * fontScale).dp
        ListRow(modifier = modifier.heightIn(min = 64.dp), showsDivider = showsDivider, onClick = onClick,
            leading = leading, content = {
                if (content == null) ListRowLabel(title, subtitle) else content()
                if (stackAction) {
                    Spacer(Modifier.height(8.dp))
                    accessoryContent()
                }
            }, trailing = { if (!stackAction) accessoryContent() })
    }
}
enum class ConnectorProvider(val title: String, val asset: Int) {
    Gmail("Gmail", R.drawable.connector_gmail), GoogleCalendar("Google Calendar", R.drawable.connector_google_calendar),
    Notion("Notion", R.drawable.connector_notion), Slack("Slack", R.drawable.connector_slack),
    GoogleDrive("Google Drive", R.drawable.connector_google_drive), Linear("Linear", R.drawable.connector_linear),
    Todoist("Todoist", R.drawable.connector_todoist),
}
/** Exact node exports, original colors and documented 26×29 / Todoist 29×29 slot. */
@Composable
fun ConnectorProviderMark(provider: ConnectorProvider, modifier: Modifier = Modifier) {
    Box(modifier.size(width = if (provider == ConnectorProvider.Todoist) 29.dp else 26.dp, height = 29.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center) {
        Image(painterResource(provider.asset), contentDescription = null,
            modifier = Modifier.size(if (provider == ConnectorProvider.Todoist) 20.dp else 26.dp))
    }
}
