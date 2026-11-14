package com.freekiosklite.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freekiosklite.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KioskComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsApplicationAndAdminEntry() {
        composeRule.onNodeWithText("FreeKiosk Lite").assertIsDisplayed()
        composeRule.onNodeWithText("Administrator configuration").assertIsDisplayed()
    }

    @Test
    fun administratorEntryNavigatesToConfigurationOrAuthentication() {
        composeRule.onNodeWithText("Administrator configuration").performClick()
        composeRule.waitForIdle()
        val setupNodes = composeRule.onAllNodesWithText("Administrator setup").fetchSemanticsNodes()
        if (setupNodes.isNotEmpty()) {
            composeRule.onNodeWithText("Administrator setup").assertIsDisplayed()
        } else {
            composeRule.onNodeWithText("Administrator authentication").assertIsDisplayed()
        }
    }
}
