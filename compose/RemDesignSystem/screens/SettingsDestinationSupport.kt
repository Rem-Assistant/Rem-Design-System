package com.rem.designsystem.screens

/**
 * Shared constants for the Settings prototype destination screens (Memory, Models). Owned by the
 * destination lane; the central [SettingsEntryContent] / [AgentSettingsContent] are untouched.
 */
object PlaygroundMockData {
    /**
     * The playground-wide mock-data explanation, surfaced once per destination (as a container
     * content description) instead of as extra implementation text inside the designed rows.
     */
    const val hint = "Illustrative prototype data. No accounts, keys, services, or persistence are used."
}
